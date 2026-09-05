package ir.binercraft.binerlauncher.core

/** Metadata for a Minecraft Java Edition version. */
data class MinecraftVersion(
    val id: String,
    val type: String,
    val releaseTime: String,
    val url: String? = null,
    val sha1: String? = null
)

data class GameProfile(
    val id: String,
    val name: String,
    val accountType: AccountType = AccountType.LOCAL
)

enum class AccountType {
    LOCAL,
    MICROSOFT
}

data class LaunchOptions(
    val versionId: String,
    val gameDirectory: String,
    val javaPath: String,
    val memoryMb: Int = 2048,
    val width: Int = 1280,
    val height: Int = 720
)

enum class LauncherState {
    IDLE,
    CHECKING_RUNTIME,
    DOWNLOADING,
    PREPARING,
    LAUNCHING,
    RUNNING,
    ERROR
}
