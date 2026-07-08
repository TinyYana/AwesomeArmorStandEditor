package com.tinyyana.awesomeArmorStandEditor

import com.tinyyana.awesomeArmorStandEditor.edit.Axis
import com.tinyyana.awesomeArmorStandEditor.edit.BodyPart
import com.tinyyana.awesomeArmorStandEditor.edit.InterpolationOps
import com.tinyyana.awesomeArmorStandEditor.edit.PoseOps
import com.tinyyana.awesomeArmorStandEditor.edit.QuatMath
import com.tinyyana.awesomeArmorStandEditor.edit.TransformOps
import com.tinyyana.awesomeArmorStandEditor.model.Anchor
import com.tinyyana.awesomeArmorStandEditor.model.Animation
import com.tinyyana.awesomeArmorStandEditor.model.ArmorStandElement
import com.tinyyana.awesomeArmorStandEditor.model.ArmorStandFlags
import com.tinyyana.awesomeArmorStandEditor.model.DisplayElement
import com.tinyyana.awesomeArmorStandEditor.model.DisplayKind
import com.tinyyana.awesomeArmorStandEditor.model.Equipment
import com.tinyyana.awesomeArmorStandEditor.model.EulerXYZ
import com.tinyyana.awesomeArmorStandEditor.model.Keyframe
import com.tinyyana.awesomeArmorStandEditor.model.ParticleEmitter
import com.tinyyana.awesomeArmorStandEditor.model.Pose6
import com.tinyyana.awesomeArmorStandEditor.model.Scene
import com.tinyyana.awesomeArmorStandEditor.model.Track
import com.tinyyana.awesomeArmorStandEditor.model.Transform
import com.tinyyana.awesomeArmorStandEditor.model.Vec3
import com.tinyyana.awesomeArmorStandEditor.export.SummonExporter
import com.tinyyana.awesomeArmorStandEditor.store.SceneCodec
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EditorLogicTest {

    @Test
    fun sceneRoundTripsThroughJson() {
        val scene = Scene(
            id = "abc-123",
            owner = "00000000-0000-0000-0000-000000000001",
            name = "測試場景",
            lastAnchor = Anchor("world", 10.0, 64.0, -20.0),
        )
        scene.elements += ArmorStandElement(
            localId = 1,
            offset = Vec3(0.5, 0.0, -0.5),
            yaw = 45f,
            pose = Pose6(head = EulerXYZ(0.1, -0.2, 0.3)),
            equipment = Equipment(head = "base64helmet", mainHand = "base64sword"),
            flags = ArmorStandFlags(small = true, invisible = false, arms = true),
            customName = "<red>守衛",
        )
        scene.elements += DisplayElement(
            localId = 2,
            offset = Vec3(1.0, 2.0, 3.0),
            kind = DisplayKind.BLOCK,
            transform = Transform(translation = Vec3(0.0, 1.0, 0.0), scale = Vec3(2.0, 2.0, 2.0)),
            payload = "minecraft:stone",
            billboard = "CENTER",
            brightnessBlock = 15,
            viewRange = 2.5f,
        )

        val restored = SceneCodec.fromJson(SceneCodec.toJson(scene))
        assertEquals(scene, restored)
    }

    @Test
    fun nudgeWrapsAndNormalizes() {
        // 170° + 20° -> -170° (wrapped into (-180,180])
        val e = PoseOps.nudge(EulerXYZ(0.0, Math.toRadians(170.0), 0.0), Axis.Y, 20.0)
        assertEquals(-170.0, Math.toDegrees(PoseOps.normalizeRad(e.y)), 1e-9)

        assertTrue(PoseOps.normalizeRad(3 * PI) <= PI && PoseOps.normalizeRad(3 * PI) > -PI)
        // setPart/getPart round-trips
        val pose = PoseOps.setPart(Pose6(), BodyPart.LEFT_ARM, EulerXYZ(1.0, 2.0, 3.0))
        assertEquals(EulerXYZ(1.0, 2.0, 3.0), PoseOps.getPart(pose, BodyPart.LEFT_ARM))
    }

    @Test
    fun emittersAndAnimationRoundTrip() {
        val scene = Scene(id = "s1", owner = "00000000-0000-0000-0000-000000000002", name = "動畫")
        scene.elements += ArmorStandElement(localId = 1)
        scene.emitters += ParticleEmitter(id = 1, particle = "FLAME", offset = Vec3(0.0, 1.0, 0.0), rateTicks = 5)
        scene.animation = Animation(
            lengthTicks = 20, loop = true,
            tracks = mutableListOf(
                Track(
                    elementLocalId = 1,
                    keyframes = mutableListOf(
                        Keyframe(0, pose = Pose6()),
                        Keyframe(20, pose = Pose6(head = EulerXYZ(0.0, 1.0, 0.0))),
                    ),
                ),
            ),
        )
        assertEquals(scene, SceneCodec.fromJson(SceneCodec.toJson(scene)))
    }

    @Test
    fun interpolationSamplesMidpoint() {
        val kfs = listOf(
            Keyframe(0, pose = Pose6(head = EulerXYZ(0.0, 0.0, 0.0))),
            Keyframe(10, pose = Pose6(head = EulerXYZ(0.0, 2.0, 0.0))),
        )
        val mid = InterpolationOps.sample(kfs, 5)
        assertNotNull(mid.pose)
        assertEquals(1.0, mid.pose!!.head.y, 1e-9)  // halfway between 0 and 2
        // clamps outside range
        assertEquals(2.0, InterpolationOps.sample(kfs, 99).pose!!.head.y, 1e-9)
    }

    @Test
    fun summonExporterFormat() {
        val scene = Scene(id = "e", owner = "00000000-0000-0000-0000-000000000003", name = "x")
        scene.elements += ArmorStandElement(
            localId = 1, offset = Vec3(1.5, 0.0, -2.0), yaw = 45f,
            pose = Pose6(head = EulerXYZ(0.1, 0.0, 0.0)), customName = "<red>守衛",
        )
        scene.elements += DisplayElement(localId = 2, kind = DisplayKind.TEXT, payload = "哈囉")
        val out = SummonExporter.export(scene)

        // Text components must be SNBT, never the legacy JSON-string form (stored literally in 26.2).
        assertTrue(out.contains("CustomName:\"守衛\""), out)
        assertTrue(out.contains("text:\"哈囉\""), out)
        assertFalse(out.contains("{\"text\":"), out)

        // Coordinates must NOT carry an 'f' suffix (~0f is a parse error); NBT floats still do.
        assertTrue(out.contains("~1.5 ~0 ~-2"), out)
        assertFalse(Regex("~-?[0-9.]+f").containsMatchIn(out), "coordinate has an 'f' suffix: $out")

        // Capture the real output so it can be run against a live server (build/ is gitignored).
        java.io.File("build/export-sample.txt").writeText(out)
    }

    @Test
    fun quatAndTransformOps() {
        // fromAxisAngle produces a unit quaternion
        val q = QuatMath.fromAxisAngle(Axis.Y, Math.toRadians(90.0))
        val mag = q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w
        assertEquals(1.0, mag, 1e-9)

        // identity multiply
        assertEquals(q, QuatMath.mul(q, com.tinyyana.awesomeArmorStandEditor.model.Quat.IDENTITY))

        val t = TransformOps.translate(Transform.IDENTITY, Axis.X, 1.5)
        assertEquals(1.5, t.translation.x, 1e-9)

        // scale clamps to the positive minimum
        val s = TransformOps.scaleAxis(Transform.IDENTITY, Axis.X, -100.0)
        assertTrue(s.scale.x >= 0.05)
    }
}
