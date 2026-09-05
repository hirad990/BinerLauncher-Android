package ir.binercraft.binerlauncher.nativebridge

object NativeBridge {
    init {
        System.loadLibrary("binerlauncher-native")
    }

    external fun nativeGetEngineInfo(): String
    external fun nativeAbiVersion(): Int
}
