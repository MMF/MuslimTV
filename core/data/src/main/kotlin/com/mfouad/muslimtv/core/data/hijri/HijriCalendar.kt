package com.mfouad.muslimtv.core.data.hijri

import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import android.os.Build
import androidx.annotation.RequiresApi
import com.mfouad.muslimtv.core.model.HijriDate
import java.time.LocalDate
import javax.inject.Inject

/** Converts a Gregorian date to Hijri. Offsets for local moon sighting are applied by the caller. */
interface HijriCalendar {
    fun toHijri(date: LocalDate): HijriDate
}

/**
 * Umm al-Qura via the platform ICU calendar on API 24+. API 23 has no Umm al-Qura data,
 * so it falls back to the arithmetic (tabular) calendar, which can differ by a day; the
 * user's Hijri adjustment setting covers that.
 */
class PlatformHijriCalendar @Inject constructor() : HijriCalendar {
    override fun toHijri(date: LocalDate): HijriDate =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) UmmAlQura.toHijri(date) else TabularHijri.toHijri(date)

    @RequiresApi(Build.VERSION_CODES.N)
    private object UmmAlQura {
        fun toHijri(date: LocalDate): HijriDate {
            val cal = IslamicCalendar(TimeZone.GMT_ZONE, ULocale.ROOT)
            cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            // Noon UTC on that civil date avoids any day-boundary ambiguity.
            cal.timeInMillis = date.toEpochDay() * MILLIS_PER_DAY + MILLIS_PER_DAY / 2
            return HijriDate(
                year = cal.get(IslamicCalendar.EXTENDED_YEAR),
                month = cal.get(IslamicCalendar.MONTH) + 1,
                day = cal.get(IslamicCalendar.DAY_OF_MONTH),
            )
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

/** The arithmetic Islamic civil calendar (the "Kuwaiti algorithm"). Pure, so it is unit tested. */
object TabularHijri {
    private const val JULIAN_DAY_OF_UNIX_EPOCH = 2_440_588L

    fun toHijri(date: LocalDate): HijriDate {
        val jdn = date.toEpochDay() + JULIAN_DAY_OF_UNIX_EPOCH
        var l = jdn - 1_948_440L + 10_632L
        val n = (l - 1) / 10_631L
        l = l - 10_631L * n + 354L
        val j = ((10_985L - l) / 5_316L) * ((50L * l) / 17_719L) + (l / 5_670L) * ((43L * l) / 15_238L)
        l = l - ((30L - j) / 15L) * ((17_719L * j) / 50L) - (j / 16L) * ((15_238L * j) / 43L) + 29L
        val month = (24L * l) / 709L
        val day = l - (709L * month) / 24L
        val year = 30L * n + j - 30L
        return HijriDate(year.toInt(), month.toInt(), day.toInt())
    }
}
