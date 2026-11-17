package com.mfouad.muslimtv.core.data

import com.google.common.truth.Truth.assertThat
import com.mfouad.muslimtv.core.data.prayer.PrayerTimesCalculator
import com.mfouad.muslimtv.core.model.AsrMadhab
import com.mfouad.muslimtv.core.model.CalculationMethod
import com.mfouad.muslimtv.core.model.City
import com.mfouad.muslimtv.core.model.HighLatitudeOption
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.model.PrayerAdjustments
import com.mfouad.muslimtv.core.model.PrayerSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Test

class PrayerTimesCalculatorTest {
    private val calc = PrayerTimesCalculator()
    private val makkah = City(104515, "Makkah", "مكة", "SA", 21.4266, 39.8256, "Asia/Riyadh")
    private val cairo = City(360630, "Cairo", "القاهرة", "EG", 30.0626, 31.2497, "Africa/Cairo")
    private val london = City(2643743, "London", "لندن", "GB", 51.5085, -0.1257, "Europe/London")
    private val date = LocalDate.of(2026, 3, 1)
    private val minute = 60_000L

    private fun local(city: City, millis: Long): LocalTime =
        Instant.ofEpochMilli(millis).atZone(ZoneId.of(city.timeZone)).toLocalTime()

    @Test
    fun `times are in prayer order and dhuhr is near local solar noon`() {
        val day = calc.calculate(makkah, date, PrayerSettings(CalculationMethod.UMM_AL_QURA))
        val ordered = Prayer.entries.map { day[it] }
        assertThat(ordered).isInStrictOrder()
        assertThat(local(makkah, day[Prayer.DHUHR])).isIn(
            com.google.common.collect.Range.closed(LocalTime.of(12, 15), LocalTime.of(12, 45)),
        )
    }

    @Test
    fun `umm al-qura isha is 90 minutes after maghrib, 120 in ramadan`() {
        val settings = PrayerSettings(CalculationMethod.UMM_AL_QURA)
        val normal = calc.calculate(makkah, date, settings, isRamadan = false)
        val ramadan = calc.calculate(makkah, date, settings, isRamadan = true)
        assertThat((normal[Prayer.ISHA] - normal[Prayer.MAGHRIB]) / minute).isEqualTo(90)
        assertThat((ramadan[Prayer.ISHA] - ramadan[Prayer.MAGHRIB]) / minute).isEqualTo(120)
    }

    @Test
    fun `hanafi asr is later than shafi asr`() {
        val shafi = calc.calculate(cairo, date, PrayerSettings(CalculationMethod.EGYPTIAN, AsrMadhab.SHAFI))
        val hanafi = calc.calculate(cairo, date, PrayerSettings(CalculationMethod.EGYPTIAN, AsrMadhab.HANAFI))
        assertThat(hanafi[Prayer.ASR]).isGreaterThan(shafi[Prayer.ASR] + 20 * minute)
        assertThat(hanafi[Prayer.DHUHR]).isEqualTo(shafi[Prayer.DHUHR])
    }

    @Test
    fun `a larger fajr angle gives an earlier fajr`() {
        val egyptian = calc.calculate(cairo, date, PrayerSettings(CalculationMethod.EGYPTIAN)) // 19.5°
        val isna = calc.calculate(cairo, date, PrayerSettings(CalculationMethod.NORTH_AMERICA)) // 15°
        assertThat(egyptian[Prayer.FAJR]).isLessThan(isna[Prayer.FAJR])
    }

    @Test
    fun `per-prayer adjustments shift only that prayer by whole minutes`() {
        val base = PrayerSettings(CalculationMethod.MUSLIM_WORLD_LEAGUE)
        val adjusted = base.copy(adjustments = PrayerAdjustments().with(Prayer.MAGHRIB, 3).with(Prayer.FAJR, -2))
        val a = calc.calculate(cairo, date, base)
        val b = calc.calculate(cairo, date, adjusted)
        assertThat(b[Prayer.MAGHRIB] - a[Prayer.MAGHRIB]).isEqualTo(3 * minute)
        assertThat(b[Prayer.FAJR] - a[Prayer.FAJR]).isEqualTo(-2 * minute)
        assertThat(b[Prayer.DHUHR]).isEqualTo(a[Prayer.DHUHR])
    }

    @Test
    fun `adjustments are clamped to 30 minutes`() {
        assertThat(PrayerAdjustments().with(Prayer.ISHA, 90).isha).isEqualTo(30)
        assertThat(PrayerAdjustments().with(Prayer.ISHA, -90).isha).isEqualTo(-30)
    }

    @Test
    fun `high latitude rule changes summer fajr in london and every prayer still exists`() {
        val summer = LocalDate.of(2026, 6, 21)
        val middle = calc.calculate(london, summer, PrayerSettings(CalculationMethod.MUSLIM_WORLD_LEAGUE, highLatitude = HighLatitudeOption.MIDDLE_OF_THE_NIGHT))
        val seventh = calc.calculate(london, summer, PrayerSettings(CalculationMethod.MUSLIM_WORLD_LEAGUE, highLatitude = HighLatitudeOption.SEVENTH_OF_THE_NIGHT))
        assertThat(Prayer.entries.map { seventh[it] }).isInStrictOrder()
        assertThat(middle[Prayer.FAJR]).isNotEqualTo(seventh[Prayer.FAJR])
    }

    @Test
    fun `next prayer skips sunrise and rolls over to tomorrow's fajr after isha`() {
        val s = PrayerSettings(CalculationMethod.UMM_AL_QURA)
        val today = calc.calculate(makkah, date, s)
        val tomorrow = calc.calculate(makkah, date.plusDays(1), s)

        val afterFajr = PrayerTimesCalculator.nextPrayer(today, tomorrow, today[Prayer.FAJR] + minute)
        assertThat(afterFajr.prayer).isEqualTo(Prayer.DHUHR)

        val afterIsha = PrayerTimesCalculator.nextPrayer(today, tomorrow, today[Prayer.ISHA] + minute)
        assertThat(afterIsha.prayer).isEqualTo(Prayer.FAJR)
        assertThat(afterIsha.isTomorrow).isTrue()
        assertThat(afterIsha.atMillis).isEqualTo(tomorrow[Prayer.FAJR])
    }
}
