package com.mfouad.muslimtv.feature.prayer.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.mfouad.muslimtv.core.common.ApplicationScope
import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.prayer.PrayerScheduleRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.model.AlertMode
import com.mfouad.muslimtv.core.model.AlertPlanner
import com.mfouad.muslimtv.core.model.PlannedAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps exactly one alarm set: the next prayer whose alert is on. Re-planned when settings or the
 * day change, after each alert fires, and after reboots or clock/time-zone changes.
 */
@Singleton
class AdhanScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val schedules: PrayerScheduleRepository,
    private val time: TimeProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val alarms = context.getSystemService<AlarmManager>()!!

    /** Call once from the Application: follows settings and date changes. */
    fun start() = scope.launch {
        combine(schedules.schedule, settings.adhan) { _, _ -> }.collect { rescheduleNow() }
    }

    suspend fun rescheduleNow(): PlannedAlert? {
        val s = settings.settings.first()
        val city = s.city ?: return null.also { cancel() }
        val zone = runCatching { ZoneId.of(city.timeZone) }.getOrDefault(ZoneId.systemDefault())
        val now = time.nowMillis()
        val schedule = schedules.build(s, city, Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
        val alert = AlertPlanner.next(schedule.today, schedule.tomorrow, settings.adhan.first(), now)
        cancel()
        if (alert != null) schedule(alert)
        return alert
    }

    private fun schedule(alert: PlannedAlert) {
        if (alert.mode == AlertMode.OFF) return
        // Always a broadcast: Android 14+ blocks alarms from opening activities directly.
        val operation = PendingIntent.getBroadcast(context, REQUEST_ALERT, AlertReceiver.intent(context, alert), FLAGS)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (canExact) {
            // An alarm-clock alarm is exact, survives Doze, and may open the adhan screen directly.
            val show = PendingIntent.getActivity(context, REQUEST_SHOW, launchIntent(), FLAGS)
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(alert.atMillis, show), operation)
        } else {
            // Without the exact-alarm permission the system may deliver this a few minutes late.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alert.atMillis, operation)
        }
    }

    /** "Test the adhan": the real alarm path, one minute from now (then normal planning resumes). */
    fun scheduleTest(prayer: com.mfouad.muslimtv.core.model.Prayer) {
        schedule(PlannedAlert(prayer, time.nowMillis() + TEST_DELAY_MS, AlertMode.ADHAN))
    }

    fun cancel() {
        alarms.cancel(PendingIntent.getBroadcast(context, REQUEST_ALERT, Intent(context, AlertReceiver::class.java), FLAGS))
    }

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    private fun launchIntent(): Intent =
        context.packageManager.getLeanbackLaunchIntentForPackage(context.packageName)
            ?: context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()

    private companion object {
        const val REQUEST_ALERT = 42
        const val TEST_DELAY_MS = 60_000L
        const val REQUEST_SHOW = 43
        const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}
