package com.tinyyana.awesomeArmorStandEditor.store

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tinyyana.awesomeArmorStandEditor.model.Anchor
import com.tinyyana.awesomeArmorStandEditor.model.Animation
import com.tinyyana.awesomeArmorStandEditor.model.ArmorStandElement
import com.tinyyana.awesomeArmorStandEditor.model.ArmorStandFlags
import com.tinyyana.awesomeArmorStandEditor.model.DisplayElement
import com.tinyyana.awesomeArmorStandEditor.model.DisplayKind
import com.tinyyana.awesomeArmorStandEditor.model.Element
import com.tinyyana.awesomeArmorStandEditor.model.Equipment
import com.tinyyana.awesomeArmorStandEditor.model.EulerXYZ
import com.tinyyana.awesomeArmorStandEditor.model.Keyframe
import com.tinyyana.awesomeArmorStandEditor.model.ParticleEmitter
import com.tinyyana.awesomeArmorStandEditor.model.Pose6
import com.tinyyana.awesomeArmorStandEditor.model.Quat
import com.tinyyana.awesomeArmorStandEditor.model.Scene
import com.tinyyana.awesomeArmorStandEditor.model.Track
import com.tinyyana.awesomeArmorStandEditor.model.Transform
import com.tinyyana.awesomeArmorStandEditor.model.Vec3

/**
 * Explicit Scene <-> JSON codec. We build the JSON tree by hand (not reflective Gson mapping) so
 * schema evolution and the polymorphic [Element] `type` discriminator are under our control, and so
 * Gson never uses Unsafe to bypass Kotlin constructors (which would leave non-null fields null).
 * Gson is used only as the JSON tree + IO layer.
 */
object SceneCodec {

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun toJson(scene: Scene): String = gson.toJson(sceneToObject(scene))

    fun fromJson(text: String): Scene = objectToScene(JsonParser.parseString(text).asJsonObject)

    // --- Scene ---

    private fun sceneToObject(scene: Scene): JsonObject = JsonObject().apply {
        addProperty("schemaVersion", scene.schemaVersion)
        addProperty("id", scene.id)
        addProperty("owner", scene.owner)
        addProperty("name", scene.name)
        scene.lastAnchor?.let { add("lastAnchor", anchorToObject(it)) }
        val arr = JsonArray()
        scene.elements.forEach { arr.add(elementToObject(it)) }
        add("elements", arr)
        if (scene.emitters.isNotEmpty()) {
            val em = JsonArray()
            scene.emitters.forEach { em.add(emitterToObject(it)) }
            add("emitters", em)
        }
        scene.animation?.let { add("animation", animationToObject(it)) }
    }

    private fun objectToScene(o: JsonObject): Scene {
        val elements = o.getAsJsonArray("elements")?.map { objectToElement(it.asJsonObject) }?.toMutableList()
            ?: mutableListOf()
        val emitters = o.getAsJsonArray("emitters")?.map { objectToEmitter(it.asJsonObject) }?.toMutableList()
            ?: mutableListOf()
        return Scene(
            schemaVersion = o.get("schemaVersion")?.asInt ?: Scene.SCHEMA_VERSION,
            id = o.get("id").asString,
            owner = o.get("owner").asString,
            name = o.get("name").asString,
            lastAnchor = o.getAsJsonObject("lastAnchor")?.let { objectToAnchor(it) },
            elements = elements,
            emitters = emitters,
            animation = o.getAsJsonObject("animation")?.let { objectToAnimation(it) },
        )
    }

    // --- effects ---

    private fun emitterToObject(e: ParticleEmitter) = JsonObject().apply {
        addProperty("id", e.id); addProperty("particle", e.particle)
        add("offset", vec3(e.offset)); addProperty("count", e.count)
        add("spread", vec3(e.spread)); addProperty("speed", e.speed)
        addProperty("rateTicks", e.rateTicks); addProperty("dustColor", e.dustColor)
    }

    private fun objectToEmitter(o: JsonObject) = ParticleEmitter(
        id = o.get("id").asInt,
        particle = o.get("particle")?.asString ?: "HAPPY_VILLAGER",
        offset = o.getAsJsonArray("offset")?.let { vec3(it) } ?: Vec3.ZERO,
        count = o.get("count")?.asInt ?: 5,
        spread = o.getAsJsonArray("spread")?.let { vec3(it) } ?: Vec3(0.2, 0.2, 0.2),
        speed = o.get("speed")?.asDouble ?: 0.0,
        rateTicks = o.get("rateTicks")?.asInt ?: 10,
        dustColor = o.get("dustColor")?.asInt ?: 0xFFB7D5,
    )

    private fun animationToObject(a: Animation) = JsonObject().apply {
        addProperty("lengthTicks", a.lengthTicks)
        addProperty("loop", a.loop)
        val tracks = JsonArray()
        a.tracks.forEach { t ->
            val to = JsonObject()
            to.addProperty("elementLocalId", t.elementLocalId)
            val kfs = JsonArray()
            t.keyframes.forEach { kfs.add(keyframeToObject(it)) }
            to.add("keyframes", kfs)
            tracks.add(to)
        }
        add("tracks", tracks)
    }

    private fun objectToAnimation(o: JsonObject): Animation {
        val tracks = o.getAsJsonArray("tracks")?.map { el ->
            val to = el.asJsonObject
            Track(
                elementLocalId = to.get("elementLocalId").asInt,
                keyframes = to.getAsJsonArray("keyframes")?.map { objectToKeyframe(it.asJsonObject) }?.toMutableList()
                    ?: mutableListOf(),
            )
        }?.toMutableList() ?: mutableListOf()
        return Animation(
            lengthTicks = o.get("lengthTicks")?.asInt ?: 40,
            loop = o.get("loop")?.asBoolean ?: true,
            tracks = tracks,
        )
    }

    private fun keyframeToObject(k: Keyframe) = JsonObject().apply {
        addProperty("tick", k.tick)
        k.pose?.let { add("pose", poseToObject(it)) }
        k.transform?.let { add("transform", transformToObject(it)) }
        k.offset?.let { add("offset", vec3(it)) }
    }

    private fun objectToKeyframe(o: JsonObject) = Keyframe(
        tick = o.get("tick").asInt,
        pose = o.getAsJsonObject("pose")?.let { objectToPose(it) },
        transform = o.getAsJsonObject("transform")?.let { objectToTransform(it) },
        offset = o.getAsJsonArray("offset")?.let { vec3(it) },
    )

    private fun anchorToObject(a: Anchor) = JsonObject().apply {
        addProperty("world", a.world); addProperty("x", a.x); addProperty("y", a.y); addProperty("z", a.z)
    }

    private fun objectToAnchor(o: JsonObject) =
        Anchor(o.get("world").asString, o.get("x").asDouble, o.get("y").asDouble, o.get("z").asDouble)

    // --- Element ---

    private fun elementToObject(el: Element): JsonObject {
        val o = JsonObject()
        o.addProperty("localId", el.localId)
        o.add("offset", vec3(el.offset))
        o.addProperty("yaw", el.yaw)
        when (el) {
            is ArmorStandElement -> {
                o.addProperty("type", "armor_stand")
                o.add("pose", poseToObject(el.pose))
                o.add("equipment", equipmentToObject(el.equipment))
                o.add("flags", flagsToObject(el.flags))
                el.customName?.let { o.addProperty("customName", it) }
            }
            is DisplayElement -> {
                o.addProperty("type", "display")
                o.addProperty("kind", el.kind.name)
                o.add("transform", transformToObject(el.transform))
                o.addProperty("payload", el.payload)
                o.addProperty("billboard", el.billboard)
                el.brightnessBlock?.let { o.addProperty("brightnessBlock", it) }
                el.brightnessSky?.let { o.addProperty("brightnessSky", it) }
                el.glowColor?.let { o.addProperty("glowColor", it) }
                o.addProperty("viewRange", el.viewRange)
            }
        }
        return o
    }

    private fun objectToElement(o: JsonObject): Element {
        val localId = o.get("localId").asInt
        val offset = vec3(o.getAsJsonArray("offset"))
        val yaw = o.get("yaw")?.asFloat ?: 0f
        return when (o.get("type").asString) {
            "armor_stand" -> ArmorStandElement(
                localId = localId, offset = offset, yaw = yaw,
                pose = o.getAsJsonObject("pose")?.let { objectToPose(it) } ?: Pose6(),
                equipment = o.getAsJsonObject("equipment")?.let { objectToEquipment(it) } ?: Equipment(),
                flags = o.getAsJsonObject("flags")?.let { objectToFlags(it) } ?: ArmorStandFlags(),
                customName = o.get("customName")?.asString,
            )
            "display" -> DisplayElement(
                localId = localId, offset = offset, yaw = yaw,
                kind = DisplayKind.valueOf(o.get("kind").asString),
                transform = o.getAsJsonObject("transform")?.let { objectToTransform(it) } ?: Transform.IDENTITY,
                payload = o.get("payload")?.asString ?: "",
                billboard = o.get("billboard")?.asString ?: "FIXED",
                brightnessBlock = o.get("brightnessBlock")?.asInt,
                brightnessSky = o.get("brightnessSky")?.asInt,
                glowColor = o.get("glowColor")?.asInt,
                viewRange = o.get("viewRange")?.asFloat ?: 1.0f,
            )
            else -> error("Unknown element type: ${o.get("type")}")
        }
    }

    // --- value helpers ---

    private fun vec3(v: Vec3) = JsonArray().apply { add(v.x); add(v.y); add(v.z) }
    private fun vec3(a: JsonArray) = Vec3(a[0].asDouble, a[1].asDouble, a[2].asDouble)

    private fun euler(e: EulerXYZ) = JsonArray().apply { add(e.x); add(e.y); add(e.z) }
    private fun euler(a: JsonArray) = EulerXYZ(a[0].asDouble, a[1].asDouble, a[2].asDouble)

    private fun quat(q: Quat) = JsonArray().apply { add(q.x); add(q.y); add(q.z); add(q.w) }
    private fun quat(a: JsonArray) = Quat(a[0].asDouble, a[1].asDouble, a[2].asDouble, a[3].asDouble)

    private fun poseToObject(p: Pose6) = JsonObject().apply {
        add("head", euler(p.head)); add("body", euler(p.body))
        add("leftArm", euler(p.leftArm)); add("rightArm", euler(p.rightArm))
        add("leftLeg", euler(p.leftLeg)); add("rightLeg", euler(p.rightLeg))
    }

    private fun objectToPose(o: JsonObject) = Pose6(
        head = euler(o.getAsJsonArray("head")), body = euler(o.getAsJsonArray("body")),
        leftArm = euler(o.getAsJsonArray("leftArm")), rightArm = euler(o.getAsJsonArray("rightArm")),
        leftLeg = euler(o.getAsJsonArray("leftLeg")), rightLeg = euler(o.getAsJsonArray("rightLeg")),
    )

    private fun transformToObject(t: Transform) = JsonObject().apply {
        add("translation", vec3(t.translation)); add("leftRotation", quat(t.leftRotation))
        add("scale", vec3(t.scale)); add("rightRotation", quat(t.rightRotation))
    }

    private fun objectToTransform(o: JsonObject) = Transform(
        translation = vec3(o.getAsJsonArray("translation")), leftRotation = quat(o.getAsJsonArray("leftRotation")),
        scale = vec3(o.getAsJsonArray("scale")), rightRotation = quat(o.getAsJsonArray("rightRotation")),
    )

    private fun equipmentToObject(e: Equipment) = JsonObject().apply {
        e.head?.let { addProperty("head", it) }; e.chest?.let { addProperty("chest", it) }
        e.legs?.let { addProperty("legs", it) }; e.feet?.let { addProperty("feet", it) }
        e.mainHand?.let { addProperty("mainHand", it) }; e.offHand?.let { addProperty("offHand", it) }
    }

    private fun objectToEquipment(o: JsonObject) = Equipment(
        head = o.get("head")?.asString, chest = o.get("chest")?.asString,
        legs = o.get("legs")?.asString, feet = o.get("feet")?.asString,
        mainHand = o.get("mainHand")?.asString, offHand = o.get("offHand")?.asString,
    )

    private fun flagsToObject(f: ArmorStandFlags) = JsonObject().apply {
        addProperty("small", f.small); addProperty("invisible", f.invisible)
        addProperty("noBasePlate", f.noBasePlate); addProperty("noGravity", f.noGravity)
        addProperty("arms", f.arms); addProperty("marker", f.marker); addProperty("glowing", f.glowing)
    }

    private fun objectToFlags(o: JsonObject) = ArmorStandFlags(
        small = o.get("small")?.asBoolean ?: false, invisible = o.get("invisible")?.asBoolean ?: false,
        noBasePlate = o.get("noBasePlate")?.asBoolean ?: false, noGravity = o.get("noGravity")?.asBoolean ?: true,
        arms = o.get("arms")?.asBoolean ?: true, marker = o.get("marker")?.asBoolean ?: false,
        glowing = o.get("glowing")?.asBoolean ?: false,
    )
}
