package com.mfouad.muslimtv.core.model

import java.time.LocalDate

/** Prayer times for one day at one city, as epoch milliseconds. */
data class PrayerDay(
    val date: LocalDate,
    val timeZone: String,
    val times: Map<Prayer, Long>,
) {
    operator fun get(prayer: Prayer): Long = times.getValue(prayer)
}

data class NextPrayer(
    val prayer: Prayer,
    val atMillis: Long,
    val isTomorrow: Boolean,
)
