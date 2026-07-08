package com.tinyyana.awesomeArmorStandEditor.export

import com.tinyyana.awesomeArmorStandEditor.model.ArmorStandElement
import com.tinyyana.awesomeArmorStandEditor.model.DisplayElement
import com.tinyyana.awesomeArmorStandEditor.model.DisplayKind
import com.tinyyana.awesomeArmorStandEditor.model.Element
import com.tinyyana.awesomeArmorStandEditor.model.EulerXYZ
import com.tinyyana.awesomeArmorStandEditor.model.Scene
import com.tinyyana.awesomeArmorStandEditor.store.ItemCodec
import java.util.Locale

/**
 * Best-effort /summon export. Coordinates are relative (~dx ~dy ~dz) so the output is placed
 * relative to whoever runs the commands.
 *
 * NBT caveat: armor-stand Pose/flags and display transformation have been stable vanilla NBT for
 * years; item/block/text payloads are emitted by id (enchants/custom item data are NOT carried).
 * If a future MC changes this NBT shape, this exporter is the single isolated place to fix.
 */
object SummonExporter {

    /** [tagPrefix] (optional) adds `Tags:["<prefix><localId>"]` so frames can target each entity. */
    fun export(scene: Scene, tagPrefix: String? = null): String =
        scene.elements.joinToString("\n") { line(it, tagPrefix) }

    private fun line(el: Element, tagPrefix: String?): String {
        val (dx, dy, dz) = Triple(f(el.offset.x), f(el.offset.y), f(el.offset.z))
        val tag = tagPrefix?.let { "$it${el.localId}" }
        return when (el) {
            is ArmorStandElement -> "/summon minecraft:armor_stand ~$dx ~$dy ~$dz ${armorStandNbt(el, tag)}"
            is DisplayElement -> "/summon minecraft:${displayType(el.kind)} ~$dx ~$dy ~$dz ${displayNbt(el, tag)}"
        }
    }

    private fun displayType(kind: DisplayKind) = when (kind) {
        DisplayKind.ITEM -> "item_display"
        DisplayKind.BLOCK -> "block_display"
        DisplayKind.TEXT -> "text_display"
    }

    private fun armorStandNbt(el: ArmorStandElement, tag: String?): String {
        val f = el.flags
        val tags = mutableListOf(
            "Small:${b(f.small)}",
            "NoBasePlate:${b(f.noBasePlate)}",
            "ShowArms:${b(f.arms)}",
            "Marker:${b(f.marker)}",
            "Invisible:${b(f.invisible)}",
            "NoGravity:${b(f.noGravity)}",
            "Glowing:${b(f.glowing)}",
            "Rotation:[${f(el.yaw.toDouble())},0f]",
            "Pose:${poseNbt(el.pose)}",
        )
        armorItemsNbt(el)?.let { tags += it }
        el.customName?.takeIf { it.isNotBlank() }?.let { tags += "CustomName:'${jsonText(plain(it))}'" }
        tag?.let { tags += "Tags:[\"$it\"]" }
        return "{${tags.joinToString(",")}}"
    }

    internal fun poseNbt(p: com.tinyyana.awesomeArmorStandEditor.model.Pose6): String {
        // Pose NBT is in DEGREES.
        fun part(e: EulerXYZ) = "[${deg(e.x)},${deg(e.y)},${deg(e.z)}]"
        return "{Head:${part(p.head)},Body:${part(p.body)},LeftArm:${part(p.leftArm)}," +
            "RightArm:${part(p.rightArm)},LeftLeg:${part(p.leftLeg)},RightLeg:${part(p.rightLeg)}}"
    }

    private fun armorItemsNbt(el: ArmorStandElement): String? {
        val e = el.equipment
        if (listOf(e.head, e.chest, e.legs, e.feet, e.mainHand, e.offHand).all { it == null }) return null
        // ArmorItems order: feet, legs, chest, head. HandItems: mainhand, offhand.
        val armor = listOf(e.feet, e.legs, e.chest, e.head).joinToString(",") { itemNbt(it) }
        val hands = listOf(e.mainHand, e.offHand).joinToString(",") { itemNbt(it) }
        return "ArmorItems:[$armor],HandItems:[$hands]"
    }

    private fun itemNbt(base64: String?): String {
        if (base64 == null) return "{}"
        val item = ItemCodec.decode(base64) ?: return "{}"
        return "{id:\"${item.type.key}\",count:${item.amount.coerceAtLeast(1)}}"
    }

    private fun displayNbt(el: DisplayElement, tag: String?): String {
        val tags = mutableListOf(
            "transformation:${transformNbt(el.transform)}",
            "billboard:\"${el.billboard.lowercase()}\"",
            "view_range:${f(el.viewRange.toDouble())}",
        )
        if (el.brightnessBlock != null && el.brightnessSky != null) {
            tags += "brightness:{block:${el.brightnessBlock},sky:${el.brightnessSky}}"
        }
        el.glowColor?.let { tags += "Glowing:1b,glow_color_override:$it" }
        when (el.kind) {
            DisplayKind.ITEM -> ItemCodec.decode(el.payload)?.let {
                tags += "item:{id:\"${it.type.key}\",count:${it.amount.coerceAtLeast(1)}}"
            }
            DisplayKind.BLOCK -> tags += "block_state:{Name:\"${blockName(el.payload)}\"}"
            DisplayKind.TEXT -> tags += "text:'${jsonText(plain(el.payload))}'"
        }
        tag?.let { tags += "Tags:[\"$it\"]" }
        return "{${tags.joinToString(",")}}"
    }

    internal fun transformNbt(t: com.tinyyana.awesomeArmorStandEditor.model.Transform): String {
        fun v3(v: com.tinyyana.awesomeArmorStandEditor.model.Vec3) = "[${f(v.x)},${f(v.y)},${f(v.z)}]"
        fun q(q: com.tinyyana.awesomeArmorStandEditor.model.Quat) = "[${f(q.x)},${f(q.y)},${f(q.z)},${f(q.w)}]"
        return "{translation:${v3(t.translation)},left_rotation:${q(t.leftRotation)}," +
            "scale:${v3(t.scale)},right_rotation:${q(t.rightRotation)}}"
    }

    // --- formatting helpers ---

    private fun b(v: Boolean) = if (v) "1b" else "0b"

    private fun f(v: Double): String {
        val s = String.format(Locale.ROOT, "%.4f", v).let {
            if (it.contains('.')) it.trimEnd('0').trimEnd('.') else it
        }
        return "${s}f"
    }

    private fun deg(rad: Double) = f(Math.toDegrees(rad))

    private fun blockName(blockData: String): String =
        blockData.substringBefore('[').ifBlank { "minecraft:stone" }

    /** MiniMessage/section text -> plain text for NBT (formatting dropped in export). */
    private fun plain(s: String): String = s.replace(Regex("<[^>]*>"), "").replace(Regex("§."), "")

    private fun jsonText(text: String): String = "{\"text\":\"${text.replace("\\", "\\\\").replace("\"", "\\\"")}\"}"
}
