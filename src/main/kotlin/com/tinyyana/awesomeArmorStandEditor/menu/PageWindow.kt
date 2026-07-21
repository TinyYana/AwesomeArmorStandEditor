package com.tinyyana.awesomeArmorStandEditor.menu

/**
 * Paged slice of a list, plus the window geometry the slice needs.
 *
 * This is a deliberate local copy of the layout contract the rest of the server keeps in
 * `LycoLib` (`SectionLayout` / `NavigationSlots` / `PageSlice`). AASE ships standalone — see
 * `settings.gradle.kts` and `integration/LycoLibHook.kt`: LycoLib is an optional *runtime*
 * integration reached by reflection, never a compile dependency, because the plugin has to build
 * and run on a plain Spigot server that has never heard of Lycohinya. Importing the real thing
 * would trade that away for ~30 lines. **The numbers must stay identical to
 * `docs/ux/UI_SLOT_MAPS.md`** — this file is the one place to keep them in step.
 *
 * The rules (UI_SLOT_MAPS §1 and §3.4):
 *
 * - Footer: `base = (rows - 1) * 9`, then `←0 ⌂1 ‹3 #4 ›5 ?7 ✕8`. `base + 4` is the inert page
 *   indicator on every paged screen, so nothing destructive may ever sit there.
 * - Content fills **whole rows from column 0**, left-aligned, no margins and no centering of the
 *   final partial row.
 * - A list that fits on one page **sizes the window to the data**:
 *   `rows = headerRow + ceil(count / 9) + footerRow`, clamped to 2..6. Three presets is a 3-row
 *   window, not a 6-row window with three icons floating in it.
 * - Only past [PAGE_CAPACITY] does it page, and then the window is pinned at 6 rows so it does
 *   not resize between pages.
 */
internal data class PageWindow<T>(
    val content: List<T>,
    val number: Int,
    val totalPages: Int,
    val totalItems: Int,
    /** Content slots for this window, in reading order. */
    val slots: List<Int>,
    /** Row 0: what this list is / the page's own actions. Empty when the window has no header row. */
    val headerSlots: List<Int>,
    val rows: Int,
) {
    val hasPrevious: Boolean get() = number > 1
    val hasNext: Boolean get() = number < totalPages
    val paged: Boolean get() = totalPages > 1
    val slotCount: Int get() = rows * COLUMNS

    /** Empty state goes in the first content slot, not the middle of the window. */
    val firstSlot: Int get() = slots.first()

    private val base: Int get() = (rows - 1) * COLUMNS
    val backSlot: Int get() = base
    val homeSlot: Int get() = base + 1
    val previousSlot: Int get() = base + 3
    val pageSlot: Int get() = base + 4
    val nextSlot: Int get() = base + 5
    val helpSlot: Int get() = base + 7
    val closeSlot: Int get() = base + 8

    companion object {
        const val COLUMNS = 9
        const val MAX_ROWS = 6

        /** Entries per page once a list has to page: rows 1..4 of a 6-row window. */
        const val PAGE_CAPACITY = 36

        fun <T> resolve(items: List<T>, requestedPage: Int, header: Boolean = false): PageWindow<T> {
            val rows: Int
            val headerSlots: List<Int>
            val capacity: Int
            if (items.size > PAGE_CAPACITY) {
                rows = MAX_ROWS
                headerSlots = (0 until COLUMNS).toList()
                capacity = PAGE_CAPACITY
            } else {
                val headerRows = if (header) 1 else 0
                val contentRows = ceilDiv(items.size, COLUMNS).coerceAtLeast(1)
                rows = (headerRows + contentRows + 1).coerceIn(2, MAX_ROWS)
                headerSlots = if (header) (0 until COLUMNS).toList() else emptyList()
                capacity = (rows - 1 - headerRows) * COLUMNS
            }
            val contentStart = if (headerSlots.isEmpty()) 0 else COLUMNS
            val slots = (contentStart until contentStart + capacity).toList()

            val totalPages = if (items.isEmpty()) 1 else ceilDiv(items.size, capacity)
            val page = requestedPage.coerceIn(1, totalPages)
            val from = (page - 1) * capacity
            val content = items.subList(from, minOf(from + capacity, items.size)).toList()
            return PageWindow(content, page, totalPages, items.size, slots, headerSlots, rows)
        }

        private fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b
    }
}
