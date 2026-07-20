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

/** Paged pose/effect gallery. Slot actions keep preset ids, never transient list indices. */
class PresetGallery(private val plugin: AwesomeArmorStandEditorPlugin) : Listener {

    private val texts get() = plugin.texts
    private val controller get() = plugin.controller

    private sealed interface Entry {
        val id: String
        val icon: Material
        val name: String

        data class Pose(override val id: String, override val icon: Material, override val name: String) : Entry
        data class Fx(override val id: String, override val icon: Material, override val name: String) : Entry
    }

    private sealed interface Action {
        data class Apply(val entry: Entry) : Action
        data class Page(val number: Int) : Action
        data object Mirror : Action
        data object Back : Action
        data object Home : Action
        data object Help : Action
        data object Close : Action
    }

    private class Holder(val page: Int, val actions: Map<Int, Action>) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, requestedPage: Int = 1) {
        val entries = plugin.presets.poses.map { Entry.Pose(it.id, it.icon, texts.presetName(it.id, it.name)) } +
            plugin.presets.fx.map { Entry.Fx(it.id, it.icon, texts.presetName(it.id, it.name)) }
        val page = PageWindow.resolve(entries, requestedPage, LIST_CONTENT_SLOTS.size)
        val actions = mutableMapOf<Int, Action>()
        val holder = Holder(page.number, actions)
        val inv = Bukkit.createInventory(holder, 54, texts.legacy("gallery.title"))
        holder.inv = inv

        inv.setItem(SLOT_INFO, icon(Material.NAME_TAG, texts.legacy("gallery.info")))
        page.content.forEachIndexed { index, entry ->
            val slot = LIST_CONTENT_SLOTS[index]
            val kind = texts.legacy(if (entry is Entry.Pose) "gallery.pose-kind" else "gallery.fx-kind")
            inv.setItem(slot, named(entry.icon, entry.name, listOf(kind, texts.legacy("gallery.apply-hint"))))
            actions[slot] = Action.Apply(entry)
        }
        if (entries.isEmpty()) inv.setItem(22, icon(Material.OAK_SIGN, texts.legacy("gallery.empty")))

        inv.setItem(SLOT_SECONDARY, icon(Material.LEVER, texts.legacy("gallery.mirror")))
        actions[SLOT_SECONDARY] = Action.Mirror
        inv.setItem(SLOT_BACK, icon(Material.ARROW, texts.legacy("gallery.back")))
        actions[SLOT_BACK] = Action.Back
        inv.setItem(SLOT_HOME, icon(Material.NETHER_STAR, texts.legacy("gallery.home")))
        actions[SLOT_HOME] = Action.Home
        inv.setItem(SLOT_PREVIOUS, icon(if (page.hasPrevious) Material.ARROW else Material.GRAY_DYE, texts.legacy(if (page.hasPrevious) "gallery.previous" else "gallery.previous-disabled")))
        if (page.hasPrevious) actions[SLOT_PREVIOUS] = Action.Page(page.number - 1)
        inv.setItem(SLOT_PAGE, icon(Material.PAPER, texts.legacy("gallery.page", "page" to page.number.toString(), "pages" to page.totalPages.toString(), "count" to page.totalItems.toString())))
        inv.setItem(SLOT_NEXT, icon(if (page.hasNext) Material.ARROW else Material.GRAY_DYE, texts.legacy(if (page.hasNext) "gallery.next" else "gallery.next-disabled")))
        if (page.hasNext) actions[SLOT_NEXT] = Action.Page(page.number + 1)
        inv.setItem(SLOT_HELP, icon(Material.KNOWLEDGE_BOOK, texts.legacy("gallery.help")))
        actions[SLOT_HELP] = Action.Help
        inv.setItem(SLOT_CLOSE, icon(Material.BARRIER, texts.legacy("gallery.close")))
        actions[SLOT_CLOSE] = Action.Close

        player.openInventory(inv)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder !is Holder) return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (event.clickedInventory !== event.inventory) return
        when (val action = holder.actions[event.rawSlot]) {
            Action.Mirror -> {
                controller.mirrorPose(player)
                open(player, holder.page)
            }
            Action.Back -> plugin.panel.open(player)
            Action.Home -> {
                player.closeInventory()
                player.performCommand("menu")
            }
            Action.Help -> {
                player.closeInventory()
                player.performCommand("lyco guide armorstand-editor")
            }
            Action.Close -> player.closeInventory()
            is Action.Page -> open(player, action.number)
            is Action.Apply -> {
                when (val entry = action.entry) {
                    is Entry.Pose -> controller.applyPose(player, entry.id)
                    is Entry.Fx -> controller.applyFx(player, entry.id)
                }
                open(player, holder.page)
            }
            null -> Unit
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
    private fun named(material: Material, plainName: String, lore: List<String>): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName("§d§l$plainName")
        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    companion object {
        private const val SLOT_INFO = 4
        private const val SLOT_SECONDARY = 7
    }
}
