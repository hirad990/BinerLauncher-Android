package ir.binercraft.binerlauncher.runtime

import android.content.Context
import java.io.File

class JavaRuntimeManager(context: Context) {
    private val runtimesDir = File(context.filesDir, "runtimes")

    fun runtimeRoot(javaMajor: Int): File = File(runtimesDir, "jre$javaMajor")

    fun javaExecutable(javaMajor: Int): File {
        val root = runtimeRoot(javaMajor)
        return File(root, "bin/java")
    }

    fun isInstalled(javaMajor: Int): Boolean = javaExecutable(javaMajor).isFile

    fun installedJavaVersions(): List<Int> = listOf(8, 17, 21).filter(::isInstalled)
}
