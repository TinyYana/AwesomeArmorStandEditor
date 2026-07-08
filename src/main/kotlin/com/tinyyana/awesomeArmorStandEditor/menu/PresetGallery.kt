package com.tinyyana.awesomeArmorStandEditor.menu

import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
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
 * Pick-a-preset gallery: pose presets (top) apply to the selected armor stand; effect presets
 * (bottom) drop a tuned particle bundle. The zero-skill entry point, reachable from /aase.
 */
class PresetGallery(private val plugin: AwesomeArmorStandEditorPlugin) : Listener {

    private val texts get() = plugin.texts
    private val controller get() = plugin.controller

    private class Holder(val poseSlots: Map<Int, String>, val fxSlots: Map<Int, String>) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player) {
        val poseSlots = LinkedHashMap<Int, String>()
        val fxSlots = LinkedHashMap<Int, String>()
        var slot = 9
        for (p in plugin.presets.poses) { if (slot > 34) break; poseSlots[slot] = p.id; slot++ }
        slot = 36
        for (f in plugin.presets.fx) { if (slot > 52) break; fxSlots[slot] = f.id; slot++ }

        val holder = Holder(poseSlots, fxSlots)
        val inv = Bukkit.createInventory(holder, 54, texts.legacy("gallery.title"))
        holder.inv = inv

        inv.setItem(SLOT_INFO, icon(Material.NAME_TAG, texts.legacy("gallery.info")))
        inv.setItem(SLOT_MIRROR, icon(Material.LEVER, texts.legacy("gallery.mirror")))
        inv.setItem(SLOT_CLOSE, icon(Material.RED_STAINED_GLASS_PANE, texts.legacy("gallery.close")))
        inv.setItem(SLOT_POSE_LABEL, icon(Material.ARMOR_STAND, texts.legacy("gallery.poses-label")))
        inv.setItem(SLOT_FX_LABEL, icon(Material.BLAZE_POWDER, texts.legacy("gallery.fx-label")))

        val hint = texts.legacy("gallery.apply-hint")
        for ((s, id) in poseSlots) plugin.presets.pose(id)?.let { inv.setItem(s, named(it.icon, it.name, hint)) }
        for ((s, id) in fxSlots) plugin.presets.fx(id)?.let { inv.setItem(s, named(it.icon, it.name, hint)) }

        player.openInventory(inv)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder !is Holder) return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (event.clickedInventory !== event.inventory) return
        when (val slot = event.rawSlot) {
            SLOT_MIRROR -> controller.mirrorPose(player)
            SLOT_CLOSE -> player.closeInventory()
            in holder.poseSlots.keys -> controller.applyPose(player, holder.poseSlots.getValue(slot))
            in holder.fxSlots.keys -> controller.applyFx(player, holder.fxSlots.getValue(slot))
        }
    }

    @Suppress("DEPRECATION")
    private fun icon(material: Material, nameLegacy: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(nameLegacy)
        item.itemMeta = meta
        return item
    }

    @Suppress("DEPRECATION")
    private fun named(material: Material, plainName: String, hintLegacy: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName("§f$plainName")
        meta.lore = listOf(hintLegacy)
        item.itemMeta = meta
        return item
    }

    companion object {
        private const val SLOT_INFO = 0
        private const val SLOT_MIRROR = 4
        private const val SLOT_CLOSE = 8
        private const val SLOT_POSE_LABEL = 35
        private const val SLOT_FX_LABEL = 53
    }
}
