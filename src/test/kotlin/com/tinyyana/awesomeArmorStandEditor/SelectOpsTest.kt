package com.tinyyana.awesomeArmorStandEditor

import com.tinyyana.awesomeArmorStandEditor.edit.SelectOps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * /aase select is the only distance-free way to reach an element (displays have no hitbox,
 * /aase edit binds the nearest entity), so resolution must be exact: a wrong pick silently
 * edits the wrong element.
 */
class SelectOpsTest {

    // Scene order as stored, deliberately not sorted: load keeps authoring order.
    private val ids = listOf(1, 3, 4, 2, 5, 6, 7)

    @Test
    fun `selects by id, with or without the # the messages display`() {
        assertEquals(2, SelectOps.resolve(ids, current = 1, arg = "2"))
        assertEquals(2, SelectOps.resolve(ids, current = 1, arg = "#2"))
    }

    @Test
    fun `rejects unknown ids and garbage instead of guessing`() {
        assertNull(SelectOps.resolve(ids, current = 1, arg = "99"))
        assertNull(SelectOps.resolve(ids, current = 1, arg = "abc"))
        assertNull(SelectOps.resolve(emptyList(), current = null, arg = "next"))
    }

    @Test
    fun `next and prev cycle in scene order and wrap around`() {
        assertEquals(3, SelectOps.resolve(ids, current = 1, arg = "next"))
        assertEquals(1, SelectOps.resolve(ids, current = 3, arg = "prev"))
        assertEquals(1, SelectOps.resolve(ids, current = 7, arg = "next"), "尾端 next 要繞回第一個")
        assertEquals(7, SelectOps.resolve(ids, current = 1, arg = "prev"), "頭端 prev 要繞到最後一個")
    }

    @Test
    fun `nothing selected yet starts from the first element`() {
        assertEquals(1, SelectOps.resolve(ids, current = null, arg = "next"))
        assertEquals(1, SelectOps.resolve(ids, current = null, arg = "prev"))
    }
}
