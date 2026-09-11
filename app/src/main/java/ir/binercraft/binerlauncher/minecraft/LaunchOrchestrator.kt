package ir.binercraft.binerlauncher.minecraft

import android.content.Context
import ir.binercraft.binerlauncher.runtime.JavaRuntimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LaunchOrchestrator(context: Context) {
    private val paths = MinecraftPaths(context)
    private val runtimes = JavaRuntimeManager(context)
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
        extraGameArgs: List<String> = emptyList(),
        onProgress: (String) -> Unit = {}
    ): Result = withContext(Dispatchers.IO) {
        paths.ensureDirectories()
        onProgress("Checking Minecraft $versionId")
        if (!paths.versionJson(versionId).isFile || !paths.clientJar(versionId).isFile) {
            onProgress("Downloading Minecraft $versionId")
            val url = metadataUrl ?: MinecraftVersionRepository().fetchVersions()
                .firstOrNull { it.id == versionId }?.url
                ?: error("Minecraft metadata URL not found for $versionId")
            installer.install(versionId, url)
        }
        val resolved = VersionResolver(paths).resolve(versionId)
        onProgress("Preparing Java ${resolved.javaMajor}")
        runtimes.installIfMissing(resolved.javaMajor) { progress ->
            val percent = if (progress.fraction > 0f) " ${(progress.fraction * 100).toInt()}%" else ""
            onProgress(progress.stage + percent)
        }
        onProgress("Preparing game files")
        val profile = LaunchProfile(username, uuid, accessToken, userType, xuid, clientId)
        val options = LaunchOptions(memoryMb, width, height, extraJvmArgs, extraGameArgs)
        val plan = planner.plan(resolved, profile, options)
        onProgress("Launching Minecraft")
        Result(plan, executor.launch(plan))
    }
}
