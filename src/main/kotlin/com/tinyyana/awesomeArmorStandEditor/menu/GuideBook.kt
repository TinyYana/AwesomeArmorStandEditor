package com.tinyyana.awesomeArmorStandEditor.menu

import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

/**
 * The in-game manual as a flip-through written book (/aase guide). Pages are data-driven from
 * `guide.pages` in the active lang file (MiniMessage), so they translate and reload with everything
 * else. Cross-platform: legacy pages + openBook (Bukkit API), with a give-to-inventory fallback.
 */
class GuideBook(private val plugin: AwesomeArmorStandEditorPlugin) {

    @Suppress("DEPRECATION")
    fun open(player: Player) {
        val book = ItemStack(Material.WRITTEN_BOOK)
        val meta = book.itemMeta as? BookMeta ?: return
        meta.title = plugin.texts.legacyOf(plugin.texts.mini(plugin.texts.label("guide.book-title")))
        meta.author = "AwesomeArmorStandEditor"
        meta.setPages(plugin.texts.guidePages().map { plugin.texts.legacyOf(plugin.texts.mini(it)) })
        book.itemMeta = meta
        runCatching { player.openBook(book) }.onFailure { player.inventory.addItem(book) }
    }
}
