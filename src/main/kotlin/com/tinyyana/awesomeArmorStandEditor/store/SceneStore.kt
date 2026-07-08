package com.tinyyana.awesomeArmorStandEditor.store

import com.tinyyana.awesomeArmorStandEditor.model.Scene
import java.io.File
import java.util.UUID

/**
 * Flat JSON persistence: plugins/AwesomeArmorStandEditor/scenes/<owner>/<sceneId>.json.
 * Portable and shareable — a scene file is human-readable JSON. No database.
 */
class SceneStore(private val root: File) {

    init {
        root.mkdirs()
    }

    private fun ownerDir(owner: UUID) = File(root, owner.toString())
    private fun file(owner: UUID, sceneId: String) = File(ownerDir(owner), "$sceneId.json")

    fun save(scene: Scene) {
        val f = file(UUID.fromString(scene.owner), scene.id)
        f.parentFile.mkdirs()
        f.writeText(SceneCodec.toJson(scene), Charsets.UTF_8)
    }

    fun load(owner: UUID, sceneId: String): Scene? {
        val f = file(owner, sceneId)
        if (!f.isFile) return null
        return runCatching { SceneCodec.fromJson(f.readText(Charsets.UTF_8)) }.getOrNull()
    }

    /** Small per-owner directory; loading all to find/list is fine (not a hot path, not a world scan). */
    fun list(owner: UUID): List<Scene> {
        val dir = ownerDir(owner)
        val files = dir.listFiles { f -> f.isFile && f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { runCatching { SceneCodec.fromJson(it.readText(Charsets.UTF_8)) }.getOrNull() }
            .sortedBy { it.name.lowercase() }
    }

    fun loadByName(owner: UUID, name: String): Scene? =
        list(owner).firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun delete(owner: UUID, sceneId: String): Boolean = file(owner, sceneId).delete()

    fun exportFile(owner: UUID, sceneId: String): File = file(owner, sceneId)
}
