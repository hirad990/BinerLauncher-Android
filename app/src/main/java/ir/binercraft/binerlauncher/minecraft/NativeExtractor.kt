package ir.binercraft.binerlauncher.minecraft

import java.io.File
import java.util.zip.ZipFile

object NativeExtractor {
    fun extract(jar: File, destination: File) {
        require(jar.isFile) { "Native library archive not found: ${jar.absolutePath}" }
        destination.mkdirs()
        ZipFile(jar).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("META-INF/").not() }
                .forEach { entry ->
                    val relative = entry.name.removePrefix("META-INF/")
                    val output = File(destination, relative)
                    val canonicalRoot = destination.canonicalFile.toPath()
                    val canonicalOutput = output.canonicalFile.toPath()
                    require(canonicalOutput.startsWith(canonicalRoot)) { "Unsafe native archive entry: ${entry.name}" }
                    output.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input -> output.outputStream().use { out -> input.copyTo(out) } }
                }
        }
    }
}
