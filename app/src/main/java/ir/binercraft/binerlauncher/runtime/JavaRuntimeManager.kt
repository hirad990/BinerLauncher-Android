package ir.binercraft.binerlauncher.runtime

import android.content.Context
import java.io.File

class JavaRuntimeManager(context: Context) {
    data class RuntimeInfo(
        val major: Int,
        val root: File,
        val executable: File,
        val installed: Boolean
    )

    private val runtimesDir = File(context.filesDir, "runtimes")

    fun runtimeRoot(javaMajor: Int): File = File(runtimesDir, "jre$javaMajor")

    fun javaExecutable(javaMajor: Int): File = File(runtimeRoot(javaMajor), "bin/java")

    fun inspect(javaMajor: Int): RuntimeInfo {
        val executable = javaExecutable(javaMajor)
        return RuntimeInfo(javaMajor, runtimeRoot(javaMajor), executable, executable.isFile)
    }

    fun isInstalled(javaMajor: Int): Boolean = javaExecutable(javaMajor).isFile

    fun installedJavaVersions(): List<Int> = listOf(8, 17, 21).filter(::isInstalled)
}
