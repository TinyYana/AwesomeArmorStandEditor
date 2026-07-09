package com.tinyyana.awesomeArmorStandEditor.admin

import org.bukkit.Location
import java.util.UUID

/**
 * Pure pieces of the purge flow, kept out of [AdminTools] so they can be unit-tested without a
 * server: argument parsing (a bad radius must never reach a world query) and confirm-token expiry.
 */
internal data class PurgeRequest(val radius: Int, val ownerName: String?)

/**
 * Parses `/aase admin purge <radius> [owner]`.
 * Returns null for anything that isn't a positive integer radius — the caller then prints usage
 * rather than silently purging with a default. Radius is clamped to the configured maximum.
 */
internal fun parsePurgeArgs(args: List<String>, maxRadius: Int): PurgeRequest? {
    val radius = args.getOrNull(0)?.toIntOrNull() ?: return null
    if (radius <= 0) return null
    return PurgeRequest(
        radius = radius.coerceAtMost(maxRadius),
        ownerName = args.getOrNull(1)?.takeIf { it.isNotBlank() },
    )
}

/**
 * Which elements a builder may clear with `/aase clear`: someone *else's*, standing somewhere the
 * builder is allowed to build. Pure so the rule survives refactors — getting either half wrong turns
 * a cleanup tool into a griefing tool (drop the owner test and you nuke your own art; drop the build
 * test and you nuke anyone's).
 */
internal fun <T> selectClearable(
    candidates: List<T>,
    self: UUID,
    ownerOf: (T) -> UUID?,
    canBuildAt: (T) -> Boolean,
): List<T> = candidates.filter { candidate ->
    val owner = ownerOf(candidate)
    owner != null && owner != self && canBuildAt(candidate)
}

/** A previewed purge awaiting `/aase admin confirm`. Expires so a stale token can't fire later. */
internal data class PendingPurge(
    val center: Location,
    val radius: Int,
    val ownerFilter: UUID?,
    val createdAtMillis: Long,
) {
    fun isExpired(nowMillis: Long): Boolean = nowMillis - createdAtMillis > TTL_MILLIS

    companion object {
        const val TTL_MILLIS: Long = 60_000
    }
}
