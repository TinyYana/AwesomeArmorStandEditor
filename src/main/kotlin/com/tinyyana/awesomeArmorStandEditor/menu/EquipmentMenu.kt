package com.tinyyana.awesomeArmorStandEditor.menu

import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
import com.tinyyana.awesomeArmorStandEditor.store.ItemCodec
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * Equip the selected armor stand by clicking a slot with an item on your cursor — no dragging items
 * out of your inventory, so nothing can ever be lost or duplicated. The click is always cancelled;
 * we only *copy* whatever is on the cursor into the model and re-apply live. Empty cursor = clear.
 */
class EquipmentMenu(private val plugin: AwesomeArmorStandEditorPlugin) : Listener {

    private val texts get() = plugin.texts
    private val controller get() = plugin.controller

    private class Holder : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    /** Opens the menu for the player, or tells them to select an armor stand first. */
    fun open(player: Player) {
        if (controller.equipmentSnapshot(player) == null) {
            texts.send(player, "equip-menu.only-stand")
            return
        }
        val holder = Holder()
        val inv = Bukkit.createInventory(holder, 27, texts.legacy("equip-menu.title"))
        holder.inv = inv
        populate(player, inv)
        player.openInventory(inv)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder !is Holder) return
        event.isCancelled = true                     // nothing ever moves in or out
        val player = event.whoClicked as? Player ?: return
        if (event.clickedInventory !== event.inventory) return  // clicks in the player's own inv: just blocked

        val slot = event.rawSlot
        if (slot == SLOT_BACK) { plugin.panel.open(player); return }
        val key = SLOTS[slot] ?: return

        val cursor = event.cursor
        val toSet = cursor?.takeIf { !it.type.isAir }?.clone()
        controller.setEquipItem(player, key, toSet)  // null cursor clears the slot
        populate(player, event.inventory)
    }

    /** Cancel drags so a click-drag can't dump items into the menu. */
    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is Holder) event.isCancelled = true
    }

    private fun populate(player: Player, inv: Inventory) {
        inv.clear()
        val snapshot = controller.equipmentSnapshot(player) ?: return
        inv.setItem(SLOT_INFO, icon(Material.ARMOR_STAND, texts.legacy("equip-menu.info")))
        for ((slot, key) in SLOTS) {
            val encoded = snapshot[key]
            inv.setItem(slot, slotItem(key, encoded))
        }
        inv.setItem(SLOT_BACK, icon(Material.ARROW, texts.legacy("equip-menu.back")))
    }

    /** Show the equipped item (with a "click to change/remove" hint) or a labelled empty placeholder. */
    @Suppress("DEPRECATION")
    private fun slotItem(key: String, encoded: String?): ItemStack {
        val label = texts.legacy("equip-menu.$key")
        val decoded = encoded?.let { ItemCodec.decode(it) }
        if (decoded != null && !decoded.type.isAir) {
            val item = decoded.clone()
            val meta = item.itemMeta
            if (meta != null) {
                val lore = (meta.lore ?: mutableListOf()).toMutableList()
                lore.add(0, label)
                lore.add(texts.legacy("equip-menu.filled-hint"))
                meta.lore = lore
                item.itemMeta = meta
            }
            return item
        }
        val pane = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
        val meta = pane.itemMeta ?: return pane
        meta.setDisplayName(label)
        meta.lore = listOf(texts.legacy("equip-menu.empty-hint"))
        pane.itemMeta = meta
        return pane
    }

    @Suppress("DEPRECATION")
    private fun icon(material: Material, nameLegacy: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(nameLegacy)
        item.itemMeta = meta
        return item
    }

    companion object {
        private const val SLOT_INFO = 4
        private const val SLOT_BACK = 22

        // head, chest, legs, feet, mainhand, offhand along the middle row.
        private val SLOTS = linkedMapOf(
            10 to "head", 11 to "chest", 12 to "legs",
            13 to "feet", 14 to "mainhand", 15 to "offhand",
        )
    }
}
