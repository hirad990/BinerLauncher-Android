package ir.binercraft.binerlauncher.minecraft

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MinecraftLaunchExecutor {
    suspend fun launch(plan: LaunchPlan): Process = withContext(Dispatchers.IO) {
        val processBuilder = ProcessBuilder(plan.command)
            .directory(plan.workingDirectory)
            .redirectErrorStream(true)

        processBuilder.environment().putAll(plan.environment)
        processBuilder.start()
    }
}
