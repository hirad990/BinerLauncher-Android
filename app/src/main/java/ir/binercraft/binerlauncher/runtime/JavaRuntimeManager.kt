package ir.binercraft.binerlauncher.runtime

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import java.io.InputStream

class JavaRuntimeManager(private val context: Context) {
    data class RuntimeInfo(val major: Int, val root: File, val executable: File, val installed: Boolean)
    data class Progress(val stage: String, val downloaded: Long, val total: Long, val fraction: Float)

    private val runtimesDir = File(context.filesDir, "runtimes")
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val DEFAULT_MANIFEST_URL = "https://raw.githubusercontent.com/hirad990/BinerLauncher-Android/main/runtime-manifest.json"
    }

    fun runtimeRoot(javaMajor: Int): File = File(runtimesDir, "jre$javaMajor")
    fun javaExecutable(javaMajor: Int): File = File(runtimeRoot(javaMajor), "bin/java")
    fun inspect(javaMajor: Int): RuntimeInfo = RuntimeInfo(javaMajor, runtimeRoot(javaMajor), javaExecutable(javaMajor), isInstalled(javaMajor))
    fun isInstalled(javaMajor: Int): Boolean = javaExecutable(javaMajor).isFile && javaExecutable(javaMajor).canExecute()
    fun installedJavaVersions(): List<Int> = listOf(8, 17, 21, 25).filter(::isInstalled)

    fun currentAbi(): String = when {
        Build.SUPPORTED_ABIS.any { it == "arm64-v8a" } -> "arm64"
        Build.SUPPORTED_ABIS.any { it == "armeabi-v7a" } -> "arm"
        Build.SUPPORTED_ABIS.any { it == "x86_64" } -> "x86_64"
        else -> "x86"
    }

    suspend fun installIfMissing(
        javaMajor: Int,
        manifestUrl: String = DEFAULT_MANIFEST_URL,
        onProgress: (Progress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        if (isInstalled(javaMajor)) return@withContext

        val manifest = json.decodeFromString<RuntimeManifest>(downloadText(manifestUrl))
        val pkg = manifest.runtimes.firstOrNull {
            it.major == javaMajor && it.abi.equals(currentAbi(), true)
        } ?: error("No Android Java $javaMajor runtime is available for ${currentAbi()}")

        runtimesDir.mkdirs()
        val partial = File(runtimesDir, ".jre$javaMajor.download")
        val archive = File(runtimesDir, ".jre$javaMajor.archive")
        val target = runtimeRoot(javaMajor)
        val staging = File(runtimesDir, ".jre$javaMajor.staging")

        onProgress(Progress("Preparing Java $javaMajor", 0, -1, 0f))
        downloadResumable(pkg.url, partial, onProgress)
        verifySha256(partial, pkg.sha256)
        archive.delete()
        check(partial.renameTo(archive)) { "Unable to prepare Java archive" }

        staging.deleteRecursively()
        staging.mkdirs()
        onProgress(Progress("Extracting Java $javaMajor", 0, 1, 0f))
        when (pkg.archive.lowercase()) {
            "zip" -> unzip(archive, staging)
            "tar.xz", "txz" -> untarXz(archive, staging)
            else -> error("Unsupported runtime archive: ${pkg.archive}")
        }

        val root = locateRuntimeRoot(staging)
        target.deleteRecursively()
        if (!root.renameTo(target)) root.copyRecursively(target, overwrite = true)
        makeRuntimeExecutables(target)
        File(target, ".biner-runtime-version").writeText(pkg.version)
        check(isInstalled(javaMajor)) { "Java $javaMajor installation is incomplete" }

        staging.deleteRecursively()
        archive.delete()
        partial.delete()
        onProgress(Progress("Java $javaMajor ready", 1, 1, 1f))
    }

    private fun downloadText(url: String): String = URL(url).openStream().bufferedReader().use { it.readText() }

    private fun downloadResumable(url: String, destination: File, onProgress: (Progress) -> Unit) {
        var offset = if (destination.isFile) destination.length() else 0L
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 120_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "BinerLauncher/0.2")
                if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
            }
            connection.connect()
            val append = offset > 0 && connection.responseCode == HttpURLConnection.HTTP_PARTIAL
            if (!append) {
                if (offset > 0) destination.delete()
                offset = 0
            }
            if (connection.responseCode !in 200..299) error("Runtime download failed: HTTP ${connection.responseCode}")
            val length = connection.contentLengthLong
            val total = if (length >= 0) length + offset else -1L
            var done = offset
            FileOutputStream(destination, append).use { out ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        out.write(buffer, 0, n)
                        done += n
                        onProgress(Progress("Downloading Java", done, total, if (total > 0) done.toFloat() / total else 0f))
                    }
                }
            }
        } finally {
            connection?.disconnect()
        }
    }

    private fun verifySha256(file: File, expected: String) {
        require(expected.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid runtime SHA-256" }
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                digest.update(buffer, 0, n)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        check(actual.equals(expected, true)) { "Runtime checksum mismatch" }
    }

    private fun unzip(file: File, destination: File) {
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                extractEntry(entry.name, entry.isDirectory, zip, destination)
            }
        }
    }

    private fun untarXz(file: File, destination: File) {
        XZCompressorInputStream(file.inputStream().buffered()).use { xz ->
            TarArchiveInputStream(xz).use { tar ->
                while (true) {
                    val entry = tar.nextTarEntry ?: break
                    extractEntry(entry.name, entry.isDirectory, tar, destination)
                }
            }
        }
    }

    private fun extractEntry(name: String, directory: Boolean, input: InputStream, destination: File) {
        val safe = File(destination, name)
        require(safe.canonicalPath.startsWith(destination.canonicalPath + File.separator)) { "Unsafe runtime archive" }
        if (directory) safe.mkdirs() else {
            safe.parentFile?.mkdirs()
            FileOutputStream(safe).use { input.copyTo(it) }
        }
    }

    private fun makeRuntimeExecutables(root: File) {
        File(root, "bin").walkTopDown().filter { it.isFile }.forEach { it.setExecutable(true, false) }
    }

    private fun locateRuntimeRoot(staging: File): File {
        if (File(staging, "bin/java").isFile) return staging
        return staging.walkTopDown().firstOrNull { it.isDirectory && File(it, "bin/java").isFile }
            ?: error("Runtime archive does not contain bin/java")
    }
}
