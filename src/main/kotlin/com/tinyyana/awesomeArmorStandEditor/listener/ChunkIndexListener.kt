package com.tinyyana.awesomeArmorStandEditor.listener

import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.world.ChunkLoadEvent

/** Keep the in-memory registry aware of our entities as chunks load (for counts/purge). */
class ChunkIndexListener(private val plugin: AwesomeArmorStandEditorPlugin) : org.bukkit.event.Listener {

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        plugin.registry.indexChunk(event.chunk)
        plugin.particles.indexChunk(event.chunk)
    }
}
