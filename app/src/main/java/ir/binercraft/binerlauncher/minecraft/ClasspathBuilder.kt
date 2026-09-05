package ir.binercraft.binerlauncher.minecraft

import java.io.File

object ClasspathBuilder {
    fun build(libraries: List<File>, clientJar: File): String {
        val entries = libraries.filter(File::isFile).toMutableList()
        if (clientJar.isFile) entries += clientJar
        require(entries.isNotEmpty()) { "Minecraft classpath is empty" }
        return entries.joinToString(File.pathSeparator) { it.absolutePath }
    }
}
