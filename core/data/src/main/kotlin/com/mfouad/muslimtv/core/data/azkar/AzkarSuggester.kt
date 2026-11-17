package com.mfouad.muslimtv.core.data.azkar

import com.mfouad.muslimtv.core.model.AzkarCategory
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.model.PrayerDay

/** Picks the azkar that fit the current moment of the day. Pure, so it is unit tested. */
object AzkarSuggester {
    const val AFTER_PRAYER_WINDOW_MILLIS = 20 * 60_000L

    fun suggest(nowMillis: Long, today: PrayerDay): AzkarCategory {
        val justPrayed = Prayer.entries
            .filter { it.isObligatory }
            .any { nowMillis - today[it] in 0 until AFTER_PRAYER_WINDOW_MILLIS }
        return when {
            justPrayed -> AzkarCategory.AFTER_PRAYER
            nowMillis < today[Prayer.FAJR] -> AzkarCategory.SLEEP
            nowMillis < today[Prayer.DHUHR] -> AzkarCategory.MORNING
            nowMillis < today[Prayer.ASR] -> AzkarCategory.AFTER_PRAYER
            nowMillis < today[Prayer.ISHA] -> AzkarCategory.EVENING
            else -> AzkarCategory.SLEEP
        }
    }
}
