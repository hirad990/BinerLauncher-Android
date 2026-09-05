package ir.binercraft.binerlauncher.minecraft

object LaunchCommandBuilder {
    fun buildGameArguments(
        username: String,
        uuid: String,
        accessToken: String,
        version: String,
        gameDirectory: String,
        assetsDirectory: String,
        assetIndex: String,
        nativesDirectory: String,
        launcherName: String = "BinerLauncher",
        launcherVersion: String = "0.1.0"
    ): List<String> = listOf(
        "--username", username,
        "--uuid", uuid,
        "--accessToken", accessToken,
        "--version", version,
        "--gameDir", gameDirectory,
        "--assetsDir", assetsDirectory,
        "--assetIndex", assetIndex,
        "--nativesDir", nativesDirectory,
        "--userType", "msa",
        "--versionType", "BinerLauncher",
        "--launcherName", launcherName,
        "--launcherVersion", launcherVersion
    )
}
