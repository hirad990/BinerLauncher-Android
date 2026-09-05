package ir.binercraft.binerlauncher.minecraft

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Coordinates the final install -> plan -> execute path for a Minecraft instance. */
class LaunchOrchestrator(context: Context) {
    private val paths = MinecraftPaths(context)
    private val runtimeManager = ir.binercraft.binerlauncher.runtime.JavaRuntimeManager(context)
    private val planner = MinecraftLaunchPlanner(paths, runtimeManager)
    private val executor = MinecraftLaunchExecutor()

    data class Result(
        val command: List<String>,
        val process: Process
    )

    suspend fun launch(
        versionId: String,
        username: String,
        uuid: String,
        accessToken: String = "0",
        width: Int = 1280,
        height: Int = 720,
        maxMemoryMb: Int = 2048,
        extraJvmArgs: List<String> = emptyList(),
        extraGameArgs: List<String> = emptyList()
    ): Result = withContext(Dispatchers.IO) {
        paths.ensureDirectories()
        val resolved = VersionResolver(paths).resolve(versionId)
        require(resolved.clientPath(paths).isFile) {
            "Minecraft client is not installed: $versionId"
        }
        require(runtimeManager.isInstalled(resolved.javaMajor)) {
            "Java ${resolved.javaMajor} runtime is not installed"
        }

        val plan = planner.createPlan(
            version = resolved,
            username = username,
            uuid = uuid,
            accessToken = accessToken,
            width = width,
            height = height,
            maxMemoryMb = maxMemoryMb,
            extraJvmArgs = extraJvmArgs,
            extraGameArgs = extraGameArgs
        )
        val process = executor.start(plan)
        Result(plan.command, process)
    }

    private fun ResolvedVersion.clientPath(paths: MinecraftPaths): File = paths.clientJar(id)
}
