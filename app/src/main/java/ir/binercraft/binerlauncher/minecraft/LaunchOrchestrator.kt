package ir.binercraft.binerlauncher.minecraft

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Final install -> resolve -> plan -> execute bridge used by the launcher UI. */
class LaunchOrchestrator(context: Context) {
    private val paths = MinecraftPaths(context)
    private val runtimes = ir.binercraft.binerlauncher.runtime.JavaRuntimeManager(context)
    private val planner = MinecraftLaunchPlanner(paths, runtimes)
    private val executor = MinecraftLaunchExecutor()

    data class Result(
        val plan: LaunchPlan,
        val process: Process
    )

    suspend fun launch(
        versionId: String,
        username: String,
        uuid: String,
        accessToken: String = "0",
        userType: String = "mojang",
        xuid: String? = null,
        clientId: String? = null,
        width: Int = 1280,
        height: Int = 720,
        memoryMb: Int = 2048,
        extraJvmArgs: List<String> = emptyList(),
        extraGameArgs: List<String> = emptyList()
    ): Result = withContext(Dispatchers.IO) {
        paths.ensureDirectories()
        val resolved = VersionResolver(paths).resolve(versionId)

        require(paths.clientJar(versionId).isFile) {
            "Minecraft client is not installed: $versionId"
        }
        require(runtimes.isInstalled(resolved.javaMajor)) {
            "Java ${resolved.javaMajor} runtime is not installed"
        }

        val profile = LaunchProfile(
            username = username,
            uuid = uuid,
            accessToken = accessToken,
            userType = userType,
            xuid = xuid,
            clientId = clientId
        )
        val options = LaunchOptions(
            memoryMb = memoryMb,
            width = width,
            height = height,
            extraJvmArgs = extraJvmArgs,
            extraGameArgs = extraGameArgs
        )

        val plan = planner.plan(resolved, profile, options)
        val process = executor.launch(plan)
        Result(plan, process)
    }
}
