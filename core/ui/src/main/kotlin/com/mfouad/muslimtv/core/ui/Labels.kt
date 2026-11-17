package com.mfouad.muslimtv.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.mfouad.muslimtv.core.model.AsrMadhab
import com.mfouad.muslimtv.core.model.AzkarCategory
import com.mfouad.muslimtv.core.model.CalculationMethod
import com.mfouad.muslimtv.core.model.HighLatitudeOption
import com.mfouad.muslimtv.core.model.HijriDate
import com.mfouad.muslimtv.core.model.Prayer
import java.time.DayOfWeek
import java.time.LocalDate

@StringRes
fun Prayer.labelRes(date: LocalDate? = null): Int = when (this) {
    Prayer.FAJR -> R.string.prayer_fajr
    Prayer.SUNRISE -> R.string.prayer_sunrise
    Prayer.DHUHR -> if (date?.dayOfWeek == DayOfWeek.FRIDAY) R.string.prayer_jumuah else R.string.prayer_dhuhr
    Prayer.ASR -> R.string.prayer_asr
    Prayer.MAGHRIB -> R.string.prayer_maghrib
    Prayer.ISHA -> R.string.prayer_isha
}

@Composable
fun Prayer.label(date: LocalDate? = null): String = stringResource(labelRes(date))

@StringRes
fun CalculationMethod.labelRes(): Int = when (this) {
    CalculationMethod.UMM_AL_QURA -> R.string.method_umm_al_qura
    CalculationMethod.MUSLIM_WORLD_LEAGUE -> R.string.method_mwl
    CalculationMethod.EGYPTIAN -> R.string.method_egyptian
    CalculationMethod.KARACHI -> R.string.method_karachi
    CalculationMethod.NORTH_AMERICA -> R.string.method_isna
    CalculationMethod.DUBAI -> R.string.method_dubai
    CalculationMethod.KUWAIT -> R.string.method_kuwait
    CalculationMethod.QATAR -> R.string.method_qatar
    CalculationMethod.SINGAPORE -> R.string.method_singapore
    CalculationMethod.TURKEY -> R.string.method_turkey
    CalculationMethod.MOON_SIGHTING_COMMITTEE -> R.string.method_moonsighting
}

@StringRes
fun AsrMadhab.labelRes(): Int = when (this) {
    AsrMadhab.SHAFI -> R.string.madhab_shafi
    AsrMadhab.HANAFI -> R.string.madhab_hanafi
}

@StringRes
fun HighLatitudeOption.labelRes(): Int = when (this) {
    HighLatitudeOption.AUTO -> R.string.highlat_auto
    HighLatitudeOption.MIDDLE_OF_THE_NIGHT -> R.string.highlat_middle
    HighLatitudeOption.SEVENTH_OF_THE_NIGHT -> R.string.highlat_seventh
    HighLatitudeOption.TWILIGHT_ANGLE -> R.string.highlat_twilight
}

@StringRes
fun AzkarCategory.labelRes(): Int = when (this) {
    AzkarCategory.MORNING -> R.string.azkar_morning
    AzkarCategory.EVENING -> R.string.azkar_evening
    AzkarCategory.AFTER_PRAYER -> R.string.azkar_after_prayer
    AzkarCategory.SLEEP -> R.string.azkar_sleep
    AzkarCategory.WAKING -> R.string.azkar_waking
    AzkarCategory.OCCASIONS -> R.string.azkar_occasions
    AzkarCategory.IFTAR -> R.string.azkar_iftar
}

@Composable
fun HijriDate.format(): String {
    val months = stringArrayResource(R.array.hijri_months)
    val f = rememberTimeFormatter()
    return stringResource(R.string.hijri_date_format, f.number(day), months[month - 1], f.number(year))
}
