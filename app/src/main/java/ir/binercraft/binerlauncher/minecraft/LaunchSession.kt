package ir.binercraft.binerlauncher.minecraft

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class LaunchSession {
    suspend fun run(spec: MinecraftProcessBuilder.LaunchSpec): Int = withContext(Dispatchers.IO) {
        val process = MinecraftProcessBuilder().start(spec)
        BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
            lines.forEach { line -> println("[Minecraft] $line") }
        }
        process.waitFor()
    }
}
