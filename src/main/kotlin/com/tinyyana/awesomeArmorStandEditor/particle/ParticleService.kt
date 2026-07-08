package com.tinyyana.awesomeArmorStandEditor.particle

import com.tinyyana.awesomeArmorStandEditor.AaseKeys
import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
import com.tinyyana.awesomeArmorStandEditor.model.ParticleEmitter
import org.bukkit.Chunk
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * Particle emitters are invisible marker entities carrying their params in PDC, so they persist with
 * the placed art. A single global ticker emits only for markers in loaded chunks with a player in
 * range, and stops at a per-tick budget. No world scan — markers are indexed as chunks load.
 */
class ParticleService(private val plugin: AwesomeArmorStandEditorPlugin, private val keys: AaseKeys) {

    private val markers = java.util.concurrent.ConcurrentHashMap.newKeySet<Entity>()
    private var tick = 0L
    private var task: BukkitTask? = null

    fun start() {
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable { run() }, 20L, 1L)
    }

    fun stop() {
        task?.cancel(); task = null
    }

    fun spawnEmitter(origin: Location, sceneId: String, owner: UUID, emitter: ParticleEmitter): Entity {
        val loc = origin.clone().add(emitter.offset.x, emitter.offset.y, emitter.offset.z)
        val world = loc.world ?: error("no world")
        val marker = world.spawn(loc, ArmorStand::class.java) {
            it.isMarker = true; it.isInvisible = true; it.isSmall = true
            it.setGravity(false); it.setBasePlate(false)
        }
        val pdc = marker.persistentDataContainer
        pdc.set(keys.owner, PersistentDataType.STRING, owner.toString())
        pdc.set(keys.scene, PersistentDataType.STRING, sceneId)
        pdc.set(keys.local, PersistentDataType.INTEGER, emitter.id)
        pdc.set(keys.emitter, PersistentDataType.STRING, encode(emitter))
        markers.add(marker)
        return marker
    }

    fun indexChunk(chunk: Chunk) {
        for (e in chunk.entities) if (e.persistentDataContainer.has(keys.emitter, PersistentDataType.STRING)) markers.add(e)
    }

    /** One-time startup index of emitter markers in currently-loaded chunks. */
    fun indexLoaded() {
        for (world in plugin.server.worlds) for (chunk in world.loadedChunks) indexChunk(chunk)
    }

    fun removeForScene(sceneId: String) {
        val it = markers.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.persistentDataContainer.get(keys.scene, PersistentDataType.STRING) == sceneId) {
                if (!e.isDead) e.remove(); it.remove()
            }
        }
    }

    private fun run() {
        tick++
        var budget = plugin.settings.particleBudget
        val range = plugin.settings.particleRange.toDouble()
        val rangeSq = range * range
        val it = markers.iterator()
        while (it.hasNext()) {
            val marker = it.next()
            if (!marker.isValid) { it.remove(); continue }
            val data = marker.persistentDataContainer.get(keys.emitter, PersistentDataType.STRING) ?: continue
            val e = decode(data) ?: continue
            if (e.rateTicks <= 0 || tick % e.rateTicks != 0L) continue
            val loc = marker.location
            if (loc.world?.players?.none { it.location.distanceSquared(loc) <= rangeSq } != false) continue
            emit(loc, e)
            if (--budget <= 0) break
        }
    }

    private fun emit(loc: Location, e: ParticleEmitter) {
        val world = loc.world ?: return
        runCatching {
            val particle = Particle.valueOf(e.particle.uppercase())
            if (particle.dataType == Particle.DustOptions::class.java) {
                val dust = Particle.DustOptions(Color.fromRGB(e.dustColor and 0xFFFFFF), 1.0f)
                world.spawnParticle(particle, loc, e.count, e.spread.x, e.spread.y, e.spread.z, e.speed, dust)
            } else {
                world.spawnParticle(particle, loc, e.count, e.spread.x, e.spread.y, e.spread.z, e.speed)
            }
        }
    }

    private fun encode(e: ParticleEmitter): String =
        listOf(e.particle, e.count, e.speed, e.spread.x, e.spread.y, e.spread.z, e.rateTicks, e.dustColor).joinToString(";")

    private fun decode(s: String): ParticleEmitter? = runCatching {
        val p = s.split(";")
        ParticleEmitter(
            id = 0, particle = p[0], offset = com.tinyyana.awesomeArmorStandEditor.model.Vec3.ZERO,
            count = p[1].toInt(), speed = p[2].toDouble(),
            spread = com.tinyyana.awesomeArmorStandEditor.model.Vec3(p[3].toDouble(), p[4].toDouble(), p[5].toDouble()),
            rateTicks = p[6].toInt(), dustColor = p[7].toInt(),
        )
    }.getOrNull()
}
