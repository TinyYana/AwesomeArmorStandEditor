package com.tinyyana.awesomeArmorStandEditor.text

import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Player-facing text: messages.yml (MiniMessage) -> Adventure Component, sent via BukkitAudiences
 * so it works on Spigot and Paper alike. All strings are externalized here; debug/log text is not.
 */
class Texts private constructor(private val audiences: BukkitAudiences) {

    private val mm = MiniMessage.miniMessage()

    @Volatile private var messages: Map<String, String> = emptyMap()
    @Volatile private var prefix: Component = Component.empty()

    fun send(sender: CommandSender, key: String, vararg placeholders: Pair<String, String>) {
        audiences.sender(sender).sendMessage(prefix.append(render(key, placeholders)))
    }

    fun actionbar(sender: CommandSender, key: String, vararg placeholders: Pair<String, String>) {
        audiences.sender(sender).sendActionBar(render(key, placeholders))
    }

    /** Send raw MiniMessage text as an action bar (for dynamic readouts). */
    fun actionbarRaw(sender: CommandSender, text: String) {
        audiences.sender(sender).sendActionBar(mm.deserialize(text))
    }

    /** Raw component, no prefix, italic forced off — for GUI item names/lore. */
    fun component(key: String, vararg placeholders: Pair<String, String>): Component =
        render(key, placeholders).decoration(TextDecoration.ITALIC, false)

    fun mini(text: String): Component = mm.deserialize(text).decoration(TextDecoration.ITALIC, false)

    fun sendComponent(sender: CommandSender, component: Component) {
        audiences.sender(sender).sendMessage(prefix.append(component))
    }

    fun raw(key: String): String? = messages[key]

    /** Legacy section-string render — for ItemMeta display name/lore (cross-platform, no Paper API). */
    fun legacy(key: String, vararg placeholders: Pair<String, String>): String =
        LegacyComponentSerializer.legacySection().serialize(component(key, *placeholders))

    fun legacyOf(component: Component): String = LegacyComponentSerializer.legacySection().serialize(component)

    private fun render(key: String, placeholders: Array<out Pair<String, String>>): Component {
        var text = messages[key] ?: return mm.deserialize("<red>$key")
        for ((k, v) in placeholders) text = text.replace("{$k}", v)
        return mm.deserialize(text)
    }

    fun reload(plugin: JavaPlugin) {
        val flat = readMessages(plugin)
        messages = flat
        prefix = flat["prefix"]?.let { mm.deserialize(it) } ?: Component.empty()
    }

    fun close() = audiences.close()

    companion object {
        fun load(plugin: JavaPlugin): Texts =
            Texts(BukkitAudiences.create(plugin)).also { it.reload(plugin) }

        private fun readMessages(plugin: JavaPlugin): Map<String, String> {
            plugin.saveResource("messages.yml", false)
            val user = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "messages.yml"))
            plugin.getResource("messages.yml")?.use { stream ->
                val defaults = YamlConfiguration.loadConfiguration(InputStreamReader(stream, StandardCharsets.UTF_8))
                user.setDefaults(defaults)
                user.options().copyDefaults(true)
            }
            val flat = HashMap<String, String>()
            for (key in user.getKeys(true)) if (user.isString(key)) flat[key] = user.getString(key)!!
            return flat
        }
    }
}
