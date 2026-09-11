package ir.binercraft.binerlauncher.runtime

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class JavaRuntimeManager(private val context: Context) {
    data class RuntimeInfo(val major: Int, val root: File, val executable: File, val installed: Boolean)
    data class Progress(val stage: String, val downloaded: Long, val total: Long, val fraction: Float)

    private val runtimesDir = File(context.filesDir, "runtimes")
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val DEFAULT_MANIFEST_URL = "https://binercraft.ir/cdn/runtime/manifest.json"
    }

    fun runtimeRoot(javaMajor: Int): File = File(runtimesDir, "jre$javaMajor")
    fun javaExecutable(javaMajor: Int): File = File(runtimeRoot(javaMajor), "bin/java")
    fun inspect(javaMajor: Int): RuntimeInfo = RuntimeInfo(javaMajor, runtimeRoot(javaMajor), javaExecutable(javaMajor), isInstalled(javaMajor))
    fun isInstalled(javaMajor: Int): Boolean = javaExecutable(javaMajor).isFile
    fun installedJavaVersions(): List<Int> = listOf(8, 17, 21).filter(::isInstalled)

    fun currentAbi(): String = when {
        Build.SUPPORTED_ABIS.any { it == "arm64-v8a" } -> "arm64"
        Build.SUPPORTED_ABIS.any { it == "armeabi-v7a" } -> "arm"
        Build.SUPPORTED_ABIS.any { it == "x86_64" } -> "x86_64"
        else -> "x86"
    }

    suspend fun installIfMissing(javaMajor: Int, manifestUrl: String = DEFAULT_MANIFEST_URL, onProgress: (Progress) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (isInstalled(javaMajor)) return@withContext
        val manifest = json.decodeFromString<RuntimeManifest>(downloadText(manifestUrl))
        val pkg = manifest.runtimes.firstOrNull { it.major == javaMajor && it.abi.equals(currentAbi(), true) }
            ?: error("No Android Java $javaMajor runtime is available for ${currentAbi()}")
        runtimesDir.mkdirs()
        val temp = File(runtimesDir, ".jre$javaMajor.download")
        val archive = File(runtimesDir, ".jre$javaMajor.${pkg.archive}")
        val target = runtimeRoot(javaMajor)
        val staging = File(runtimesDir, ".jre$javaMajor.staging")
        onProgress(Progress("Downloading Java $javaMajor", 0, -1, 0f))
        download(pkg.url, temp, onProgress)
        verifySha256(temp, pkg.sha256)
        temp.renameTo(archive)
        staging.deleteRecursively(); staging.mkdirs()
        onProgress(Progress("Installing Java $javaMajor", 0, 1, 0f))
        when (pkg.archive.lowercase()) { "zip" -> unzip(archive, staging); else -> error("Unsupported runtime archive: ${pkg.archive}") }
        val root = locateRuntimeRoot(staging)
        target.deleteRecursively()
        if (!root.renameTo(target)) root.copyRecursively(target, overwrite = true)
        javaExecutable(javaMajor).setExecutable(true, false)
        check(isInstalled(javaMajor)) { "Java $javaMajor installation is incomplete" }
        staging.deleteRecursively(); archive.delete(); temp.delete()
        onProgress(Progress("Java $javaMajor ready", 1, 1, 1f))
    }

    private fun downloadText(url: String): String = URL(url).openStream().bufferedReader().use { it.readText() }

    private fun download(url: String, destination: File, onProgress: (Progress) -> Unit) {
        val c = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 20_000; readTimeout = 60_000; instanceFollowRedirects = true }
        c.connect(); if (c.responseCode !in 200..299) error("Runtime download failed: HTTP ${c.responseCode}")
        val total = c.contentLengthLong; var done = 0L
        FileOutputStream(destination).use { out -> c.inputStream.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val n = input.read(buffer); if (n <= 0) break; out.write(buffer, 0, n); done += n; onProgress(Progress("Downloading Java", done, total, if (total > 0) done.toFloat() / total else 0f)) }
        } }
        c.disconnect()
    }

    private fun verifySha256(file: File, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input -> val buffer = ByteArray(64 * 1024); while (true) { val n = input.read(buffer); if (n <= 0) break; digest.update(buffer, 0, n) } }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        check(actual.equals(expected, true)) { "Runtime checksum mismatch" }
    }

    private fun unzip(file: File, destination: File) {
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val safe = File(destination, entry.name)
                require(safe.canonicalPath.startsWith(destination.canonicalPath + File.separator)) { "Unsafe runtime archive" }
                if (entry.isDirectory) safe.mkdirs() else { safe.parentFile?.mkdirs(); FileOutputStream(safe).use { zip.copyTo(it) } }
            }
        }
    }

    private fun locateRuntimeRoot(staging: File): File {
        if (File(staging, "bin/java").isFile) return staging
        return staging.listFiles().orEmpty().firstOrNull { it.isDirectory && File(it, "bin/java").isFile }
            ?: error("Runtime archive does not contain bin/java")
    }
}
