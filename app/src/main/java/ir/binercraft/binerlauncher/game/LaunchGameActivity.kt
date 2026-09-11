package ir.binercraft.binerlauncher.game

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import ir.binercraft.binerlauncher.minecraft.LaunchOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LaunchGameActivity : ComponentActivity() {
    private val launchScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val version = intent.getStringExtra(EXTRA_VERSION) ?: run { finish(); return }
        val username = intent.getStringExtra(EXTRA_USERNAME) ?: "Player"
        val uuid = intent.getStringExtra(EXTRA_UUID) ?: "00000000-0000-0000-0000-000000000000"
        showLoading("Starting BinerLauncher…")

        launchScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    LaunchOrchestrator(this@LaunchGameActivity).launch(version, username, uuid) { message ->
                        runOnUiThread { status.text = message }
                    }
                }

                if (!result.process.isAlive) {
                    error("Minecraft process exited immediately (exit=${result.process.exitValue()})")
                }

                val game = Intent(this@LaunchGameActivity, GameActivity::class.java).apply {
                    putExtra(GameActivity.EXTRA_VERSION, version)
                    putExtra(GameActivity.EXTRA_PID, result.process.pid())
                }
                startActivity(game)
                finish()
            } catch (error: Throwable) {
                android.util.Log.e("BinerLauncher", "Minecraft launch failed", error)
                runOnUiThread { status.text = "Launch failed\n${error.message ?: error.javaClass.simpleName}" }
            }
        }
    }

    private fun showLoading(message: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.rgb(8, 10, 16))
        }
        val title = TextView(this).apply { text = "BINER LAUNCHER"; textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        status = TextView(this).apply { text = message; textSize = 17f; setTextColor(Color.rgb(34, 211, 238)); gravity = Gravity.CENTER; setPadding(0, 24, 0, 20) }
        val progress = ProgressBar(this).apply { isIndeterminate = true }
        root.addView(title); root.addView(status); root.addView(progress)
        setContentView(root)
    }

    override fun onDestroy() { launchScope.cancel(); super.onDestroy() }

    companion object {
        const val EXTRA_VERSION = "version"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_UUID = "uuid"
    }
}
