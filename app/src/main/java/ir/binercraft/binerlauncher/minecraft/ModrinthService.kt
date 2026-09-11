package ir.binercraft.binerlauncher.minecraft

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class ModrinthService(private val paths: MinecraftPaths) {
    data class ModResult(val projectId: String, val slug: String, val title: String, val description: String)

    fun search(query: String, gameVersion: String, loader: String = "fabric"): List<ModResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val facets = "[[\"project_type:mod\"],[\"versions:$gameVersion\"],[\"categories:$loader\"]]"
        val url = "https://api.modrinth.com/v2/search?query=$encoded&facets=${URLEncoder.encode(facets, "UTF-8")}&limit=20"
        val json = get(url)
        val hits = JSONObject(json).optJSONArray("hits") ?: JSONArray()
        return buildList(hits.length()) {
            for (i in 0 until hits.length()) {
                val item = hits.getJSONObject(i)
                add(ModResult(item.optString("project_id"), item.optString("slug"), item.optString("title"), item.optString("description")))
            }
        }
    }

    fun installLatest(projectId: String, gameVersion: String, loader: String): File {
        val versions = JSONArray(get("https://api.modrinth.com/v2/project/$projectId/version"))
        for (i in 0 until versions.length()) {
            val version = versions.getJSONObject(i)
            val games = version.optJSONArray("game_versions") ?: continue
            val loaders = version.optJSONArray("loaders") ?: continue
            if (!contains(games, gameVersion) || !contains(loaders, loader)) continue
            val files = version.optJSONArray("files") ?: continue
            if (files.length() == 0) continue
            val file = files.getJSONObject(0)
            val name = file.optString("filename")
            val targetDir = paths.modsDirectory(gameVersion).apply { mkdirs() }
            val target = File(targetDir, name)
            download(file.getString("url"), target)
            return target
        }
        error("No compatible Modrinth version found for $gameVersion / $loader")
    }

    private fun contains(array: JSONArray, value: String): Boolean = (0 until array.length()).any { array.optString(it) == value }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", "BinerLauncher/0.2 (+https://binercraft.ir)")
        return try { if (c.responseCode !in 200..299) error("Modrinth HTTP ${c.responseCode}"); c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    private fun download(url: String, target: File) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 60_000
        try { if (c.responseCode !in 200..299) error("Mod download HTTP ${c.responseCode}"); c.inputStream.use { input -> target.outputStream().use { output -> input.copyTo(output, 64 * 1024) } } } finally { c.disconnect() }
    }
}
