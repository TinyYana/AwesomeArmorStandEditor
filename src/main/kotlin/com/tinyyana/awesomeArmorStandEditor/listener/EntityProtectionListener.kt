package com.tinyyana.awesomeArmorStandEditor.listener

import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent

/** Keeps our elements from being destroyed or fiddled with by vanilla mechanics. */
class EntityProtectionListener(private val plugin: AwesomeArmorStandEditorPlugin) : Listener {

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        if (plugin.registry.isOurs(event.entity)) event.isCancelled = true
    }

    @EventHandler
    fun onManipulate(event: PlayerArmorStandManipulateEvent) {
        // Editing equipment goes through the GUI; block vanilla equip-swapping on our stands.
        if (plugin.registry.isOurs(event.rightClicked)) event.isCancelled = true
    }
}
