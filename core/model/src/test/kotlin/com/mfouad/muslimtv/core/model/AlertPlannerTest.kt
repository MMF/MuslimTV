package com.mfouad.muslimtv.core.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class AlertPlannerTest {
    private val h = 3_600_000L
    private fun day(offset: Long) = PrayerDay(
        LocalDate.of(2026, 1, 1).plusDays(offset), "UTC",
        mapOf(
            Prayer.FAJR to offset * 24 * h + 5 * h, Prayer.SUNRISE to offset * 24 * h + 6 * h, Prayer.DHUHR to offset * 24 * h + 12 * h,
            Prayer.ASR to offset * 24 * h + 15 * h, Prayer.MAGHRIB to offset * 24 * h + 18 * h, Prayer.ISHA to offset * 24 * h + 19 * h,
        ),
    )
    private val today = day(0)
    private val tomorrow = day(1)

    @Test
    fun `next alert is the next obligatory prayer, never sunrise`() {
        val a = AlertPlanner.next(today, tomorrow, AdhanSettings(), nowMillis = 5 * h + 1)!!
        assertThat(a.prayer).isEqualTo(Prayer.DHUHR)
        assertThat(a.mode).isEqualTo(AlertMode.ADHAN)
    }

    @Test
    fun `prayers switched off are skipped, including into tomorrow`() {
        val settings = AdhanSettings(modes = mapOf(Prayer.FAJR to AlertMode.NOTIFICATION, Prayer.DHUHR to AlertMode.OFF, Prayer.ASR to AlertMode.OFF, Prayer.MAGHRIB to AlertMode.OFF, Prayer.ISHA to AlertMode.OFF))
        val a = AlertPlanner.next(today, tomorrow, settings, nowMillis = 6 * h)!!
        assertThat(a.prayer).isEqualTo(Prayer.FAJR)
        assertThat(a.atMillis).isEqualTo(tomorrow[Prayer.FAJR])
        assertThat(a.mode).isEqualTo(AlertMode.NOTIFICATION)
    }

    @Test
    fun `nothing is scheduled when every prayer is off`() {
        val off = AdhanSettings(modes = Prayer.entries.associateWith { AlertMode.OFF })
        assertThat(AlertPlanner.next(today, tomorrow, off, 0)).isNull()
    }

    @Test
    fun `after isha the next alert is tomorrow's fajr`() {
        val a = AlertPlanner.next(today, tomorrow, AdhanSettings(), nowMillis = 19 * h)!!
        assertThat(a.prayer to a.atMillis).isEqualTo(Prayer.FAJR to tomorrow[Prayer.FAJR])
    }
}
