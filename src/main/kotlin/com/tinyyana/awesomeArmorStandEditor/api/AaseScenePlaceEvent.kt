package com.tinyyana.awesomeArmorStandEditor.api

import com.tinyyana.awesomeArmorStandEditor.model.Scene
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired before a scene blueprint is placed into the world (via load or share-code import). Cancel it
 * to veto the placement — e.g. a plot/economy plugin gating where blueprints may be dropped. The
 * plugin's own ownership/limit/region checks run first; this is the third-party hook on top.
 */
class AaseScenePlaceEvent(val player: Player, val scene: Scene, val origin: Location) : Event(), Cancellable {
    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled
    override fun setCancelled(cancel: Boolean) { cancelled = cancel }
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic private val HANDLERS = HandlerList()
        @JvmStatic fun getHandlerList(): HandlerList = HANDLERS
    }
}
