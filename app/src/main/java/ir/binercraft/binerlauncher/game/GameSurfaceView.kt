package ir.binercraft.binerlauncher.game

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import ir.binercraft.binerlauncher.nativebridge.NativeBridge

/** Android game surface. The native layer owns the EGL context and swap chain. */
class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    interface Listener {
        fun onSurfaceReady(surfaceHolder: SurfaceHolder)
        fun onSurfaceReleased()
        fun onTouch(event: MotionEvent): Boolean
    }

    var listener: Listener? = null
    @Volatile private var rendering = false
    private var renderThread: Thread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        listener?.onSurfaceReady(holder)
        rendering = true
        renderThread = Thread({
            val start = System.nanoTime()
            while (rendering && holder.surface.isValid) {
                val elapsed = (System.nanoTime() - start) / 1_000_000_000f
                NativeBridge.renderTestFrame(elapsed)
                try { Thread.sleep(8L) } catch (_: InterruptedException) { break }
            }
        }, "Biner-GL-Render").also { it.start() }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        rendering = false
        renderThread?.interrupt()
        renderThread?.join(300)
        renderThread = null
        listener?.onSurfaceReleased()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun onTouchEvent(event: MotionEvent): Boolean = listener?.onTouch(event) ?: true
}
