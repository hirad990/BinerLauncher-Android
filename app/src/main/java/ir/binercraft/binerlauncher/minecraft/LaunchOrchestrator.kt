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
    private val installer = MinecraftInstaller(paths)

    data class Result(val plan: LaunchPlan, val process: Process)

    suspend fun launch(
        versionId: String,
        username: String,
        uuid: String,
        metadataUrl: String? = null,
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

        if (!paths.versionJson(versionId).isFile || !paths.clientJar(versionId).isFile) {
            val url = metadataUrl ?: MinecraftVersionRepository().fetchVersions()
                .firstOrNull { it.id == versionId }?.url
                ?: error("Minecraft metadata URL not found for $versionId")
            installer.install(versionId, url)
        }

        val resolved = VersionResolver(paths).resolve(versionId)
        require(runtimes.isInstalled(resolved.javaMajor)) {
            "Java ${resolved.javaMajor} runtime is not installed. Install the Android-compatible Java runtime for this Minecraft version first."
        }

        val profile = LaunchProfile(username, uuid, accessToken, userType, xuid, clientId)
        val options = LaunchOptions(memoryMb, width, height, extraJvmArgs, extraGameArgs)
        val plan = planner.plan(resolved, profile, options)
        Result(plan, executor.launch(plan))
    }
}
