package ir.binercraft.binerlauncher.game

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import ir.binercraft.binerlauncher.minecraft.LaunchOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Launches the installed Minecraft process and keeps the game surface alive. */
class LaunchGameActivity : ComponentActivity() {
    private var process: Process? = null
    private val launchScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val version = intent.getStringExtra(EXTRA_VERSION) ?: run {
            finish()
            return
        }
        val username = intent.getStringExtra(EXTRA_USERNAME) ?: "Player"
        val uuid = intent.getStringExtra(EXTRA_UUID) ?: "00000000-0000-0000-0000-000000000000"

        launchScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    LaunchOrchestrator(this@LaunchGameActivity).launch(
                        versionId = version,
                        username = username,
                        uuid = uuid
                    )
                }
                process = result.process
                withContext(Dispatchers.IO) {
                    result.process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { android.util.Log.i("BinerMinecraft", it) }
                    }
                }
            } catch (error: Throwable) {
                android.util.Log.e("BinerLauncher", "Minecraft launch failed", error)
                if (!isFinishing) finish()
            }
        }
    }

    override fun onDestroy() {
        launchScope.cancel()
        process?.takeIf { it.isAlive }?.destroy()
        process = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_VERSION = "version"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_UUID = "uuid"
    }
}
