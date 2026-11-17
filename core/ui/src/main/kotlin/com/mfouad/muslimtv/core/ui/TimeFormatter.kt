package com.mfouad.muslimtv.core.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.ConfigurationCompat
import com.mfouad.muslimtv.core.model.TimeFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats times, dates, numbers and countdowns in the UI language, so Arabic shows Arabic-Indic
 * digits and Arabic month and day names.
 */
@Immutable
class TimeFormatter(
    val locale: Locale,
    private val use24Hour: Boolean,
    private val am: String,
    private val pm: String,
) {
    private val numbers: NumberFormat = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
    private val gregorian = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale)
    private val gregorianShort = DateTimeFormatter.ofPattern("EEE d MMM", locale)

    fun number(n: Int): String = numbers.format(n.toLong())

    private fun two(n: Int): String = number(n).padStart(2, number(0).first())

    /** "5:42 PM" / "17:42" in the city's zone. */
    fun time(millis: Long, zone: ZoneId): String = timeParts(millis, zone).let { (t, suffix) -> if (suffix == null) t else "$t $suffix" }

    /** Time and AM/PM suffix separately, so the suffix can be drawn smaller. */
    fun timeParts(millis: Long, zone: ZoneId): Pair<String, String?> {
        val t = Instant.ofEpochMilli(millis).atZone(zone)
        return if (use24Hour) {
            "${two(t.hour)}:${two(t.minute)}" to null
        } else {
            val h = (t.hour % 12).let { if (it == 0) 12 else it }
            "${number(h)}:${two(t.minute)}" to (if (t.hour < 12) am else pm)
        }
    }

    /** Wall clock with seconds hidden, for headers and the ambient screen. */
    fun clock(millis: Long, zone: ZoneId): Pair<String, String?> = timeParts(millis, zone)

    /** "01:23:45", or "23:45" under an hour. */
    fun countdown(remainingMillis: Long): String {
        val total = (remainingMillis.coerceAtLeast(0) + 999) / 1000
        val h = (total / 3600).toInt()
        val m = ((total % 3600) / 60).toInt()
        val s = (total % 60).toInt()
        return if (h > 0) "${two(h)}:${two(m)}:${two(s)}" else "${two(m)}:${two(s)}"
    }

    fun gregorian(date: LocalDate): String = gregorian.format(date)

    fun gregorianShort(date: LocalDate): String = gregorianShort.format(date)
}

@Composable
fun rememberTimeFormatter(format: TimeFormat = TimeFormat.SYSTEM): TimeFormatter {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.ROOT
    val am = stringResource(R.string.time_am)
    val pm = stringResource(R.string.time_pm)
    return remember(locale, format, am, pm) {
        val use24 = when (format) {
            TimeFormat.SYSTEM -> DateFormat.is24HourFormat(context)
            TimeFormat.H12 -> false
            TimeFormat.H24 -> true
        }
        TimeFormatter(locale, use24, am, pm)
    }
}
