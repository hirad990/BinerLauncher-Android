package ir.binercraft.binerlauncher.minecraft

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object LibraryResolver {
    fun resolve(root: JsonObject): List<VersionArtifact> {
        return root["libraries"]?.jsonArray.orEmpty().flatMap { element ->
            val library = element.jsonObject
            val downloads = library["downloads"]?.jsonObject ?: return@flatMap emptyList()
            val artifact = downloads["artifact"]?.jsonObject ?: return@flatMap emptyList()
            val path = artifact["path"]?.jsonPrimitive?.content ?: return@flatMap emptyList()
            val url = artifact["url"]?.jsonPrimitive?.content ?: return@flatMap emptyList()
            listOf(
                VersionArtifact(
                    path = path,
                    url = url,
                    sha1 = artifact["sha1"]?.jsonPrimitive?.content.orEmpty(),
                    size = artifact["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    required = true
                )
            )
        }
    }
}
