package com.tinyyana.awesomeArmorStandEditor.menu

import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
import com.tinyyana.awesomeArmorStandEditor.model.ArmorStandElement
import com.tinyyana.awesomeArmorStandEditor.model.DisplayKind
import com.tinyyana.awesomeArmorStandEditor.session.EditMode
import com.tinyyana.awesomeArmorStandEditor.edit.Axis
import com.tinyyana.awesomeArmorStandEditor.edit.BodyPart
import com.tinyyana.awesomeArmorStandEditor.store.ItemCodec
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * The control panel GUI. Mirrors the in-world tool (mode/part/axis/nudge) and adds flags, equipment
 * hint, save/export/delete. Holder+dispatch pattern; all clicks cancelled. Cross-platform ItemMeta.
 */
class ControlPanel(private val plugin: AwesomeArmorStandEditorPlugin) : Listener {

    private val texts get() = plugin.texts
    private val controller get() = plugin.controller

    private class Holder : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player) {
        val holder = Holder()
        val inv = Bukkit.createInventory(holder, 54, texts.legacy("panel.title"))
        holder.inv = inv
        populate(player, inv)
        player.openInventory(inv)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder !is Holder) return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (event.clickedInventory !== event.inventory) return

        when (event.rawSlot) {
            SLOT_ADD_STAND -> controller.addStand(player)
            SLOT_ADD_ITEM -> controller.addDisplay(player, DisplayKind.ITEM, itemPayload(player))
            SLOT_ADD_BLOCK -> controller.addDisplay(player, DisplayKind.BLOCK, "minecraft:stone")
            SLOT_ADD_TEXT -> controller.addDisplay(player, DisplayKind.TEXT, texts.raw("display.default-text") ?: "文字")

            SLOT_MODE_MOVE -> setMode(player, EditMode.MOVE)
            SLOT_MODE_POSE -> setMode(player, EditMode.POSE)
            SLOT_MODE_TRANSLATE -> setMode(player, EditMode.TRANSLATE)
            SLOT_MODE_ROTATE -> setMode(player, EditMode.ROTATE)
            SLOT_MODE_SCALE -> setMode(player, EditMode.SCALE)

            SLOT_AXIS_X -> setAxis(player, Axis.X)
            SLOT_AXIS_Y -> setAxis(player, Axis.Y)
            SLOT_AXIS_Z -> setAxis(player, Axis.Z)

            SLOT_STEP_DOWN -> controller.cycleStep(player, -1)
            SLOT_STEP_UP -> controller.cycleStep(player, 1)
            SLOT_NUDGE_DOWN -> controller.adjust(player, -1)
            SLOT_NUDGE_UP -> controller.adjust(player, 1)

            SLOT_EQUIP -> texts.send(player, "equip.hint")
            SLOT_PRESETS -> { plugin.gallery.open(player); return }
            SLOT_SAVE -> controller.save(player)
            SLOT_EXPORT -> controller.exportCommands(player)
            SLOT_DELETE -> controller.deleteSelected(player)
            SLOT_CLOSE -> { player.closeInventory(); return }

            in PART_SLOTS.keys -> setPart(player, PART_SLOTS.getValue(event.rawSlot))
            in FLAG_SLOTS.keys -> controller.toggleFlag(player, FLAG_SLOTS.getValue(event.rawSlot))
            else -> return
        }
        // Repopulate in place so highlights/values refresh.
        populate(player, event.inventory)
    }

    private fun setMode(player: Player, mode: EditMode) {
        plugin.sessions.get(player.uniqueId)?.let { it.mode = mode; controller.readout(player, it) }
    }

    private fun setAxis(player: Player, axis: Axis) {
        plugin.sessions.get(player.uniqueId)?.let { it.axis = axis; controller.readout(player, it) }
    }

    private fun setPart(player: Player, part: BodyPart) {
        plugin.sessions.get(player.uniqueId)?.let { it.part = part; controller.readout(player, it) }
    }

    private fun itemPayload(player: Player): String {
        val off = player.inventory.itemInOffHand
        return ItemCodec.encode(if (off.type.isAir) ItemStack(Material.STONE) else off)
    }

    private fun populate(player: Player, inv: Inventory) {
        inv.clear()
        val session = plugin.sessions.get(player.uniqueId)
        val selected = session?.selected()
        val isStand = selected is ArmorStandElement

        inv.setItem(SLOT_INFO, icon(Material.NAME_TAG, "panel.info",
            "name" to (session?.scene?.name ?: "-"),
            "sel" to (selected?.let { "#${it.localId}" } ?: "-")))

        inv.setItem(SLOT_ADD_STAND, icon(Material.ARMOR_STAND, "panel.add-stand"))
        inv.setItem(SLOT_ADD_ITEM, icon(Material.ITEM_FRAME, "panel.add-item"))
        inv.setItem(SLOT_ADD_BLOCK, icon(Material.GRASS_BLOCK, "panel.add-block"))
        inv.setItem(SLOT_ADD_TEXT, icon(Material.OAK_SIGN, "panel.add-text"))

        inv.setItem(SLOT_MODE_MOVE, modeIcon(Material.ENDER_PEARL, "panel.mode-move", session?.mode, EditMode.MOVE))
        inv.setItem(SLOT_MODE_POSE, modeIcon(Material.ARMOR_STAND, "panel.mode-pose", session?.mode, EditMode.POSE))
        inv.setItem(SLOT_MODE_TRANSLATE, modeIcon(Material.PISTON, "panel.mode-translate", session?.mode, EditMode.TRANSLATE))
        inv.setItem(SLOT_MODE_ROTATE, modeIcon(Material.CLOCK, "panel.mode-rotate", session?.mode, EditMode.ROTATE))
        inv.setItem(SLOT_MODE_SCALE, modeIcon(Material.SLIME_BALL, "panel.mode-scale", session?.mode, EditMode.SCALE))

        for ((slot, part) in PART_SLOTS) {
            val on = isStand && session?.part == part
            inv.setItem(slot, toggleIcon(if (on) Material.LIME_DYE else Material.GRAY_DYE, "panel.part-${part.name.lowercase()}", on))
        }

        for ((axis, slot) in mapOf(Axis.X to SLOT_AXIS_X, Axis.Y to SLOT_AXIS_Y, Axis.Z to SLOT_AXIS_Z)) {
            val on = session?.axis == axis
            inv.setItem(slot, toggleIcon(axisMaterial(axis, on), "panel.axis-${axis.name.lowercase()}", on))
        }

        inv.setItem(SLOT_STEP_DOWN, icon(Material.RED_DYE, "panel.step-down"))
        inv.setItem(SLOT_STEP_UP, icon(Material.LIME_DYE, "panel.step-up"))
        inv.setItem(SLOT_NUDGE_DOWN, icon(Material.RED_CONCRETE, "panel.nudge-down"))
        inv.setItem(SLOT_NUDGE_UP, icon(Material.LIME_CONCRETE, "panel.nudge-up"))

        for ((slot, flag) in FLAG_SLOTS) {
            val on = isStand && (selected as ArmorStandElement).flagValue(flag)
            inv.setItem(slot, toggleIcon(if (on) Material.LIME_DYE else Material.GRAY_DYE, "panel.flag-$flag", on))
        }
        inv.setItem(SLOT_EQUIP, icon(Material.CHEST, "panel.equip"))

        inv.setItem(SLOT_PRESETS, icon(Material.PAINTING, "panel.presets"))
        inv.setItem(SLOT_SAVE, icon(Material.WRITABLE_BOOK, "panel.save"))
        inv.setItem(SLOT_EXPORT, icon(Material.COMMAND_BLOCK, "panel.export"))
        inv.setItem(SLOT_DELETE, icon(Material.BARRIER, "panel.delete"))
        inv.setItem(SLOT_CLOSE, icon(Material.RED_STAINED_GLASS_PANE, "panel.close"))
    }

    private fun ArmorStandElement.flagValue(flag: String) = when (flag) {
        "small" -> flags.small; "invisible" -> flags.invisible; "nobaseplate" -> flags.noBasePlate
        "nogravity" -> flags.noGravity; "arms" -> flags.arms; "marker" -> flags.marker
        "glowing" -> flags.glowing; else -> false
    }

    private fun axisMaterial(axis: Axis, on: Boolean): Material = when (axis) {
        Axis.X -> if (on) Material.RED_CONCRETE else Material.RED_STAINED_GLASS
        Axis.Y -> if (on) Material.LIME_CONCRETE else Material.GREEN_STAINED_GLASS
        Axis.Z -> if (on) Material.LIGHT_BLUE_CONCRETE else Material.BLUE_STAINED_GLASS
    }

    private fun modeIcon(material: Material, key: String, current: EditMode?, mode: EditMode) =
        toggleIcon(material, key, current == mode)

    @Suppress("DEPRECATION")
    private fun icon(material: Material, nameKey: String, vararg ph: Pair<String, String>): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(texts.legacy(nameKey, *ph))
        item.itemMeta = meta
        return item
    }

    @Suppress("DEPRECATION")
    private fun toggleIcon(material: Material, nameKey: String, on: Boolean): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        val marker = if (on) "» " else ""
        meta.setDisplayName(marker + texts.legacy(nameKey))
        item.itemMeta = meta
        return item
    }

    companion object {
        private const val SLOT_INFO = 0
        private const val SLOT_ADD_STAND = 2
        private const val SLOT_ADD_ITEM = 3
        private const val SLOT_ADD_BLOCK = 4
        private const val SLOT_ADD_TEXT = 5

        private const val SLOT_MODE_MOVE = 9
        private const val SLOT_MODE_POSE = 10
        private const val SLOT_MODE_TRANSLATE = 11
        private const val SLOT_MODE_ROTATE = 12
        private const val SLOT_MODE_SCALE = 13

        private const val SLOT_AXIS_X = 27
        private const val SLOT_AXIS_Y = 28
        private const val SLOT_AXIS_Z = 29
        private const val SLOT_STEP_DOWN = 31
        private const val SLOT_STEP_UP = 32
        private const val SLOT_NUDGE_DOWN = 34
        private const val SLOT_NUDGE_UP = 35

        private const val SLOT_EQUIP = 44
        private const val SLOT_PRESETS = 51
        private const val SLOT_SAVE = 45
        private const val SLOT_EXPORT = 47
        private const val SLOT_DELETE = 49
        private const val SLOT_CLOSE = 53

        private val PART_SLOTS = mapOf(
            18 to BodyPart.HEAD, 19 to BodyPart.BODY, 20 to BodyPart.LEFT_ARM,
            21 to BodyPart.RIGHT_ARM, 22 to BodyPart.LEFT_LEG, 23 to BodyPart.RIGHT_LEG,
        )
        private val FLAG_SLOTS = mapOf(
            36 to "small", 37 to "invisible", 38 to "nobaseplate", 39 to "nogravity",
            40 to "arms", 41 to "marker", 42 to "glowing",
        )
    }
}
