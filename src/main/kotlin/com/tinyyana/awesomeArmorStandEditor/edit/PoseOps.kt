package com.tinyyana.awesomeArmorStandEditor.edit

import com.tinyyana.awesomeArmorStandEditor.model.EulerXYZ
import com.tinyyana.awesomeArmorStandEditor.model.Pose6

enum class Axis { X, Y, Z }

/** Display names live in the lang files under `part.<lowercase name>`. */
enum class BodyPart { HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }

/** Pure armor-stand pose math (degrees at the UI, radians in the model). Server-independent, unit-tested. */
object PoseOps {

    fun getPart(pose: Pose6, part: BodyPart): EulerXYZ = when (part) {
        BodyPart.HEAD -> pose.head
        BodyPart.BODY -> pose.body
        BodyPart.LEFT_ARM -> pose.leftArm
        BodyPart.RIGHT_ARM -> pose.rightArm
        BodyPart.LEFT_LEG -> pose.leftLeg
        BodyPart.RIGHT_LEG -> pose.rightLeg
    }

    fun setPart(pose: Pose6, part: BodyPart, e: EulerXYZ): Pose6 = when (part) {
        BodyPart.HEAD -> pose.copy(head = e)
        BodyPart.BODY -> pose.copy(body = e)
        BodyPart.LEFT_ARM -> pose.copy(leftArm = e)
        BodyPart.RIGHT_ARM -> pose.copy(rightArm = e)
        BodyPart.LEFT_LEG -> pose.copy(leftLeg = e)
        BodyPart.RIGHT_LEG -> pose.copy(rightLeg = e)
    }

    /** Add [deltaDeg] degrees to one axis, wrapping into (-180°, 180°]. */
    fun nudge(e: EulerXYZ, axis: Axis, deltaDeg: Double): EulerXYZ {
        val d = Math.toRadians(deltaDeg)
        return when (axis) {
            Axis.X -> e.copy(x = normalizeRad(e.x + d))
            Axis.Y -> e.copy(y = normalizeRad(e.y + d))
            Axis.Z -> e.copy(z = normalizeRad(e.z + d))
        }
    }

    fun axisValueDeg(e: EulerXYZ, axis: Axis): Double = Math.toDegrees(
        when (axis) { Axis.X -> e.x; Axis.Y -> e.y; Axis.Z -> e.z }
    )

    /** Wrap an angle in radians into (-PI, PI]. */
    fun normalizeRad(a: Double): Double {
        var r = a % (2 * Math.PI)
        if (r <= -Math.PI) r += 2 * Math.PI
        if (r > Math.PI) r -= 2 * Math.PI
        return r
    }
}
