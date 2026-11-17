package com.mfouad.muslimtv.core.model

enum class TimeFormat { SYSTEM, H12, H24 }

enum class RamadanMode { AUTO, ON, OFF }

enum class AmbientSound { OFF, RAIN, STREAM, BIRDS }

enum class AmbientVolume(val level: Float) { LOW(0.08f), MEDIUM(0.15f) }

data class UserSettings(
    val onboardingDone: Boolean = false,
    val city: City? = null,
    val prayer: PrayerSettings = PrayerSettings(),
    /** Days added to the calculated Hijri date, -2..2, to match local moon sighting. */
    val hijriOffset: Int = 0,
    val quranFontSizeSp: Int = DEFAULT_QURAN_FONT_SP,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    val reciterId: String = DEFAULT_RECITER,
    val showTranslation: Boolean = false,
    /** Source id of the translation shown under ayahs, e.g. "en.pickthall". */
    val translationId: String = DEFAULT_TRANSLATION,
    val playbackSpeed: Float = 1f,
    /** Nature scene index, or -1 to rotate through all scenes. */
    val natureScene: Int = -1,
    val ambientSound: AmbientSound = AmbientSound.OFF,
    val ambientVolume: AmbientVolume = AmbientVolume.LOW,
    /** Minutes without a key press before the ambient screen; 0 = never. */
    val ambientTimeoutMin: Int = 10,
    val ramadanMode: RamadanMode = RamadanMode.AUTO,
    val kahfReminder: Boolean = true,
    val mulkReminder: Boolean = true,
) {
    companion object {
        const val DEFAULT_QURAN_FONT_SP = 48
        const val MIN_QURAN_FONT_SP = 36
        const val MAX_QURAN_FONT_SP = 72
        const val QURAN_FONT_STEP_SP = 4
        const val MAX_HIJRI_OFFSET = 2
        const val DEFAULT_RECITER = "alafasy"
        const val DEFAULT_TRANSLATION = "en.pickthall"
    }
}
