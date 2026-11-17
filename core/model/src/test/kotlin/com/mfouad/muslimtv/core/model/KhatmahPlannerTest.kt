package com.mfouad.muslimtv.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KhatmahPlannerTest {
    private val pages = (1..604).map { AyahRef(1 + it / 6, 1 + it % 6) } // stand-in page starts

    @Test
    fun `30 days covers all 604 pages without gaps or overlaps`() {
        val portions = (1..30).map { KhatmahPlanner.portion(it, 30, pages) }
        assertThat(portions.first().pages.first).isEqualTo(1)
        assertThat(portions.last().pages.last).isEqualTo(604)
        portions.zipWithNext().forEach { (a, b) -> assertThat(b.pages.first).isEqualTo(a.pages.last + 1) }
        assertThat(portions.map { it.pages.count() }.toSet()).containsExactly(20, 21)
        assertThat(portions.last().endExclusive).isNull()
    }

    @Test
    fun `custom number of days`() {
        val p = (1..7).map { KhatmahPlanner.portion(it, 7, pages) }
        assertThat(p.sumOf { it.pages.count() }).isEqualTo(604)
        assertThat(p[1].start).isEqualTo(pages[p[0].pages.last])
    }

    @Test
    fun `progress and how far behind`() {
        val plan = KhatmahPlan(days = 30, startEpochDay = 100, completed = 3)
        assertThat(plan.behind(todayEpochDay = 104)).isEqualTo(2) // day 5, 3 done
        assertThat(plan.behind(todayEpochDay = 101)).isEqualTo(0)
        assertThat(plan.progress).isWithin(0.001f).of(0.1f)
    }
}
