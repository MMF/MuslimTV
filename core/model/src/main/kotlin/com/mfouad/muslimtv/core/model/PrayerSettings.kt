package com.mfouad.muslimtv.core.model

enum class CalculationMethod {
    UMM_AL_QURA,
    MUSLIM_WORLD_LEAGUE,
    EGYPTIAN,
    KARACHI,
    NORTH_AMERICA,
    DUBAI,
    KUWAIT,
    QATAR,
    SINGAPORE,
    TURKEY,
    MOON_SIGHTING_COMMITTEE,
}

enum class AsrMadhab { SHAFI, HANAFI }

enum class HighLatitudeOption { AUTO, MIDDLE_OF_THE_NIGHT, SEVENTH_OF_THE_NIGHT, TWILIGHT_ANGLE }

enum class Prayer { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA;

    /** Sunrise is shown in timetables but is not a prayer with an adhan. */
    val isObligatory: Boolean get() = this != SUNRISE
}

/** Per-prayer minute adjustments, applied after the calculation. */
data class PrayerAdjustments(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
) {
    operator fun get(prayer: Prayer): Int = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }

    fun with(prayer: Prayer, minutes: Int): PrayerAdjustments {
        val m = minutes.coerceIn(-MAX_ADJUSTMENT, MAX_ADJUSTMENT)
        return when (prayer) {
            Prayer.FAJR -> copy(fajr = m)
            Prayer.SUNRISE -> copy(sunrise = m)
            Prayer.DHUHR -> copy(dhuhr = m)
            Prayer.ASR -> copy(asr = m)
            Prayer.MAGHRIB -> copy(maghrib = m)
            Prayer.ISHA -> copy(isha = m)
        }
    }

    companion object {
        const val MAX_ADJUSTMENT = 30
    }
}

data class PrayerSettings(
    val method: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    val madhab: AsrMadhab = AsrMadhab.SHAFI,
    val highLatitude: HighLatitudeOption = HighLatitudeOption.AUTO,
    val adjustments: PrayerAdjustments = PrayerAdjustments(),
)

/** Recommended defaults by ISO country code. Users can always change them. */
object PrayerDefaults {
    fun methodFor(countryCode: String): CalculationMethod = when (countryCode.uppercase()) {
        "SA", "YE", "BH", "OM" -> CalculationMethod.UMM_AL_QURA
        "EG", "SD", "LY", "SY", "LB", "JO", "PS", "IQ", "DZ", "MA", "TN" -> CalculationMethod.EGYPTIAN
        "PK", "IN", "BD", "AF" -> CalculationMethod.KARACHI
        "US", "CA" -> CalculationMethod.NORTH_AMERICA
        "AE" -> CalculationMethod.DUBAI
        "KW" -> CalculationMethod.KUWAIT
        "QA" -> CalculationMethod.QATAR
        "SG", "MY", "ID", "BN" -> CalculationMethod.SINGAPORE
        "TR" -> CalculationMethod.TURKEY
        "GB", "IE" -> CalculationMethod.MOON_SIGHTING_COMMITTEE
        else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
    }

    fun madhabFor(countryCode: String): AsrMadhab = when (countryCode.uppercase()) {
        "PK", "IN", "BD", "AF", "TR" -> AsrMadhab.HANAFI
        else -> AsrMadhab.SHAFI
    }
}
