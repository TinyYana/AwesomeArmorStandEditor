package com.tinyyana.awesomeArmorStandEditor.edit

import com.tinyyana.awesomeArmorStandEditor.model.EulerXYZ
import com.tinyyana.awesomeArmorStandEditor.model.Keyframe
import com.tinyyana.awesomeArmorStandEditor.model.Pose6
import com.tinyyana.awesomeArmorStandEditor.model.Quat
import com.tinyyana.awesomeArmorStandEditor.model.Transform
import com.tinyyana.awesomeArmorStandEditor.model.Vec3
import kotlin.math.sqrt

/** Pure keyframe interpolation. Server-independent, unit-tested. Linear positions/angles, nlerp quats. */
object InterpolationOps {

    fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t

    fun lerp(a: Vec3, b: Vec3, t: Double) = Vec3(lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.z, b.z, t))

    fun lerp(a: EulerXYZ, b: EulerXYZ, t: Double) = EulerXYZ(lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.z, b.z, t))

    fun lerp(a: Pose6, b: Pose6, t: Double) = Pose6(
        lerp(a.head, b.head, t), lerp(a.body, b.body, t), lerp(a.leftArm, b.leftArm, t),
        lerp(a.rightArm, b.rightArm, t), lerp(a.leftLeg, b.leftLeg, t), lerp(a.rightLeg, b.rightLeg, t),
    )

    /** Normalized linear quaternion interpolation with shortest-path sign fix. */
    fun nlerp(a: Quat, b: Quat, t: Double): Quat {
        val dot = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w
        val s = if (dot < 0) -1.0 else 1.0
        val x = lerp(a.x, b.x * s, t); val y = lerp(a.y, b.y * s, t)
        val z = lerp(a.z, b.z * s, t); val w = lerp(a.w, b.w * s, t)
        val n = sqrt(x * x + y * y + z * z + w * w)
        return if (n == 0.0) Quat.IDENTITY else Quat(x / n, y / n, z / n, w / n)
    }

    fun lerp(a: Transform, b: Transform, t: Double) = Transform(
        translation = lerp(a.translation, b.translation, t),
        leftRotation = nlerp(a.leftRotation, b.leftRotation, t),
        scale = lerp(a.scale, b.scale, t),
        rightRotation = nlerp(a.rightRotation, b.rightRotation, t),
    )

    /**
     * Sample a (tick-sorted) keyframe list at [tick]. Fields present on both surrounding keyframes
     * are interpolated; a field present on only one is stepped. Clamps outside the range.
     */
    fun sample(sorted: List<Keyframe>, tick: Int): Keyframe {
        if (sorted.isEmpty()) return Keyframe(tick)
        if (tick <= sorted.first().tick) return sorted.first().copy(tick = tick)
        if (tick >= sorted.last().tick) return sorted.last().copy(tick = tick)
        var lo = sorted.first()
        var hi = sorted.last()
        for (i in 0 until sorted.size - 1) {
            if (sorted[i].tick <= tick && tick <= sorted[i + 1].tick) {
                lo = sorted[i]; hi = sorted[i + 1]; break
            }
        }
        val span = (hi.tick - lo.tick).toDouble()
        val f = if (span <= 0.0) 0.0 else (tick - lo.tick) / span
        return Keyframe(
            tick = tick,
            pose = when {
                lo.pose != null && hi.pose != null -> lerp(lo.pose, hi.pose, f)
                else -> lo.pose ?: hi.pose
            },
            transform = when {
                lo.transform != null && hi.transform != null -> lerp(lo.transform, hi.transform, f)
                else -> lo.transform ?: hi.transform
            },
            offset = when {
                lo.offset != null && hi.offset != null -> lerp(lo.offset, hi.offset, f)
                else -> lo.offset ?: hi.offset
            },
        )
    }
}
