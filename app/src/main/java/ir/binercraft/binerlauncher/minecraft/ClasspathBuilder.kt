package ir.binercraft.binerlauncher.minecraft

import java.io.File

object ClasspathBuilder {
    fun build(libraries: List<File>, clientJar: File): String =
        (libraries.filter(File::isFile) + clientJar.takeIf(File::isFile).orEmpty())
            .joinToString(File.pathSeparator) { it.absolutePath }
}
