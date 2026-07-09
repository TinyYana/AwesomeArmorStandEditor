package com.tinyyana.awesomeArmorStandEditor

import com.tinyyana.awesomeArmorStandEditor.admin.PendingPurge
import com.tinyyana.awesomeArmorStandEditor.admin.parsePurgeArgs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * /aase admin purge irreversibly deletes other players' work, so the two guards in front of it —
 * argument parsing and the confirm-token expiry — are the bits that must not regress.
 */
class AdminLogicTest {

    @Test
    fun `parses radius and optional owner`() {
        val request = parsePurgeArgs(listOf("16"), maxRadius = 64)
        assertEquals(16, request?.radius)
        assertNull(request?.ownerName)

        val filtered = parsePurgeArgs(listOf("16", "Steve"), maxRadius = 64)
        assertEquals(16, filtered?.radius)
        assertEquals("Steve", filtered?.ownerName)
    }

    @Test
    fun `radius is clamped to the configured maximum`() {
        assertEquals(64, parsePurgeArgs(listOf("9999"), maxRadius = 64)?.radius)
    }

    /** A missing/garbage/zero/negative radius must yield null so the caller prints usage instead of purging. */
    @Test
    fun `rejects anything that is not a positive integer radius`() {
        assertNull(parsePurgeArgs(emptyList(), maxRadius = 64), "沒給半徑不能當成預設值就清")
        assertNull(parsePurgeArgs(listOf("abc"), maxRadius = 64))
        assertNull(parsePurgeArgs(listOf("0"), maxRadius = 64))
        assertNull(parsePurgeArgs(listOf("-5"), maxRadius = 64))
        assertNull(parsePurgeArgs(listOf("1.5"), maxRadius = 64))
    }

    @Test
    fun `blank owner name is treated as no filter`() {
        assertNull(parsePurgeArgs(listOf("16", "   "), maxRadius = 64)?.ownerName)
    }

    @Test
    fun `pending purge expires after its ttl`() {
        val created = 1_000_000L
        val plan = PendingPurge(center = UNUSED_LOCATION, radius = 8, ownerFilter = null, createdAtMillis = created)

        assertFalse(plan.isExpired(created), "剛建立不該過期")
        assertFalse(plan.isExpired(created + PendingPurge.TTL_MILLIS), "剛好在 TTL 邊界上仍有效")
        assertTrue(plan.isExpired(created + PendingPurge.TTL_MILLIS + 1), "超過 TTL 的確認必須失效")
    }

    private companion object {
        /** PendingPurge only stores the Location; expiry never touches it, so a null-ish stand-in is fine. */
        val UNUSED_LOCATION: org.bukkit.Location = org.bukkit.Location(null, 0.0, 0.0, 0.0)
    }
}
