package ir.binercraft.binerlauncher.minecraft

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URI

class VersionResolver(private val paths: MinecraftPaths) {
    private val json = Json { ignoreUnknownKeys = true }

    fun resolve(versionId: String): ResolvedVersion {
        val file = paths.versionJson(versionId)
        require(file.isFile) { "Version metadata not installed: $versionId" }
        val root = json.parseToJsonElement(file.readText()).jsonObject
        val downloads = root["downloads"]!!.jsonObject
        val client = downloads["client"]!!.jsonObject
        val javaMajor = root["javaVersion"]?.jsonObject?.get("majorVersion")?.jsonPrimitive?.int ?: 17
        val libraries = root["libraries"]?.jsonArray.orEmpty().mapNotNull { element ->
            val lib = element.jsonObject
            val artifact = lib["downloads"]?.jsonObject?.get("artifact")?.jsonObject ?: return@mapNotNull null
            VersionArtifact(
                path = artifact["path"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                url = artifact["url"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                sha1 = artifact["sha1"]?.jsonPrimitive?.content.orEmpty(),
                size = artifact["size"]?.jsonPrimitive?.long ?: 0L,
                required = true
            )
        }
        return ResolvedVersion(
            id = root["id"]!!.jsonPrimitive.content,
            mainClass = root["mainClass"]!!.jsonPrimitive.content,
            releaseType = root["type"]?.jsonPrimitive?.content ?: "release",
            javaMajor = javaMajor,
            client = VersionArtifact(
                path = "versions/$versionId/$versionId.jar",
                url = client["url"]!!.jsonPrimitive.content,
                sha1 = client["sha1"]?.jsonPrimitive?.content.orEmpty(),
                size = client["size"]?.jsonPrimitive?.long ?: 0L
            ),
            libraries = libraries,
            assetIndexUrl = root["assetIndex"]?.jsonObject?.get("url")?.jsonPrimitive?.content,
            assetIndexId = root["assetIndex"]?.jsonObject?.get("id")?.jsonPrimitive?.content
        )
    }
}
