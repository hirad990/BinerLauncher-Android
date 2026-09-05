package ir.binercraft.binerlauncher.minecraft

import ir.binercraft.binerlauncher.runtime.JavaRuntimeManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class MinecraftLaunchPlanner(
    private val paths: MinecraftPaths,
    private val runtimes: JavaRuntimeManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun plan(
        version: ResolvedVersion,
        profile: LaunchProfile,
        options: LaunchOptions = LaunchOptions()
    ): LaunchPlan {
        require(profile.username.isNotBlank()) { "Username is required" }
        require(profile.uuid.isNotBlank()) { "UUID is required" }
        require(options.memoryMb >= 512) { "Memory must be at least 512 MB" }

        val runtime = runtimes.javaExecutable(version.javaMajor)
        require(runtime.isFile) { "Java ${version.javaMajor} is not installed: ${runtime.absolutePath}" }

        paths.ensureDirectories()
        val gameDirectory = File(paths.instances, version.id).apply { mkdirs() }
        val nativesDirectory = File(paths.natives, version.id).apply { mkdirs() }
        val clientJar = paths.clientJar(version.id)
        require(clientJar.isFile) { "Client jar is not installed: ${clientJar.absolutePath}" }

        val libraryFiles = version.libraries.map { File(paths.libraries, it.path) }.filter(File::isFile)
        val classpath = ClasspathBuilder.build(libraryFiles, clientJar)

        val metadata = json.parseToJsonElement(paths.versionJson(version.id).readText()).jsonObject
        val placeholders = buildPlaceholders(profile, version, gameDirectory, nativesDirectory)

        val jvmArgs = buildList {
            add("-Xms512M")
            add("-Xmx${options.memoryMb}M")
            add("-Djava.library.path=${nativesDirectory.absolutePath}")
            add("-Dorg.lwjgl.librarypath=${nativesDirectory.absolutePath}")
            add("-Dminecraft.launcher.brand=BinerLauncher")
            add("-Dminecraft.launcher.version=0.1.0")
            addAll(resolveArguments(metadata["arguments"]?.jsonObject?.get("jvm")?.jsonArray, placeholders))
            addAll(options.extraJvmArgs)
        }

        val gameArgs = if (metadata["arguments"]?.jsonObject?.get("game")?.jsonArray != null) {
            resolveArguments(metadata["arguments"]?.jsonObject?.get("game")?.jsonArray, placeholders)
        } else {
            parseLegacyArguments(metadata["minecraftArguments"]?.jsonPrimitive?.content.orEmpty(), placeholders)
        }.toMutableList().apply {
            addAll(listOf("--width", options.width.toString(), "--height", options.height.toString()))
            addAll(options.extraGameArgs)
        }

        val command = buildList {
            add(runtime.absolutePath)
            addAll(jvmArgs)
            add("-cp")
            add(classpath)
            add(version.mainClass)
            addAll(gameArgs)
        }

        return LaunchPlan(
            javaExecutable = runtime,
            workingDirectory = gameDirectory,
            command = command,
            environment = mapOf(
                "JAVA_HOME" to runtime.parentFile.parentFile.absolutePath,
                "PATH" to runtime.parentFile.absolutePath + File.pathSeparator + (System.getenv("PATH") ?: "")
            ),
            version = version
        )
    }

    private fun buildPlaceholders(
        profile: LaunchProfile,
        version: ResolvedVersion,
        gameDirectory: File,
        nativesDirectory: File
    ): Map<String, String> = mapOf(
        "auth_player_name" to profile.username,
        "auth_uuid" to profile.uuid,
        "auth_access_token" to profile.accessToken,
        "user_type" to profile.userType,
        "version_name" to version.id,
        "version_type" to version.releaseType,
        "game_directory" to gameDirectory.absolutePath,
        "assets_root" to paths.assets.absolutePath,
        "assets_index_name" to (version.assetIndexId ?: version.id),
        "natives_directory" to nativesDirectory.absolutePath,
        "launcher_name" to "BinerLauncher",
        "launcher_version" to "0.1.0",
        "user_properties" to "{}",
        "auth_xuid" to (profile.xuid ?: ""),
        "clientid" to (profile.clientId ?: "")
    )

    private fun resolveArguments(arguments: JsonArray?, placeholders: Map<String, String>): List<String> {
        if (arguments == null) return emptyList()
        val result = mutableListOf<String>()
        arguments.forEach { element ->
            when (element) {
                is JsonObject -> {
                    val rules = element["rules"]?.jsonArray
                    if (!rulesAllow(rules)) return@forEach
                    val value = element["value"] ?: return@forEach
                    when (value) {
                        is JsonArray -> value.forEach { item ->
                            result += substitute(item.jsonPrimitive.content, placeholders)
                        }
                        else -> result += substitute(value.jsonPrimitive.content, placeholders)
                    }
                }
                else -> result += substitute(element.jsonPrimitive.content, placeholders)
            }
        }
        return result
    }

    private fun parseLegacyArguments(raw: String, placeholders: Map<String, String>): List<String> =
        raw.trim().split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .map { substitute(it, placeholders) }

    private fun substitute(value: String, placeholders: Map<String, String>): String {
        var result = value
        placeholders.forEach { (key, replacement) ->
            result = result.replace("\${$key}", replacement)
        }
        return result
    }

    private fun rulesAllow(rules: JsonArray?): Boolean {
        if (rules == null) return true
        var matched = false
        var allowed = false
        rules.forEach { element ->
            val rule = element.jsonObject
            val os = rule["os"]?.jsonObject
            val osName = os?.get("name")?.jsonPrimitive?.content
            val architecture = os?.get("arch")?.jsonPrimitive?.content
            val nameMatches = osName == null || osName == "linux"
            val archMatches = architecture == null || architecture.equals("aarch64", true) || architecture.equals("arm64", true)
            if (nameMatches && archMatches) {
                matched = true
                allowed = rule["action"]?.jsonPrimitive?.content != "disallow"
            }
        }
        return !matched || allowed
    }
}
