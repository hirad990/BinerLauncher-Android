package ir.binercraft.binerlauncher.game

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.WindowManager
import ir.binercraft.binerlauncher.nativebridge.NativeBridge

class GameActivity : Activity(), GameSurfaceView.Listener {
    private lateinit var gameSurface: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gameSurface = GameSurfaceView(this)
        gameSurface.listener = this
        setContentView(gameSurface)
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
}
