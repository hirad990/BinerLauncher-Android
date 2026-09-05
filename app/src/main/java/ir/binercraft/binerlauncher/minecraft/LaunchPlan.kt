package ir.binercraft.binerlauncher.minecraft

import java.io.File

data class LaunchProfile(
    val username: String,
    val uuid: String,
    val accessToken: String,
    val userType: String = "msa",
    val xuid: String? = null,
    val clientId: String? = null
)

data class LaunchOptions(
    val memoryMb: Int = 2048,
    val width: Int = 1280,
    val height: Int = 720,
    val extraJvmArgs: List<String> = emptyList(),
    val extraGameArgs: List<String> = emptyList()
)

data class LaunchPlan(
    val javaExecutable: File,
    val workingDirectory: File,
    val command: List<String>,
    val environment: Map<String, String>,
    val version: ResolvedVersion
)
