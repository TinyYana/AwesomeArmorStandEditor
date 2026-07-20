package com.tinyyana.awesomeArmorStandEditor.menu

internal data class PageWindow<T>(
    val content: List<T>,
    val number: Int,
    val totalPages: Int,
    val totalItems: Int,
) {
    val hasPrevious: Boolean get() = number > 1
    val hasNext: Boolean get() = number < totalPages

    companion object {
        fun <T> resolve(items: List<T>, requestedPage: Int, capacity: Int): PageWindow<T> {
            require(capacity > 0) { "capacity must be positive" }
            val pages = if (items.isEmpty()) 1 else (items.size - 1) / capacity + 1
            val page = requestedPage.coerceIn(1, pages)
            val from = (page - 1) * capacity
            return PageWindow(items.subList(from, minOf(from + capacity, items.size)).toList(), page, pages, items.size)
        }
    }
}

internal val LIST_CONTENT_SLOTS = listOf(
    10, 11, 12, 13, 14, 15, 16,
    19, 20, 21, 22, 23, 24, 25,
    28, 29, 30, 31, 32, 33, 34,
    37, 38, 39, 40, 41, 42, 43,
)

internal const val SLOT_BACK = 45
internal const val SLOT_HOME = 46
internal const val SLOT_PREVIOUS = 48
internal const val SLOT_PAGE = 49
internal const val SLOT_NEXT = 50
internal const val SLOT_HELP = 52
internal const val SLOT_CLOSE = 53
