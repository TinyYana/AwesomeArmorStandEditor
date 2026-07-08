package com.tinyyana.awesomeArmorStandEditor.model

/**
 * A saved scene ("blueprint"). Elements store offsets relative to the scene origin, so a scene
 * is portable: placing it supplies a world + origin location at load time.
 *
 * Runtime entities carry only PDC tags (owner/sceneId/localId); the JSON is the source of truth.
 * Animation is intentionally absent in P1 (added at P3 with a schemaVersion bump).
 */
data class Scene(
    val schemaVersion: Int = SCHEMA_VERSION,
    val id: String,
    val owner: String,          // owner UUID as string
    var name: String,
    var lastAnchor: Anchor? = null,   // where it was last authored/placed, for convenience re-placement
    val elements: MutableList<Element> = mutableListOf(),
    val emitters: MutableList<ParticleEmitter> = mutableListOf(),
    var animation: Animation? = null,
) {
    fun nextLocalId(): Int = (elements.maxOfOrNull { it.localId } ?: 0) + 1
    fun nextEmitterId(): Int = (emitters.maxOfOrNull { it.id } ?: 0) + 1

    companion object {
        const val SCHEMA_VERSION = 2
    }
}

/** Last-known placement anchor (world + block-ish origin). Not required for portability. */
data class Anchor(val world: String, val x: Double, val y: Double, val z: Double)

/** An element is one posable node in a scene. localId is unique within its scene. */
sealed interface Element {
    val localId: Int
    val offset: Vec3   // relative to scene origin
    val yaw: Float     // body yaw in degrees
}

data class ArmorStandElement(
    override val localId: Int,
    override var offset: Vec3 = Vec3.ZERO,
    override var yaw: Float = 0f,
    var pose: Pose6 = Pose6(),
    var equipment: Equipment = Equipment(),
    var flags: ArmorStandFlags = ArmorStandFlags(),
    var customName: String? = null,   // MiniMessage
) : Element

enum class DisplayKind { ITEM, BLOCK, TEXT }

data class DisplayElement(
    override val localId: Int,
    override var offset: Vec3 = Vec3.ZERO,
    override var yaw: Float = 0f,
    var kind: DisplayKind = DisplayKind.ITEM,
    var transform: Transform = Transform.IDENTITY,
    /** ITEM: item base64; BLOCK: block-data string; TEXT: MiniMessage string. */
    var payload: String = "",
    var billboard: String = "FIXED",      // Display.Billboard name
    var brightnessBlock: Int? = null,     // 0..15, null = default
    var brightnessSky: Int? = null,
    var glowColor: Int? = null,           // ARGB, null = no override
    var viewRange: Float = 1.0f,
) : Element

/** Equipment slots as opaque item base64 (null = empty). Bukkit (de)serialization lives in the store layer. */
data class Equipment(
    var head: String? = null,
    var chest: String? = null,
    var legs: String? = null,
    var feet: String? = null,
    var mainHand: String? = null,
    var offHand: String? = null,
)

data class ArmorStandFlags(
    var small: Boolean = false,
    var invisible: Boolean = false,
    var noBasePlate: Boolean = false,
    var noGravity: Boolean = true,   // editor default: don't fall
    var arms: Boolean = true,        // editor default: arms visible/posable
    var marker: Boolean = false,
    var glowing: Boolean = false,
)
