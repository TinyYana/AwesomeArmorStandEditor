package com.tinyyana.awesomeArmorStandEditor.edit

/** Pure resolution of a `/aase select` argument. Server-independent, unit-tested. */
object SelectOps {

    /**
     * Resolves [arg] against the scene's element ids (in scene order) to the id to select,
     * or null if it names nothing. `next`/`prev` cycle from [current] and wrap around; an
     * unknown [current] (or none selected yet) starts at the first element. A number selects
     * that localId directly.
     */
    fun resolve(ids: List<Int>, current: Int?, arg: String): Int? {
        if (ids.isEmpty()) return null
        return when (arg.lowercase()) {
            "next", "prev" -> {
                val idx = ids.indexOf(current)
                if (idx < 0) ids.first()
                else ids[(idx + (if (arg.equals("next", true)) 1 else -1) + ids.size) % ids.size]
            }
            else -> arg.removePrefix("#").toIntOrNull()?.takeIf { it in ids }
        }
    }
}
