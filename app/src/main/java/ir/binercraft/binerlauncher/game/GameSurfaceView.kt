package ir.binercraft.binerlauncher.game

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent

/** Surface used by the future native Minecraft renderer. */
class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    interface Listener {
        fun onSurfaceReady(surfaceHolder: SurfaceHolder)
        fun onSurfaceReleased()
        fun onTouch(event: MotionEvent): Boolean
    }

    var listener: Listener? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        listener?.onSurfaceReady(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        listener?.onSurfaceReleased()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return listener?.onTouch(event) ?: true
    }
}
