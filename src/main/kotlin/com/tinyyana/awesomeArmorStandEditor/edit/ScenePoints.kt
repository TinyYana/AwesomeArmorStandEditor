package com.tinyyana.awesomeArmorStandEditor.edit

import com.tinyyana.awesomeArmorStandEditor.model.Scene
import com.tinyyana.awesomeArmorStandEditor.model.Vec3

/**
 * Pure geometry for the region guard: every offset from a scene's origin where the scene will put
 * an entity. Elements and particle emitters spawn at theirs; animation keyframes teleport elements
 * to theirs during playback. An offset missing here is an offset the guard never probes, which is
 * how a scene reaches into someone else's claim.
 */
object ScenePoints {

    fun offsets(scene: Scene): List<Vec3> =
        listOf(Vec3.ZERO) +                                  // the origin itself
            scene.elements.map { it.offset } +
            scene.emitters.map { it.offset } +
            scene.animation?.tracks.orEmpty().flatMap { track -> track.keyframes.mapNotNull { it.offset } }
}
