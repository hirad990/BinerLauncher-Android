package ir.binercraft.binerlauncher.minecraft

import android.content.Context
import java.io.File

class MinecraftPaths(context: Context) {
    val root: File = File(context.filesDir, "minecraft")
    val versions: File = File(root, "versions")
    val libraries: File = File(root, "libraries")
    val assets: File = File(root, "assets")
    val natives: File = File(root, "natives")
    val instances: File = File(root, "instances")

    fun versionDirectory(id: String): File = File(versions, id)
    fun versionJson(id: String): File = File(versionDirectory(id), "$id.json")
    fun clientJar(id: String): File = File(versionDirectory(id), "$id.jar")

    fun ensureDirectories() {
        listOf(root, versions, libraries, assets, natives, instances).forEach(File::mkdirs)
    }
}
