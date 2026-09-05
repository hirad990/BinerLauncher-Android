package ir.binercraft.binerlauncher.minecraft

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MinecraftDownloadManager(private val context: Context) {
    private val minecraftDir = File(context.filesDir, "minecraft")

    fun versionDirectory(versionId: String): File = File(minecraftDir, "versions/$versionId")

    fun downloadVersionJson(versionId: String, url: String): File {
        val directory = versionDirectory(versionId).apply { mkdirs() }
        val target = File(directory, "$versionId.json")
        download(url, target)
        return target
    }

    fun downloadClientJar(versionId: String, url: String): File {
        val directory = versionDirectory(versionId).apply { mkdirs() }
        val target = File(directory, "$versionId.jar")
        download(url, target)
        return target
    }

    private fun download(url: String, target: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "BinerLauncher-Android/0.2")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Download failed: HTTP ${connection.responseCode}")
            }
            val temporary = File(target.parentFile, "${target.name}.part")
            connection.inputStream.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output, BUFFER_SIZE) }
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object { private const val BUFFER_SIZE = 64 * 1024 }
}
