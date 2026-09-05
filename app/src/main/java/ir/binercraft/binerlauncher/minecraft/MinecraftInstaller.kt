package ir.binercraft.binerlauncher.minecraft

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

/** Installs an official Minecraft Java version and its required runtime artifacts. */
class MinecraftInstaller(
    private val paths: MinecraftPaths,
    private val downloader: ArtifactDownloader = ArtifactDownloader(),
    private val assetInstaller: AssetInstaller = AssetInstaller(paths)
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun install(versionId: String, metadataUrl: String): ResolvedVersion = withContext(Dispatchers.IO) {
        paths.ensureDirectories()
        val versionDir = paths.versionDirectory(versionId)
        versionDir.mkdirs()

        val metadata = fetchText(metadataUrl)
        paths.versionJson(versionId).writeText(metadata)

        val root = json.parseToJsonElement(metadata).jsonObject
        val downloads = root["downloads"]?.jsonObject
            ?: error("Version $versionId has no downloads section")
        val client = downloads["client"]?.jsonObject
            ?: error("Version $versionId has no client download")

        val clientArtifact = VersionArtifact(
            path = "versions/$versionId/$versionId.jar",
            url = client["url"]!!.jsonPrimitive.content,
            sha1 = client["sha1"]?.jsonPrimitive?.content.orEmpty(),
            size = client["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        )
        downloader.download(clientArtifact, paths.clientJar(versionId))

        val resolved = VersionResolver(paths).resolve(versionId)
        resolved.libraries.forEach { artifact ->
            downloader.download(artifact, File(paths.libraries, artifact.path))
        }
        resolved.natives.forEach { artifact ->
            val nativeJar = File(paths.libraries, artifact.path)
            downloader.download(artifact, nativeJar)
            NativeExtractor.extract(nativeJar, File(paths.natives, versionId))
        }
        if (resolved.assetIndexUrl != null && resolved.assetIndexId != null) {
            assetInstaller.install(resolved.assetIndexUrl, resolved.assetIndexId)
        }
        resolved
    }

    private fun fetchText(url: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        return try {
            if (connection.responseCode !in 200..299) {
                error("Minecraft metadata HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
