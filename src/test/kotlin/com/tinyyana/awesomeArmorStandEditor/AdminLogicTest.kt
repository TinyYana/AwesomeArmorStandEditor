package com.tinyyana.awesomeArmorStandEditor

import com.tinyyana.awesomeArmorStandEditor.admin.PendingPurge
import com.tinyyana.awesomeArmorStandEditor.admin.parsePurgeArgs
import com.tinyyana.awesomeArmorStandEditor.admin.selectClearable
import java.util.UUID
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

    @Test
    fun clearTakesOnlyOthersElementsOnGroundYouMayBuildOn() {
        val me = UUID.randomUUID()
        val other = UUID.randomUUID()
        // (owner, may-I-build-there)
        val myArt = Candidate(me, buildable = true)
        val intruderInMyClaim = Candidate(other, buildable = true)
        val theirArtOnTheirLand = Candidate(other, buildable = false)
        val untagged = Candidate(null, buildable = true)

        val cleared = selectClearable(
            candidates = listOf(myArt, intruderInMyClaim, theirArtOnTheirLand, untagged),
            self = me,
            ownerOf = { it.owner },
            canBuildAt = { it.buildable },
        )

        assertEquals(listOf(intruderInMyClaim), cleared, "只該清掉別人放在你有建築權之處的元件")
        assertFalse(myArt in cleared, "絕不能清掉自己的作品")
        assertFalse(theirArtOnTheirLand in cleared, "絕不能清掉你沒有建築權的地方的東西")
        assertFalse(untagged in cleared, "沒有擁有者標記的東西不歸這裡管")
    }

    private data class Candidate(val owner: UUID?, val buildable: Boolean)

    private companion object {
        /** PendingPurge only stores the Location; expiry never touches it, so a null-ish stand-in is fine. */
        val UNUSED_LOCATION: org.bukkit.Location = org.bukkit.Location(null, 0.0, 0.0, 0.0)
    }
}
