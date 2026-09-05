package ir.binercraft.binerlauncher.minecraft

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class VersionResolver(private val paths: MinecraftPaths) {
    private val json = Json { ignoreUnknownKeys = true }

    fun resolve(versionId: String): ResolvedVersion {
        val file = paths.versionJson(versionId)
        require(file.isFile) { "Version metadata not installed: $versionId" }
        val root = json.parseToJsonElement(file.readText()).jsonObject
        val downloads = root["downloads"]?.jsonObject ?: error("Missing downloads")
        val client = downloads["client"]?.jsonObject ?: error("Missing client download")
        val javaMajor = root["javaVersion"]?.jsonObject?.get("majorVersion")
            ?.jsonPrimitive?.content?.toIntOrNull() ?: 17

        val libraries = mutableListOf<VersionArtifact>()
        val natives = mutableListOf<VersionArtifact>()
        root["libraries"]?.jsonArray?.forEach { element ->
            val library = element.jsonObject
            if (!rulesAllow(library["rules"]?.jsonArray)) return@forEach
            val downloadsObject = library["downloads"]?.jsonObject ?: return@forEach
            downloadsObject["artifact"]?.jsonObject?.toArtifact()?.let(libraries::add)

            val classifiers = downloadsObject["classifiers"]?.jsonObject ?: return@forEach
            val classifierName = nativeClassifierName()
            classifiers[classifierName]?.jsonObject?.toArtifact()?.let(natives::add)
        }

        return ResolvedVersion(
            id = root["id"]?.jsonPrimitive?.content ?: versionId,
            mainClass = root["mainClass"]?.jsonPrimitive?.content ?: error("Missing mainClass"),
            releaseType = root["type"]?.jsonPrimitive?.content ?: "release",
            javaMajor = javaMajor,
            client = client.toArtifact("versions/$versionId/$versionId.jar"),
            libraries = libraries,
            natives = natives,
            assetIndexUrl = root["assetIndex"]?.jsonObject?.get("url")?.jsonPrimitive?.content,
            assetIndexId = root["assetIndex"]?.jsonObject?.get("id")?.jsonPrimitive?.content
        )
    }

    private fun JsonObject.toArtifact(fallbackPath: String? = null): VersionArtifact {
        return VersionArtifact(
            path = this["path"]?.jsonPrimitive?.content ?: fallbackPath ?: error("Artifact path missing"),
            url = this["url"]?.jsonPrimitive?.content ?: error("Artifact URL missing"),
            sha1 = this["sha1"]?.jsonPrimitive?.content.orEmpty(),
            size = this["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        )
    }

    private fun rulesAllow(rules: kotlinx.serialization.json.JsonArray?): Boolean {
        if (rules == null) return true
        var allowed = false
        rules.forEach { ruleElement ->
            val rule = ruleElement.jsonObject
            val os = rule["os"]?.jsonObject
            val name = os?.get("name")?.jsonPrimitive?.content
            val matches = name == null || name == "linux"
            if (matches) allowed = rule["action"]?.jsonPrimitive?.content != "disallow"
        }
        return allowed
    }

    private fun nativeClassifierName(): String = when {
        System.getProperty("os.arch", "").contains("aarch64", true) -> "natives-linux"
        System.getProperty("os.arch", "").contains("arm64", true) -> "natives-linux"
        else -> "natives-linux"
    }
}
