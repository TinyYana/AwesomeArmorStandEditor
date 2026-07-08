package com.tinyyana.awesomeArmorStandEditor.animation

import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
import com.tinyyana.awesomeArmorStandEditor.edit.InterpolationOps
import com.tinyyana.awesomeArmorStandEditor.model.Animation
import com.tinyyana.awesomeArmorStandEditor.session.EditSession
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Display
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Live keyframe playback over a session's placed entities. Displays get client-side interpolation
 * (cheap); armor stands are set per tick (server cost — so playback runs only for the editing
 * session that started it, and stopping restores the model state).
 */
class AnimationPlayer(private val plugin: AwesomeArmorStandEditorPlugin) {

    private val playing = ConcurrentHashMap<UUID, BukkitTask>()

    fun isPlaying(playerId: UUID): Boolean = playing.containsKey(playerId)

    fun play(session: EditSession): Boolean {
        val anim = session.scene.animation ?: return false
        if (anim.tracks.isEmpty() || anim.lengthTicks <= 0) return false
        stop(session)
        var t = 0
        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            // Self-cancel if the editor left — otherwise a looping animation ticks forever.
            if (plugin.server.getPlayer(session.playerId) == null) {
                stopSilently(session.playerId)
                return@Runnable
            }
            applyFrame(session, anim, t)
            t++
            if (t > anim.lengthTicks) {
                if (anim.loop) t = 0 else stop(session)
            }
        }, 0L, 1L)
        playing[session.playerId] = task
        return true
    }

    fun stop(session: EditSession) {
        val task = playing.remove(session.playerId) ?: return
        task.cancel()
        restore(session)
    }

    fun stopSilently(playerId: UUID) {
        playing.remove(playerId)?.cancel()
    }

    private fun applyFrame(session: EditSession, anim: Animation, t: Int) {
        for (track in anim.tracks) {
            val entity = session.entities[track.elementLocalId] ?: continue
            if (!entity.isValid) continue
            val kf = InterpolationOps.sample(track.sorted(), t)
            when {
                entity is ArmorStand && kf.pose != null -> plugin.placement.applyPose(entity, kf.pose)
                entity is Display && kf.transform != null -> plugin.placement.applyDisplayTransform(entity, kf.transform, 1)
            }
            val off = kf.offset
            val origin = session.origin
            if (off != null && origin != null) entity.teleport(origin.clone().add(off.x, off.y, off.z))
        }
    }

    /** Put entities back to their saved model state after playback. */
    private fun restore(session: EditSession) {
        for ((localId, entity) in session.entities) {
            if (!entity.isValid) continue
            val element = session.scene.elements.find { it.localId == localId } ?: continue
            plugin.placement.apply(entity, element)
            session.origin?.let { entity.teleport(plugin.placement.elementLocation(it, element)) }
        }
    }
}
