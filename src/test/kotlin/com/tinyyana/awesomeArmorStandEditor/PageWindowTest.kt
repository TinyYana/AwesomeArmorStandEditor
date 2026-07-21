package com.tinyyana.awesomeArmorStandEditor

import com.tinyyana.awesomeArmorStandEditor.menu.PageWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageWindowTest {

    @Test
    fun `handles list boundary sizes`() {
        for (count in listOf(0, 1, 35, 36)) {
            val page = PageWindow.resolve((0 until count).toList(), 1, header = true)
            assertEquals(1, page.number)
            assertEquals(1, page.totalPages)
            assertFalse(page.hasNext)
            assertFalse(page.paged)
            assertEquals(count, page.content.size)
        }

        val first = PageWindow.resolve((0..36).toList(), 1, header = true)
        assertEquals(36, first.content.size)
        assertTrue(first.hasNext)
        val last = PageWindow.resolve((0..36).toList(), 2, header = true)
        assertEquals(listOf(36), last.content)
        assertTrue(last.hasPrevious)
    }

    @Test
    fun `clamps after last page item disappears`() {
        val page = PageWindow.resolve((0 until 36).toList(), 2, header = true)
        assertEquals(1, page.number)
        assertEquals(36, page.content.size)
    }

    @Test
    fun `window shrinks to the data instead of always opening six rows`() {
        // Three presets is a three-row window (header + one content row + footer), not 54 slots.
        assertEquals(3, PageWindow.resolve((0 until 3).toList(), 1, header = true).rows)
        assertEquals(27, PageWindow.resolve((0 until 3).toList(), 1, header = true).slotCount)
        assertEquals(2, PageWindow.resolve((0 until 3).toList(), 1).rows)
        assertEquals(4, PageWindow.resolve((0 until 10).toList(), 1, header = true).rows)
        // Only real paging pins it at six rows, so the window cannot resize between pages.
        assertEquals(6, PageWindow.resolve((0 until 37).toList(), 1, header = true).rows)
        assertEquals(6, PageWindow.resolve((0 until 37).toList(), 2, header = true).rows)
    }

    @Test
    fun `content fills whole rows from column zero`() {
        val window = PageWindow.resolve((0 until 20).toList(), 1, header = true)
        assertEquals((0..8).toList(), window.headerSlots)
        assertEquals((9 until 36).toList(), window.slots)
        assertEquals(9, window.firstSlot)

        val paged = PageWindow.resolve((0 until 73).toList(), 1, header = true)
        assertEquals((9..44).toList(), paged.slots)
        assertTrue(paged.slots.none { it >= paged.backSlot })
    }

    @Test
    fun `footer matches the shared chest UI contract at every window size`() {
        for (count in listOf(0, 3, 20, 73)) {
            val window = PageWindow.resolve((0 until count).toList(), 1, header = true)
            val base = (window.rows - 1) * 9
            assertEquals(base, window.backSlot)
            assertEquals(base + 1, window.homeSlot)
            assertEquals(base + 3, window.previousSlot)
            assertEquals(base + 4, window.pageSlot)
            assertEquals(base + 5, window.nextSlot)
            assertEquals(base + 7, window.helpSlot)
            assertEquals(base + 8, window.closeSlot)
        }

        // The six-row numbers everyone's muscle memory is built on
        val large = PageWindow.resolve((0 until 73).toList(), 1, header = true)
        assertEquals(45, large.backSlot)
        assertEquals(49, large.pageSlot)
        assertEquals(53, large.closeSlot)
    }
}
