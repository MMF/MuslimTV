package com.mfouad.muslimtv.core.data

import com.google.common.truth.Truth.assertThat
import com.mfouad.muslimtv.core.data.azkar.AzkarSuggester
import com.mfouad.muslimtv.core.data.hijri.TabularHijri
import com.mfouad.muslimtv.core.model.AzkarCategory
import com.mfouad.muslimtv.core.model.HijriDate
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.model.PrayerDay
import java.time.LocalDate
import org.junit.Test

class HijriAndAzkarTest {
    @Test
    fun `tabular hijri epoch is 1 muharram 1 AH`() {
        // 16 July 622 (Julian) is 19 July 622 in the proleptic Gregorian calendar.
        assertThat(TabularHijri.toHijri(LocalDate.of(622, 7, 19))).isEqualTo(HijriDate(1, 1, 1))
    }

    @Test
    fun `tabular hijri is within one day of known umm al-qura dates`() {
        // Umm al-Qura: 1 Ramadan 1446 = 1 March 2025, 1 Muharram 1447 = 26 June 2025.
        val ramadan = listOf(0L, -1L, 1L).map { TabularHijri.toHijri(LocalDate.of(2025, 3, 1).plusDays(it)) }
        assertThat(ramadan).contains(HijriDate(1446, 9, 1))
        val muharram = listOf(0L, -1L, 1L).map { TabularHijri.toHijri(LocalDate.of(2025, 6, 26).plusDays(it)) }
        assertThat(muharram).contains(HijriDate(1447, 1, 1))
    }

    @Test
    fun `tabular hijri days advance continuously`() {
        var prev = TabularHijri.toHijri(LocalDate.of(2026, 1, 1))
        var date = LocalDate.of(2026, 1, 2)
        repeat(800) {
            val cur = TabularHijri.toHijri(date)
            val sameMonth = cur.year == prev.year && cur.month == prev.month && cur.day == prev.day + 1
            val newMonth = cur.day == 1 && prev.day in 29..30
            assertThat(sameMonth || newMonth).isTrue()
            prev = cur
            date = date.plusDays(1)
        }
    }

    private val hour = 3_600_000L
    private val day = PrayerDay(
        LocalDate.of(2026, 3, 1), "UTC",
        mapOf(
            Prayer.FAJR to 5 * hour, Prayer.SUNRISE to 6 * hour + hour / 2, Prayer.DHUHR to 12 * hour,
            Prayer.ASR to 15 * hour + hour / 2, Prayer.MAGHRIB to 18 * hour, Prayer.ISHA to 19 * hour + hour / 2,
        ),
    )

    @Test
    fun `azkar suggestion follows the time of day`() {
        assertThat(AzkarSuggester.suggest(3 * hour, day)).isEqualTo(AzkarCategory.SLEEP)
        assertThat(AzkarSuggester.suggest(5 * hour + 10 * 60_000, day)).isEqualTo(AzkarCategory.AFTER_PRAYER)
        assertThat(AzkarSuggester.suggest(8 * hour, day)).isEqualTo(AzkarCategory.MORNING)
        assertThat(AzkarSuggester.suggest(16 * hour + hour / 2, day)).isEqualTo(AzkarCategory.EVENING)
        assertThat(AzkarSuggester.suggest(18 * hour + 5 * 60_000, day)).isEqualTo(AzkarCategory.AFTER_PRAYER)
        assertThat(AzkarSuggester.suggest(22 * hour, day)).isEqualTo(AzkarCategory.SLEEP)
    }
}
