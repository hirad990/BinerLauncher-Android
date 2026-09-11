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
    external fun nativeDispatchKey(action: Int, keyCode: Int)
    external fun nativeDispatchMouse(action: Int, button: Int)
    external fun nativeDispatchScroll(amount: Float)
    external fun nativePollInput(): Long

    fun attachSurface(surface: Surface) = nativeAttachSurface(surface)
    fun detachSurface() = nativeDetachSurface()
    fun dispatchTouch(action: Int, x: Float, y: Float) = nativeDispatchTouch(action, x, y)
    fun keyDown(keyCode: Int) = nativeDispatchKey(0, keyCode)
    fun keyUp(keyCode: Int) = nativeDispatchKey(1, keyCode)
    fun mouseDown(button: Int) = nativeDispatchMouse(0, button)
    fun mouseUp(button: Int) = nativeDispatchMouse(1, button)
    fun scroll(amount: Float) = nativeDispatchScroll(amount)
    fun pollInput(): Long = nativePollInput()
}
