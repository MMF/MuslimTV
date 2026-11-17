package com.mfouad.muslimtv.feature.home

import com.google.common.truth.Truth.assertThat
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.model.PrayerDay
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Test

class SurahRemindersTest {
    private val h = 3_600_000L
    private val day = PrayerDay(
        LocalDate.of(2026, 11, 20), "UTC",
        mapOf(Prayer.FAJR to 5 * h, Prayer.SUNRISE to 6 * h, Prayer.DHUHR to 12 * h, Prayer.ASR to 15 * h, Prayer.MAGHRIB to 18 * h, Prayer.ISHA to 19 * h),
    )

    @Test
    fun `al-kahf from thursday maghrib to friday maghrib`() {
        assertThat(SurahReminders.at(10 * h, day, DayOfWeek.FRIDAY, kahf = true, mulk = true)).isEqualTo(SurahReminder.KAHF)
        assertThat(SurahReminders.at(18 * h + 1, day, DayOfWeek.THURSDAY, kahf = true, mulk = true)).isEqualTo(SurahReminder.KAHF)
        assertThat(SurahReminders.at(10 * h, day, DayOfWeek.THURSDAY, kahf = true, mulk = true)).isNull()
    }

    @Test
    fun `al-mulk after isha, and switched-off reminders never show`() {
        assertThat(SurahReminders.at(20 * h, day, DayOfWeek.MONDAY, kahf = true, mulk = true)).isEqualTo(SurahReminder.MULK)
        assertThat(SurahReminders.at(20 * h, day, DayOfWeek.MONDAY, kahf = true, mulk = false)).isNull()
        assertThat(SurahReminders.at(10 * h, day, DayOfWeek.FRIDAY, kahf = false, mulk = true)).isNull()
    }
}
