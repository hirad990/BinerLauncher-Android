package ir.binercraft.binerlauncher.minecraft

import ir.binercraft.binerlauncher.core.MinecraftVersion
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MinecraftVersionRepository {
    companion object {
        const val VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    }

    fun fetchVersions(): List<MinecraftVersion> {
        val connection = (URL(VERSION_MANIFEST_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
        }

        return try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Minecraft manifest HTTP ${connection.responseCode}")
            }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val versions = JSONObject(json).getJSONArray("versions")
            buildList(versions.length()) {
                for (index in 0 until versions.length()) {
                    val item = versions.getJSONObject(index)
                    add(
                        MinecraftVersion(
                            id = item.getString("id"),
                            type = item.optString("type", "unknown"),
                            releaseTime = item.optString("releaseTime"),
                            url = item.optString("url").ifBlank { null },
                            sha1 = item.optString("sha1").ifBlank { null }
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
