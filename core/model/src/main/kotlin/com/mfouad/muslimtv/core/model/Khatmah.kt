package com.mfouad.muslimtv.core.model

data class Profile(val id: Long, val name: String)

/** One day's reading in a khatmah plan, from [start] up to (not including) [endExclusive]. */
data class KhatmahPortion(val day: Int, val start: AyahRef, val endExclusive: AyahRef?, val pages: IntRange)

data class KhatmahPlan(val days: Int, val startEpochDay: Long, val completed: Int) {
    val finished: Boolean get() = completed >= days
    val progress: Float get() = (completed.toFloat() / days).coerceIn(0f, 1f)

    /** Portions behind schedule today (0 when on track). */
    fun behind(todayEpochDay: Long): Int = ((todayEpochDay - startEpochDay + 1).toInt().coerceAtMost(days) - completed).coerceAtLeast(0)
}

/**
 * Splits the 604 pages into [days] nearly equal portions by page, so each day ends on a page
 * boundary. Pure, so it is unit tested.
 */
object KhatmahPlanner {
    const val PAGES = 604

    /** [pageStarts] holds the first ayah of each page (index 0 = page 1). */
    fun portion(day: Int, days: Int, pageStarts: List<AyahRef>): KhatmahPortion {
        require(days in 1..PAGES && day in 1..days)
        val first = pagesBefore(day - 1, days) + 1
        val last = pagesBefore(day, days)
        return KhatmahPortion(day, pageStarts[first - 1], pageStarts.getOrNull(last), first..last)
    }

    /** Pages covered by the first [n] portions, spreading the remainder over the first days. */
    private fun pagesBefore(n: Int, days: Int): Int {
        val base = PAGES / days
        val extra = PAGES % days
        return n * base + minOf(n, extra)
    }
}
