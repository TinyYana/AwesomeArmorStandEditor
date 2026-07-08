package com.tinyyana.awesomeArmorStandEditor.model

/**
 * A particle emitter attached to a scene. Realised at runtime as an invisible marker entity so it
 * persists with the placed art and is ticked only when its chunk is loaded and a player is near.
 */
data class ParticleEmitter(
    val id: Int,
    var particle: String = "HAPPY_VILLAGER",   // org.bukkit.Particle name
    var offset: Vec3 = Vec3.ZERO,               // relative to scene origin
    var count: Int = 5,
    var spread: Vec3 = Vec3(0.2, 0.2, 0.2),
    var speed: Double = 0.0,
    var rateTicks: Int = 10,                    // emit every N ticks
    var dustColor: Int = 0xFFB7D5,              // used only for DUST-type particles (RGB)
)

/** One keyframe: the target state of a tracked element at [tick]. Only the relevant field is set. */
data class Keyframe(
    val tick: Int,
    val pose: Pose6? = null,        // for armor-stand tracks
    val transform: Transform? = null, // for display tracks
    val offset: Vec3? = null,        // element world-offset (both types)
)

/** An animation track bound to one element (by localId). */
data class Track(
    val elementLocalId: Int,
    val keyframes: MutableList<Keyframe> = mutableListOf(),
) {
    fun sorted(): List<Keyframe> = keyframes.sortedBy { it.tick }
}

/** A keyframe animation over a scene's elements. */
data class Animation(
    var lengthTicks: Int = 40,
    var loop: Boolean = true,
    val tracks: MutableList<Track> = mutableListOf(),
) {
    fun track(elementLocalId: Int): Track =
        tracks.find { it.elementLocalId == elementLocalId } ?: Track(elementLocalId).also { tracks += it }
}
