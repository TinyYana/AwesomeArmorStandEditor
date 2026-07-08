package com.tinyyana.awesomeArmorStandEditor.model

/**
 * Pure-data geometry types. No Bukkit/JOML imports here so this layer stays unit-testable
 * without a server. Conversion to Bukkit/JOML lives in the placement/store layers.
 */

/** A 3D vector (position, translation, or scale). */
data class Vec3(val x: Double, val y: Double, val z: Double) {
    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)
        val ONE = Vec3(1.0, 1.0, 1.0)
    }
}

/** A quaternion (Display-entity rotation). Identity = (0,0,0,1). */
data class Quat(val x: Double, val y: Double, val z: Double, val w: Double) {
    companion object {
        val IDENTITY = Quat(0.0, 0.0, 0.0, 1.0)
    }
}

/** Euler angles in **radians** for one armor-stand body part (maps 1:1 to org.bukkit.util.EulerAngle). */
data class EulerXYZ(val x: Double, val y: Double, val z: Double) {
    companion object {
        val ZERO = EulerXYZ(0.0, 0.0, 0.0)
    }
}

/** The six posable parts of an armor stand. */
data class Pose6(
    val head: EulerXYZ = EulerXYZ.ZERO,
    val body: EulerXYZ = EulerXYZ.ZERO,
    val leftArm: EulerXYZ = EulerXYZ.ZERO,
    val rightArm: EulerXYZ = EulerXYZ.ZERO,
    val leftLeg: EulerXYZ = EulerXYZ.ZERO,
    val rightLeg: EulerXYZ = EulerXYZ.ZERO,
)

/** Display-entity transformation (matches org.bukkit.util.Transformation component order). */
data class Transform(
    val translation: Vec3 = Vec3.ZERO,
    val leftRotation: Quat = Quat.IDENTITY,
    val scale: Vec3 = Vec3.ONE,
    val rightRotation: Quat = Quat.IDENTITY,
) {
    companion object {
        val IDENTITY = Transform()
    }
}
