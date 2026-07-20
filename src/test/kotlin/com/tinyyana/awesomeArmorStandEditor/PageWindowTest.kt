package com.tinyyana.awesomeArmorStandEditor

import com.tinyyana.awesomeArmorStandEditor.menu.LIST_CONTENT_SLOTS
import com.tinyyana.awesomeArmorStandEditor.menu.PageWindow
import com.tinyyana.awesomeArmorStandEditor.menu.SLOT_BACK
import com.tinyyana.awesomeArmorStandEditor.menu.SLOT_CLOSE
import com.tinyyana.awesomeArmorStandEditor.menu.SLOT_HELP
import com.tinyyana.awesomeArmorStandEditor.menu.SLOT_NEXT
import com.tinyyana.awesomeArmorStandEditor.menu.SLOT_PAGE
import com.tinyyana.awesomeArmorStandEditor.menu.SLOT_PREVIOUS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageWindowTest {
    @Test
    fun `handles list boundary sizes`() {
        assertEquals(28, LIST_CONTENT_SLOTS.size)
        for (count in listOf(0, 1, 27, 28)) {
            val page = PageWindow.resolve((0 until count).toList(), 1, LIST_CONTENT_SLOTS.size)
            assertEquals(1, page.number)
            assertEquals(1, page.totalPages)
            assertFalse(page.hasNext)
        }

        val first = PageWindow.resolve((0..28).toList(), 1, LIST_CONTENT_SLOTS.size)
        assertEquals(28, first.content.size)
        assertTrue(first.hasNext)
        val last = PageWindow.resolve((0..28).toList(), 2, LIST_CONTENT_SLOTS.size)
        assertEquals(listOf(28), last.content)
        assertTrue(last.hasPrevious)
    }

    @Test
    fun `clamps after last page item disappears`() {
        val page = PageWindow.resolve((0 until 28).toList(), 2, LIST_CONTENT_SLOTS.size)
        assertEquals(1, page.number)
        assertEquals(28, page.content.size)
    }

    @Test
    fun `footer matches the shared list menu contract`() {
        assertEquals(45, SLOT_BACK)
        assertEquals(48, SLOT_PREVIOUS)
        assertEquals(49, SLOT_PAGE)
        assertEquals(50, SLOT_NEXT)
        assertEquals(52, SLOT_HELP)
        assertEquals(53, SLOT_CLOSE)
    }
}
