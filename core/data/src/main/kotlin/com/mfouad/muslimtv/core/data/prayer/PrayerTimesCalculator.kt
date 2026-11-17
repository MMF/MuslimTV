package com.mfouad.muslimtv.core.data.prayer

import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.mfouad.muslimtv.core.model.AsrMadhab
import com.mfouad.muslimtv.core.model.CalculationMethod
import com.mfouad.muslimtv.core.model.City
import com.mfouad.muslimtv.core.model.HighLatitudeOption
import com.mfouad.muslimtv.core.model.NextPrayer
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.model.PrayerDay
import com.mfouad.muslimtv.core.model.PrayerSettings
import java.time.LocalDate
import javax.inject.Inject
import com.batoulapps.adhan2.CalculationMethod as AdhanMethod
import com.batoulapps.adhan2.PrayerAdjustments as AdhanAdjustments

/**
 * Calculates prayer times on the device with the Adhan library (no network).
 * All results are absolute instants; display them in the city's time zone.
 */
class PrayerTimesCalculator @Inject constructor() {

    fun calculate(
        city: City,
        date: LocalDate,
        settings: PrayerSettings,
        isRamadan: Boolean = false,
    ): PrayerDay {
        val coordinates = Coordinates(city.latitude, city.longitude)
        val base = settings.method.toAdhan().parameters
        val adj = settings.adjustments
        var params = base.copy(
            madhab = if (settings.madhab == AsrMadhab.HANAFI) Madhab.HANAFI else Madhab.SHAFI,
            highLatitudeRule = settings.highLatitude.toAdhan(),
            prayerAdjustments = AdhanAdjustments(
                fajr = adj.fajr,
                sunrise = adj.sunrise,
                dhuhr = adj.dhuhr,
                asr = adj.asr,
                maghrib = adj.maghrib,
                isha = adj.isha,
            ),
        )
        // Umm al-Qura: Isha is 120 minutes after Maghrib during Ramadan instead of 90.
        if (settings.method == CalculationMethod.UMM_AL_QURA && isRamadan) {
            params = params.copy(ishaInterval = UMM_AL_QURA_RAMADAN_ISHA_MINUTES)
        }
        val times = PrayerTimes(coordinates, DateComponents(date.year, date.monthValue, date.dayOfMonth), params)
        return PrayerDay(
            date = date,
            timeZone = city.timeZone,
            times = mapOf(
                Prayer.FAJR to times.fajr.toEpochMilliseconds(),
                Prayer.SUNRISE to times.sunrise.toEpochMilliseconds(),
                Prayer.DHUHR to times.dhuhr.toEpochMilliseconds(),
                Prayer.ASR to times.asr.toEpochMilliseconds(),
                Prayer.MAGHRIB to times.maghrib.toEpochMilliseconds(),
                Prayer.ISHA to times.isha.toEpochMilliseconds(),
            ),
        )
    }

    companion object {
        const val UMM_AL_QURA_RAMADAN_ISHA_MINUTES = 120

        /** The next obligatory prayer after [nowMillis]; sunrise is skipped. */
        fun nextPrayer(today: PrayerDay, tomorrow: PrayerDay, nowMillis: Long): NextPrayer {
            Prayer.entries.filter { it.isObligatory }.forEach { prayer ->
                val at = today[prayer]
                if (at > nowMillis) return NextPrayer(prayer, at, isTomorrow = false)
            }
            return NextPrayer(Prayer.FAJR, tomorrow[Prayer.FAJR], isTomorrow = true)
        }

        /** The most recent obligatory prayer at or before [nowMillis] today, if any. */
        fun currentPrayer(today: PrayerDay, nowMillis: Long): Prayer? =
            Prayer.entries.filter { it.isObligatory }.lastOrNull { today[it] <= nowMillis }
    }
}

internal fun CalculationMethod.toAdhan(): AdhanMethod = when (this) {
    CalculationMethod.UMM_AL_QURA -> AdhanMethod.UMM_AL_QURA
    CalculationMethod.MUSLIM_WORLD_LEAGUE -> AdhanMethod.MUSLIM_WORLD_LEAGUE
    CalculationMethod.EGYPTIAN -> AdhanMethod.EGYPTIAN
    CalculationMethod.KARACHI -> AdhanMethod.KARACHI
    CalculationMethod.NORTH_AMERICA -> AdhanMethod.NORTH_AMERICA
    CalculationMethod.DUBAI -> AdhanMethod.DUBAI
    CalculationMethod.KUWAIT -> AdhanMethod.KUWAIT
    CalculationMethod.QATAR -> AdhanMethod.QATAR
    CalculationMethod.SINGAPORE -> AdhanMethod.SINGAPORE
    CalculationMethod.TURKEY -> AdhanMethod.TURKEY
    CalculationMethod.MOON_SIGHTING_COMMITTEE -> AdhanMethod.MOON_SIGHTING_COMMITTEE
}

/** AUTO lets Adhan choose: seventh of the night above 48° latitude, middle of the night below. */
internal fun HighLatitudeOption.toAdhan(): HighLatitudeRule? = when (this) {
    HighLatitudeOption.AUTO -> null
    HighLatitudeOption.MIDDLE_OF_THE_NIGHT -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
    HighLatitudeOption.SEVENTH_OF_THE_NIGHT -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
    HighLatitudeOption.TWILIGHT_ANGLE -> HighLatitudeRule.TWILIGHT_ANGLE
}
