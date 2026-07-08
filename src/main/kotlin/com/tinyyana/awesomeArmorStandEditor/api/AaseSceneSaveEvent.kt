package com.tinyyana.awesomeArmorStandEditor.api

import com.tinyyana.awesomeArmorStandEditor.model.Scene
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired after a player saves a scene blueprint to disk. Informational (not cancellable) — a third
 * party can log it, mirror the file, award something, etc. Part of the plugin's public API surface;
 * other plugins hook us rather than us depending on them.
 */
class AaseSceneSaveEvent(val player: Player, val scene: Scene) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic private val HANDLERS = HandlerList()
        @JvmStatic fun getHandlerList(): HandlerList = HANDLERS
    }
}
