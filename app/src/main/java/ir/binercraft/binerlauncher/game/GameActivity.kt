package ir.binercraft.binerlauncher.game

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.FrameLayout
import ir.binercraft.binerlauncher.nativebridge.NativeBridge

class GameActivity : Activity(), GameSurfaceView.Listener {
    private lateinit var root: FrameLayout
    private lateinit var gameSurface: GameSurfaceView
    private lateinit var touchHud: TouchHudView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        root = FrameLayout(this)
        gameSurface = GameSurfaceView(this).also { it.listener = this }
        touchHud = TouchHudView(this)
        root.addView(gameSurface, FrameLayout.LayoutParams(-1, -1))
        root.addView(touchHud, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    override fun onSurfaceReady(surfaceHolder: SurfaceHolder) {
        NativeBridge.attachSurface(surfaceHolder.surface)
    }

    override fun onSurfaceReleased() {
        NativeBridge.detachSurface()
    }

    override fun onTouch(event: MotionEvent): Boolean {
        NativeBridge.dispatchTouch(event.actionMasked, event.x, event.y)
        return true
    }

    override fun onDestroy() {
        NativeBridge.detachSurface()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_VERSION = "version"
        const val EXTRA_PID = "pid"
    }
}
