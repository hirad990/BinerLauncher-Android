package ir.binercraft.binerlauncher.nativebridge

import android.view.Surface

object NativeBridge {
    init {
        System.loadLibrary("binerlauncher-native")
    }

    external fun nativeGetEngineInfo(): String
    external fun nativeAbiVersion(): Int
    external fun nativeAttachSurface(surface: Surface)
    external fun nativeDetachSurface()
    external fun nativeDispatchTouch(action: Int, x: Float, y: Float)

    fun attachSurface(surface: Surface) = nativeAttachSurface(surface)
    fun detachSurface() = nativeDetachSurface()
    fun dispatchTouch(action: Int, x: Float, y: Float) = nativeDispatchTouch(action, x, y)
}
