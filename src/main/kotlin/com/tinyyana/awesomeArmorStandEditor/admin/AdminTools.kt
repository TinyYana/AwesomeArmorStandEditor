package com.tinyyana.awesomeArmorStandEditor.admin

import com.tinyyana.awesomeArmorStandEditor.AwesomeArmorStandEditorPlugin
import com.tinyyana.awesomeArmorStandEditor.integration.LycoLibHook
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Moderation tools: an admin needs a way to deal with art dumped in stupid places by someone else.
 *
 * Deliberate boundaries:
 *  - Only ever touches entities this plugin tagged (PDC owner key). A player's hand-placed vanilla
 *    armor stand is not ours to kill — /aase admin whois says so out loud.
 *  - Only reaches entities in *loaded* chunks. No world/chunk scan (hard performance rule), so a
 *    purge cannot reach art sitting in chunks nobody has loaded. Reported honestly to the admin.
 *  - Purge removes placed entities, NOT the owner's saved scene files. Least destructive thing that
 *    solves "get this out of my world"; the owner can still re-place it somewhere sane.
 *  - Radius purge is irreversible and hits other people's work, so it is two-stage: preview, then
 *    an explicit /aase admin confirm within [PendingPurge.TTL_MILLIS].
 */
class AdminTools(private val plugin: AwesomeArmorStandEditorPlugin) {

    private val pending = ConcurrentHashMap<UUID, PendingPurge>()

    // --- whois -------------------------------------------------------------

    fun whois(admin: Player) {
        val target = nearestOwned(admin) ?: return plugin.texts.send(admin, "admin.no-target")
        val tag = plugin.registry.read(target) ?: return plugin.texts.send(admin, "admin.no-target")
        val sceneName = plugin.store.load(tag.owner, tag.sceneId)?.name
            ?: plugin.texts.raw("admin.scene-missing") ?: "?"
        val loc = target.location

        plugin.texts.send(
            admin, "admin.whois",
            "owner" to ownerName(tag.owner),
            "scene" to sceneName,
            "id" to tag.localId.toString(),
        )
        plugin.texts.send(
            admin, "admin.whois-where",
            "world" to (loc.world?.name ?: "?"),
            "x" to loc.blockX.toString(), "y" to loc.blockY.toString(), "z" to loc.blockZ.toString(),
            "extra" to if (isEmitter(target)) (plugin.texts.raw("admin.whois-emitter") ?: "") else "",
        )
    }

    // --- remove one --------------------------------------------------------

    fun removeTargeted(admin: Player) {
        val target = nearestOwned(admin) ?: return plugin.texts.send(admin, "admin.no-target")
        val tag = plugin.registry.read(target) ?: return plugin.texts.send(admin, "admin.no-target")
        val loc = target.location
        remove(target)

        plugin.texts.send(
            admin, "admin.removed-one",
            "owner" to ownerName(tag.owner),
            "id" to tag.localId.toString(),
        )
        LycoLibHook.audit(
            plugin.name, admin.name, "admin.remove",
            "owner=${tag.owner} scene=${tag.sceneId} id=${tag.localId} " +
                "at=${loc.world?.name}:${loc.blockX},${loc.blockY},${loc.blockZ}",
        )
    }

    // --- purge radius (two-stage) -----------------------------------------

    fun previewPurge(admin: Player, args: List<String>) {
        val request = parsePurgeArgs(args, plugin.settings.maxPurgeRadius)
            ?: return plugin.texts.send(admin, "usage.admin")

        val ownerFilter = request.ownerName?.let {
            resolveOwner(it) ?: return plugin.texts.send(admin, "admin.owner-not-found", "name" to it)
        }
        val center = admin.location.clone()
        val victims = ownedNear(center, request.radius, ownerFilter)
        if (victims.isEmpty()) {
            return plugin.texts.send(admin, "admin.purge-none", "radius" to request.radius.toString())
        }

        pending[admin.uniqueId] = PendingPurge(
            center = center,
            radius = request.radius,
            ownerFilter = ownerFilter,
            createdAtMillis = System.currentTimeMillis(),
        )
        val ownerNote = request.ownerName?.let {
            plugin.texts.raw("admin.purge-preview-owner")?.replace("{owner}", it) ?: ""
        } ?: ""
        plugin.texts.send(
            admin, "admin.purge-preview",
            "radius" to request.radius.toString(),
            "count" to victims.size.toString(),
            "owner" to ownerNote,
        )
        plugin.texts.send(admin, "admin.purge-confirm-hint")
        plugin.texts.send(admin, "admin.loaded-only")
    }

    fun confirmPurge(admin: Player) {
        val plan = pending.remove(admin.uniqueId) ?: return plugin.texts.send(admin, "admin.purge-none-pending")
        if (plan.isExpired(System.currentTimeMillis())) return plugin.texts.send(admin, "admin.purge-expired")

        // Re-resolve against the world at confirm time: entities may have been removed or chunks
        // unloaded since the preview, and holding Entity references across ticks invites stale refs.
        val victims = ownedNear(plan.center, plan.radius, plan.ownerFilter)
        if (victims.isEmpty()) {
            return plugin.texts.send(admin, "admin.purge-none", "radius" to plan.radius.toString())
        }
        for (entity in victims) remove(entity)

        plugin.texts.send(admin, "admin.purge-done", "count" to victims.size.toString())
        LycoLibHook.audit(
            plugin.name, admin.name, "admin.purge",
            "count=${victims.size} radius=${plan.radius} owner=${plan.ownerFilter ?: "*"} " +
                "center=${plan.center.world?.name}:${plan.center.blockX},${plan.center.blockY},${plan.center.blockZ}",
        )
    }

    // --- internals ---------------------------------------------------------

    /** Same targeting rule as /aase edit: nearest owned entity within the tool's select range. */
    private fun nearestOwned(admin: Player): Entity? {
        val range = plugin.settings.selectRange.toDouble()
        return admin.getNearbyEntities(range, range, range)
            .filter { plugin.registry.isOurs(it) }
            .minByOrNull { it.location.distanceSquared(admin.eyeLocation) }
    }

    /** Bounded lookup over loaded chunks only — never a world scan. Box query, then true sphere. */
    private fun ownedNear(center: Location, radius: Int, ownerFilter: UUID?): List<Entity> {
        val world = center.world ?: return emptyList()
        val r = radius.toDouble()
        val rSq = r * r
        return world.getNearbyEntities(center, r, r, r)
            .filter { plugin.registry.isOurs(it) }
            .filter { it.location.distanceSquared(center) <= rSq }
            .filter { entity -> ownerFilter == null || plugin.registry.read(entity)?.owner == ownerFilter }
    }

    private fun remove(entity: Entity) {
        plugin.registry.forget(entity.uniqueId)
        detachFromSessions(entity)
        entity.remove()
        // Emitter markers drop out of ParticleService's map on its next tick (it skips !isValid).
    }

    /** An open editor session holds live entity refs; drop the ones we just killed. */
    private fun detachFromSessions(entity: Entity) {
        for (session in plugin.sessions.all()) {
            session.entities.entries.removeIf { it.value.uniqueId == entity.uniqueId }
        }
    }

    private fun isEmitter(entity: Entity): Boolean =
        entity.persistentDataContainer.has(plugin.keys.emitter, PersistentDataType.STRING)

    /**
     * ponytail: name -> UUID via the online list, then the server's known-players cache. Both are
     * local (no Mojang lookup, no main-thread stall); Bukkit's getOfflinePlayer(String) would block.
     * Cost is O(known players) but this is an admin command run by hand, not a hot path.
     */
    private fun resolveOwner(name: String): UUID? =
        plugin.server.getPlayerExact(name)?.uniqueId
            ?: plugin.server.offlinePlayers.firstOrNull { name.equals(it.name, ignoreCase = true) }?.uniqueId

    private fun ownerName(uuid: UUID): String =
        plugin.server.getPlayer(uuid)?.name
            ?: plugin.server.offlinePlayers.firstOrNull { it.uniqueId == uuid }?.name
            ?: uuid.toString()
}
