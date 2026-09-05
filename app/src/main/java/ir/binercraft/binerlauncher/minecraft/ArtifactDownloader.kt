package ir.binercraft.binerlauncher.minecraft

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

class ArtifactDownloader {
    suspend fun download(artifact: VersionArtifact, destination: File): File = withContext(Dispatchers.IO) {
        destination.parentFile?.mkdirs()
        val connection = URI(artifact.url).toURL().openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.requestMethod = "GET"
        connection.connect()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Download failed: HTTP ${connection.responseCode}")
        }
        connection.inputStream.use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
        if (artifact.sha1.isNotBlank()) {
            val actual = sha1(destination)
            require(actual.equals(artifact.sha1, ignoreCase = true)) {
                "SHA-1 mismatch for ${artifact.path}: expected ${artifact.sha1}, got $actual"
            }
        }
        destination
    }

    private fun sha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
