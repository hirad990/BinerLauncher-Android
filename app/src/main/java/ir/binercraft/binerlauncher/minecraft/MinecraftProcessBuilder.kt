package ir.binercraft.binerlauncher.minecraft

import java.io.File

class MinecraftProcessBuilder {
    data class LaunchSpec(
        val java: File,
        val classpath: String,
        val mainClass: String,
        val workingDirectory: File,
        val jvmArgs: List<String> = emptyList(),
        val gameArgs: List<String> = emptyList()
    )

    fun command(spec: LaunchSpec): List<String> = buildList {
        add(spec.java.absolutePath)
        addAll(spec.jvmArgs)
        add("-cp")
        add(spec.classpath)
        add(spec.mainClass)
        addAll(spec.gameArgs)
    }

    fun start(spec: LaunchSpec): Process =
        ProcessBuilder(command(spec))
            .directory(spec.workingDirectory)
            .redirectErrorStream(true)
            .start()
}
