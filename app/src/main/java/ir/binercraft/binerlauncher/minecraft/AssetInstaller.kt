package ir.binercraft.binerlauncher.minecraft

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

class AssetInstaller(private val paths: MinecraftPaths) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun install(indexUrl: String, indexId: String): Int = withContext(Dispatchers.IO) {
        val indexFile = File(paths.assets, "indexes/$indexId.json")
        indexFile.parentFile?.mkdirs()
        download(indexUrl, indexFile)

        val objects = json.parseToJsonElement(indexFile.readText()).jsonObject["objects"]?.jsonObject
            ?: return@withContext 0
        var count = 0
        for ((name, value) in objects) {
            val hash = value.jsonObject["hash"]?.jsonPrimitive?.content ?: continue
            if (hash.length < 2) continue
            val prefix = hash.substring(0, 2)
            val objectFile = File(paths.assets, "objects/$prefix/$hash")
            if (!objectFile.isFile) {
                download("https://resources.download.minecraft.net/$prefix/$hash", objectFile)
            }
            count++
        }
        count
    }

    private fun download(url: String, destination: File) {
        destination.parentFile?.mkdirs()
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.requestMethod = "GET"
        try {
            if (connection.responseCode !in 200..299) error("Asset download HTTP ${connection.responseCode}")
            connection.inputStream.use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
        } finally {
            connection.disconnect()
        }
    }
}
