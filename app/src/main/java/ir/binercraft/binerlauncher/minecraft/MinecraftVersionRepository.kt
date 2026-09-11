package ir.binercraft.binerlauncher.minecraft

import ir.binercraft.binerlauncher.core.MinecraftVersion
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MinecraftVersionRepository {
    companion object {
        const val VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        private const val MIN_MINOR = 7
        private const val MIN_PATCH = 10
        private const val MAX_MAJOR = 26
        private const val MAX_MINOR = 2
    }

    fun fetchVersions(): List<MinecraftVersion> {
        val connection = (URL(VERSION_MANIFEST_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) error("Minecraft manifest HTTP ${connection.responseCode}")
            val versions = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getJSONArray("versions")
            buildList(versions.length()) {
                for (index in 0 until versions.length()) {
                    val item = versions.getJSONObject(index)
                    val type = item.optString("type", "unknown")
                    val id = item.getString("id")
                    if (type != "release" || !isSupportedRelease(id)) continue
                    add(MinecraftVersion(id, type, item.optString("releaseTime"), item.optString("url").ifBlank { null }, item.optString("sha1").ifBlank { null }))
                }
            }
        } finally { connection.disconnect() }
    }

    private fun isSupportedRelease(id: String): Boolean {
        val p = id.split('.').mapNotNull { it.toIntOrNull() }
        if (p.size < 2) return false
        val major = p[0]
        val minor = p[1]
        val patch = p.getOrElse(2) { 0 }
        if (major == 1) {
            if (minor < MIN_MINOR) return false
            if (minor == MIN_MINOR && patch < MIN_PATCH) return false
            return true
        }
        return major in 2 until MAX_MAJOR || (major == MAX_MAJOR && minor <= MAX_MINOR)
    }
}
