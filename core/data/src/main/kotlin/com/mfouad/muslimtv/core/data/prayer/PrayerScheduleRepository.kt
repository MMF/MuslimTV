package com.mfouad.muslimtv.core.data.prayer

import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.hijri.HijriCalendar
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.model.City
import com.mfouad.muslimtv.core.model.HijriDate
import com.mfouad.muslimtv.core.model.PrayerDay
import com.mfouad.muslimtv.core.model.UserSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/** Today's and tomorrow's prayer times plus the Hijri date for the chosen city. */
data class PrayerSchedule(
    val city: City,
    val zone: ZoneId,
    val date: LocalDate,
    val hijri: HijriDate,
    val today: PrayerDay,
    val tomorrow: PrayerDay,
    val settings: UserSettings,
)

@Singleton
class PrayerScheduleRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val calculator: PrayerTimesCalculator,
    private val hijriCalendar: HijriCalendar,
    private val time: TimeProvider,
) {
    /** Recalculates only when settings change or the city's date rolls over, not every second. */
    val schedule: Flow<PrayerSchedule> = combine(
        settingsRepository.settings,
        time.ticker(MINUTE),
    ) { settings, now -> settings to now }
        .map { (settings, now) ->
            val city = settings.city ?: return@map null
            val zone = zoneOf(city)
            Triple(settings, city, Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
        }
        .filterNotNull()
        .distinctUntilChanged()
        .map { (settings, city, date) -> build(settings, city, date) }

    fun build(settings: UserSettings, city: City, date: LocalDate): PrayerSchedule {
        val hijri = hijriFor(date, settings.hijriOffset)
        val hijriTomorrow = hijriFor(date.plusDays(1), settings.hijriOffset)
        return PrayerSchedule(
            city = city,
            zone = zoneOf(city),
            date = date,
            hijri = hijri,
            today = calculator.calculate(city, date, settings.prayer, hijri.isRamadan),
            tomorrow = calculator.calculate(city, date.plusDays(1), settings.prayer, hijriTomorrow.isRamadan),
            settings = settings,
        )
    }

    fun hijriFor(date: LocalDate, offset: Int): HijriDate = hijriCalendar.toHijri(date.plusDays(offset.toLong()))

    private fun zoneOf(city: City): ZoneId = runCatching { ZoneId.of(city.timeZone) }.getOrDefault(ZoneId.systemDefault())

    private companion object {
        const val MINUTE = 60_000L
    }
}
