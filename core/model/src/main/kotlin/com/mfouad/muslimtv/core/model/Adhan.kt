package com.mfouad.muslimtv.core.model

/** What happens at a prayer time. */
enum class AlertMode { ADHAN, NOTIFICATION, OFF }

data class AdhanSettings(
    val modes: Map<Prayer, AlertMode> = Prayer.entries.filter { it.isObligatory }.associateWith { AlertMode.ADHAN },
    val muezzinId: String = "full",
    /** Minutes from adhan to iqamah, per prayer. */
    val iqamahMinutes: Map<Prayer, Int> = mapOf(
        Prayer.FAJR to 20, Prayer.DHUHR to 15, Prayer.ASR to 15, Prayer.MAGHRIB to 7, Prayer.ISHA to 15,
    ),
    val startOnBoot: Boolean = false,
) {
    fun mode(p: Prayer): AlertMode = if (p.isObligatory) modes[p] ?: AlertMode.ADHAN else AlertMode.OFF
    fun iqamah(p: Prayer): Int = iqamahMinutes[p] ?: 15
}

/** The next thing the alarm should wake the TV for. */
data class PlannedAlert(val prayer: Prayer, val atMillis: Long, val mode: AlertMode)

/** Chooses the next alert to schedule. Pure, so it is unit tested. */
object AlertPlanner {
    fun next(today: PrayerDay, tomorrow: PrayerDay, settings: AdhanSettings, nowMillis: Long): PlannedAlert? =
        (listOf(today, tomorrow)).asSequence()
            .flatMap { day -> Prayer.entries.asSequence().filter { it.isObligatory }.map { it to day[it] } }
            .filter { (_, at) -> at > nowMillis }
            .map { (p, at) -> PlannedAlert(p, at, settings.mode(p)) }
            .firstOrNull { it.mode != AlertMode.OFF }
}
