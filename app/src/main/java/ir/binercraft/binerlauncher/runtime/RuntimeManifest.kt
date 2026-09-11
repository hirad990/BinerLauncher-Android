package ir.binercraft.binerlauncher.runtime

import kotlinx.serialization.Serializable

@Serializable
data class RuntimeManifest(
    val runtimes: List<RuntimePackage> = emptyList()
)

@Serializable
data class RuntimePackage(
    val major: Int,
    val version: String,
    val abi: String,
    val url: String,
    val sha256: String,
    val archive: String = "zip"
)
