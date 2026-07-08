package com.tinyyana.awesomeArmorStandEditor.edit

import com.tinyyana.awesomeArmorStandEditor.model.Quat
import com.tinyyana.awesomeArmorStandEditor.model.Transform
import com.tinyyana.awesomeArmorStandEditor.model.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure quaternion math (no JOML dependency, so it's unit-testable off-server). */
object QuatMath {
    fun fromAxisAngle(axis: Axis, angleRad: Double): Quat {
        val h = angleRad / 2.0
        val s = sin(h)
        return when (axis) {
            Axis.X -> Quat(s, 0.0, 0.0, cos(h))
            Axis.Y -> Quat(0.0, s, 0.0, cos(h))
            Axis.Z -> Quat(0.0, 0.0, s, cos(h))
        }
    }

    /** Hamilton product a*b. */
    fun mul(a: Quat, b: Quat): Quat = Quat(
        a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y,
        a.w * b.y - a.x * b.z + a.y * b.w + a.z * b.x,
        a.w * b.z + a.x * b.y - a.y * b.x + a.z * b.w,
        a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z,
    )

    fun normalize(q: Quat): Quat {
        val n = sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w)
        if (n == 0.0) return Quat.IDENTITY
        return Quat(q.x / n, q.y / n, q.z / n, q.w / n)
    }
}

/** Pure Display-entity transform edits. */
object TransformOps {

    fun translate(t: Transform, axis: Axis, delta: Double): Transform {
        val v = t.translation
        val nv = when (axis) {
            Axis.X -> v.copy(x = v.x + delta)
            Axis.Y -> v.copy(y = v.y + delta)
            Axis.Z -> v.copy(z = v.z + delta)
        }
        return t.copy(translation = nv)
    }

    /** Add [delta] to one scale axis, clamped to a small positive minimum. */
    fun scaleAxis(t: Transform, axis: Axis, delta: Double, min: Double = 0.05): Transform {
        val v = t.scale
        val nv = when (axis) {
            Axis.X -> v.copy(x = maxOf(min, v.x + delta))
            Axis.Y -> v.copy(y = maxOf(min, v.y + delta))
            Axis.Z -> v.copy(z = maxOf(min, v.z + delta))
        }
        return t.copy(scale = nv)
    }

    fun setScaleUniform(t: Transform, value: Double, min: Double = 0.05): Transform =
        t.copy(scale = Vec3(maxOf(min, value), maxOf(min, value), maxOf(min, value)))

    /** Rotate the left-rotation quaternion by [angleDeg] about [axis]. */
    fun rotate(t: Transform, axis: Axis, angleDeg: Double): Transform {
        val delta = QuatMath.fromAxisAngle(axis, Math.toRadians(angleDeg))
        return t.copy(leftRotation = QuatMath.normalize(QuatMath.mul(delta, t.leftRotation)))
    }
}
