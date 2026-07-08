package com.tinyyana.awesomeArmorStandEditor.menu

import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.io.File

/**
 * The in-game manual as a flip-through written book (/aase guide). Pages are data-driven from
 * guide.yml (MiniMessage) so they're translatable and reloadable. Cross-platform: legacy pages +
 * openBook (Bukkit API), with a give-to-inventory fallback.
 */
class GuideBook(private val plugin: AwesomeArmorStandEditorPlugin) {

    @Volatile private var pages: List<String> = emptyList()

    fun reload() {
        plugin.saveResource("guide.yml", false)
        val cfg = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "guide.yml"))
        pages = cfg.getStringList("pages")
    }

    @Suppress("DEPRECATION")
    fun open(player: Player) {
        val book = ItemStack(Material.WRITTEN_BOOK)
        val meta = book.itemMeta as? BookMeta ?: return
        meta.title = plugin.texts.legacyOf(plugin.texts.mini(plugin.texts.raw("guide.book-title") ?: "盔甲座編輯器 手冊"))
        meta.author = "AwesomeArmorStandEditor"
        meta.setPages(pages.map { plugin.texts.legacyOf(plugin.texts.mini(it)) })
        book.itemMeta = meta
        runCatching { player.openBook(book) }.onFailure { player.inventory.addItem(book) }
    }
}
