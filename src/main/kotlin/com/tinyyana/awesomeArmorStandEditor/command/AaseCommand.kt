package com.tinyyana.awesomeArmorStandEditor.command

import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
import com.tinyyana.awesomeArmorStandEditor.edit.ToolItem
import com.tinyyana.awesomeArmorStandEditor.model.DisplayKind
import com.tinyyana.awesomeArmorStandEditor.store.ItemCodec
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class AaseCommand(private val plugin: AwesomeArmorStandEditorPlugin) : TabExecutor {

    private val texts get() = plugin.texts
    private val controller get() = plugin.controller

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val sub = args.firstOrNull()?.lowercase() ?: ""

        // Console can only reload.
        if (sender !is Player) {
            if (sub == "reload" && sender.hasPermission("aase.admin")) {
                plugin.reloadAll(); texts.send(sender, "reload.ok")
            } else {
                texts.send(sender, "system.player-only")
            }
            return true
        }

        when (sub) {
            "" -> require(sender, "aase.use") { plugin.panel.open(sender) }
            "tool" -> require(sender, "aase.use") { giveTool(sender) }
            "new" -> require(sender, "aase.use") {
                val name = args.getOrNull(1)
                if (name == null) deny(sender, "usage.new") else controller.openNew(sender, name)
            }
            "addstand" -> require(sender, "aase.create.armorstand") { controller.addStand(sender) }
            "adddisplay" -> require(sender, "aase.create.display") { addDisplay(sender, args.getOrNull(1)) }
            "setblock" -> require(sender, "aase.use") {
                controller.setDisplayPayload(sender, DisplayKind.BLOCK, args.drop(1).joinToString(" ").ifBlank { "minecraft:stone" })
            }
            "settext" -> require(sender, "aase.use") {
                controller.setDisplayPayload(sender, DisplayKind.TEXT, args.drop(1).joinToString(" "))
            }
            "setitem" -> require(sender, "aase.use") { controller.setDisplayItemFromOffhand(sender) }
            "setequip" -> require(sender, "aase.use") {
                val slot = args.getOrNull(1)
                if (slot == null) deny(sender, "usage.setequip") else controller.setEquip(sender, slot)
            }
            "flag" -> require(sender, "aase.use") {
                val flag = args.getOrNull(1)
                if (flag == null) deny(sender, "usage.flag") else controller.toggleFlag(sender, flag)
            }
            "setname" -> require(sender, "aase.use") {
                controller.setName(sender, args.drop(1).joinToString(" ").ifBlank { null })
            }
            "save" -> require(sender, "aase.scene.save") { controller.save(sender) }
            "load" -> require(sender, "aase.use") {
                val name = args.getOrNull(1)
                if (name == null) deny(sender, "usage.load") else controller.loadFresh(sender, name)
            }
            "edit" -> require(sender, "aase.use") { controller.editFromTarget(sender) }
            "list" -> require(sender, "aase.use") { listScenes(sender) }
            "delete" -> require(sender, "aase.use") { controller.deleteSelected(sender) }
            "export" -> require(sender, "aase.export.command") {
                when (args.getOrNull(1)?.lowercase()) {
                    "command" -> controller.exportCommands(sender)
                    "function", "mcfunction" -> controller.exportMcFunction(sender)
                    else -> deny(sender, "usage.export")
                }
            }
            "particle" -> require(sender, "aase.use") {
                when (args.getOrNull(1)?.lowercase()) {
                    "add" -> args.getOrNull(2)?.let { controller.addEmitter(sender, it) } ?: deny(sender, "usage.particle")
                    "clear" -> controller.clearEmitters(sender)
                    else -> deny(sender, "usage.particle")
                }
            }
            "anim" -> require(sender, "aase.animate") {
                when (args.getOrNull(1)?.lowercase()) {
                    "key" -> controller.animKey(sender, args.getOrNull(2)?.toIntOrNull() ?: 0)
                    "length" -> args.getOrNull(2)?.toIntOrNull()?.let { controller.animLength(sender, it) } ?: deny(sender, "usage.anim")
                    "loop" -> controller.animToggleLoop(sender)
                    "play" -> controller.animPlay(sender)
                    "stop" -> controller.animStop(sender)
                    "clear" -> controller.animClear(sender)
                    else -> deny(sender, "usage.anim")
                }
            }
            "guide" -> require(sender, "aase.use") {
                if (sender is Player) plugin.guideBook.open(sender) else texts.send(sender, "system.player-only")
            }
            "presets", "gallery" -> require(sender, "aase.use") { plugin.gallery.open(sender) }
            "pose" -> require(sender, "aase.use") {
                when (val a = args.getOrNull(1)?.lowercase()) {
                    null -> deny(sender, "usage.pose")
                    "save" -> args.getOrNull(2)?.let { controller.savePose(sender, it, args.drop(3).joinToString(" ").ifBlank { null }) }
                        ?: deny(sender, "usage.pose")
                    else -> controller.applyPose(sender, a)
                }
            }
            "fx" -> require(sender, "aase.use") {
                args.getOrNull(1)?.let { controller.applyFx(sender, it) } ?: deny(sender, "usage.fx")
            }
            "mirror" -> require(sender, "aase.use") { controller.mirrorPose(sender) }
            "close" -> require(sender, "aase.use") { controller.close(sender) }
            "reload" -> require(sender, "aase.admin") { plugin.reloadAll(); texts.send(sender, "reload.ok") }
            else -> help(sender)
        }
        return true
    }

    private fun addDisplay(player: Player, kindArg: String?) {
        val kind = when (kindArg?.lowercase()) {
            "item" -> DisplayKind.ITEM
            "block" -> DisplayKind.BLOCK
            "text" -> DisplayKind.TEXT
            else -> return deny(player, "usage.adddisplay")
        }
        val payload = when (kind) {
            DisplayKind.ITEM -> {
                val off = player.inventory.itemInOffHand
                ItemCodec.encode(if (off.type.isAir) ItemStack(Material.STONE) else off)
            }
            DisplayKind.BLOCK -> "minecraft:stone"
            DisplayKind.TEXT -> texts.raw("display.default-text") ?: "文字"
        }
        controller.addDisplay(player, kind, payload)
    }

    private fun giveTool(player: Player) {
        val lore = listOf("tool.lore1", "tool.lore2", "tool.lore3", "tool.lore4")
            .filter { texts.raw(it) != null }
            .map { texts.legacy(it) }
        val tool = ToolItem.create(plugin.keys, plugin.settings.toolMaterial, texts.legacy("tool.name"), lore)
        player.inventory.addItem(tool)
        texts.send(player, "tool.given")
    }

    private fun listScenes(player: Player) {
        val scenes = plugin.store.list(player.uniqueId)
        if (scenes.isEmpty()) return texts.send(player, "list.empty")
        texts.send(player, "list.header", "count" to scenes.size.toString())
        for (s in scenes) texts.send(player, "list.entry", "name" to s.name, "count" to s.elements.size.toString())
    }

    private fun help(sender: CommandSender) {
        // Players get the full flip-through book; console gets the quick chat list.
        if (sender is Player) {
            plugin.guideBook.open(sender)
            texts.send(sender, "help.opened-book")
            return
        }
        texts.send(sender, "help.header")
        for (line in listOf(
            "help.new", "help.tool", "help.presets", "help.addstand", "help.adddisplay", "help.controls",
            "help.save", "help.load", "help.edit", "help.list", "help.export", "help.close",
        )) if (texts.raw(line) != null) texts.send(sender, line)
    }

    private inline fun require(sender: CommandSender, permission: String, block: () -> Unit) {
        if (!sender.hasPermission(permission)) texts.send(sender, "system.no-permission") else block()
    }

    private fun deny(sender: CommandSender, usageKey: String) = texts.send(sender, usageKey)

    private val subcommands = listOf(
        "guide", "tool", "new", "presets", "pose", "fx", "mirror", "addstand", "adddisplay", "setblock", "settext",
        "setitem", "setname", "setequip", "flag", "particle", "anim", "save", "load", "edit", "list", "delete",
        "export", "close", "reload",
    )

    private val equipSlots = listOf("head", "chest", "legs", "feet", "mainhand", "offhand")
    private val flagNames = listOf("small", "invisible", "nobaseplate", "nogravity", "arms", "marker", "glowing")
    private val commonParticles = listOf("HAPPY_VILLAGER", "HEART", "FLAME", "SOUL_FIRE_FLAME", "END_ROD", "CHERRY_LEAVES", "DUST", "CRIT", "ENCHANT", "PORTAL")

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> subcommands.filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "adddisplay" -> listOf("item", "block", "text").filter { it.startsWith(args[1].lowercase()) }
                "export" -> listOf("command", "function").filter { it.startsWith(args[1].lowercase()) }
                "setequip" -> equipSlots.filter { it.startsWith(args[1].lowercase()) }
                "flag" -> flagNames.filter { it.startsWith(args[1].lowercase()) }
                "particle" -> listOf("add", "clear").filter { it.startsWith(args[1].lowercase()) }
                "anim" -> listOf("key", "length", "loop", "play", "stop", "clear").filter { it.startsWith(args[1].lowercase()) }
                "pose" -> (listOf("save") + plugin.presets.poses.map { it.id }).filter { it.startsWith(args[1].lowercase()) }
                "fx" -> plugin.presets.fx.map { it.id }.filter { it.startsWith(args[1].lowercase()) }
                "load" -> if (sender is Player) plugin.store.list(sender.uniqueId).map { it.name }.filter { it.startsWith(args[1]) } else emptyList()
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "particle" -> if (args[1].equals("add", true)) commonParticles.filter { it.startsWith(args[2].uppercase()) } else emptyList()
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
