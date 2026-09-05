package ir.binercraft.binerlauncher.minecraft

data class VersionArtifact(
    val path: String,
    val url: String,
    val sha1: String,
    val size: Long,
    val required: Boolean = true
)

data class ResolvedVersion(
    val id: String,
    val mainClass: String,
    val releaseType: String,
    val javaMajor: Int,
    val client: VersionArtifact,
    val libraries: List<VersionArtifact>,
    val assetIndexUrl: String?,
    val assetIndexId: String?
)
