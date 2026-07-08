package com.tinyyana.awesomeArmorStandEditor.region

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot

/** Decides whether a player may edit/place at a location, respecting land-protection plugins. */
interface RegionGuard {
    fun canBuild(player: Player, location: Location): Boolean
}

/** No land integration — always allows. Used when the event probe is disabled by config. */
object PermissiveGuard : RegionGuard {
    override fun canBuild(player: Player, location: Location): Boolean = true
}

/**
 * Universal guard: fires a synthetic [BlockPlaceEvent] and honours any land plugin's veto
 * (GriefPrevention/WorldGuard/Towny/Lands all listen to it). No block is actually placed.
 *
 * Must be called on the main thread (edit handlers already are). The event constructor is the
 * public non-deprecated one; EntityPlaceEvent is @ApiStatus.Internal, so we use BlockPlaceEvent.
 */
class EventProbeGuard : RegionGuard {
    override fun canBuild(player: Player, location: Location): Boolean {
        val block = location.block
        val against = block.getRelative(BlockFace.DOWN)
        val hand = player.inventory.itemInMainHand
        val event = BlockPlaceEvent(block, block.state, against, hand, player, true, EquipmentSlot.HAND)
        Bukkit.getPluginManager().callEvent(event)
        return event.canBuild() && !event.isCancelled
    }
}
