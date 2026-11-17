package com.mfouad.muslimtv.feature.prayer.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.mfouad.muslimtv.core.common.ApplicationScope
import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.prayer.PrayerScheduleRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.feature.prayer.R
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Gentle reminders: Surah Al-Kahf on Friday (late morning) and Surah Al-Mulk an hour after Isha.
 * Inexact alarms are enough here; Home also shows a chip at the right times.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val schedules: PrayerScheduleRepository,
    private val time: TimeProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val alarms = context.getSystemService<AlarmManager>()!!

    fun start() = scope.launch { combine(schedules.schedule, settings.settings) { _, _ -> }.collect { reschedule() } }

    suspend fun reschedule() {
        val s = settings.settings.first()
        val city = s.city ?: return
        val zone = runCatching { ZoneId.of(city.timeZone) }.getOrDefault(ZoneId.systemDefault())
        val now = time.nowMillis()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        cancel(SURAH_KAHF); cancel(SURAH_MULK)
        if (s.kahfReminder) {
            // Next Friday, 90 minutes before Dhuhr.
            val friday = (0..7L).map { today.plusDays(it) }.first { d ->
                d.dayOfWeek == DayOfWeek.FRIDAY && schedules.build(s, city, d).today[Prayer.DHUHR] - KAHF_BEFORE_DHUHR_MS > now
            }
            set(SURAH_KAHF, schedules.build(s, city, friday).today[Prayer.DHUHR] - KAHF_BEFORE_DHUHR_MS)
        }
        if (s.mulkReminder) {
            val tonight = schedules.build(s, city, today).today[Prayer.ISHA] + MULK_AFTER_ISHA_MS
            set(SURAH_MULK, if (tonight > now) tonight else schedules.build(s, city, today.plusDays(1)).today[Prayer.ISHA] + MULK_AFTER_ISHA_MS)
        }
    }

    private fun pending(surah: Int) = PendingIntent.getBroadcast(
        context, surah, Intent(context, ReminderReceiver::class.java).putExtra(ReminderReceiver.EXTRA_SURAH, surah),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun set(surah: Int, at: Long) = alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(surah))
    private fun cancel(surah: Int) = alarms.cancel(pending(surah))

    companion object {
        const val SURAH_KAHF = 18
        const val SURAH_MULK = 67
        private const val KAHF_BEFORE_DHUHR_MS = 90 * 60_000L
        private const val MULK_AFTER_ISHA_MS = 60 * 60_000L
    }
}

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val surah = intent.getIntExtra(EXTRA_SURAH, 0)
        if (surah != 0) notify(context, surah)
        val pending = goAsync()
        scope.launch { try { scheduler.reschedule() } finally { pending.finish() } }
    }

    private fun notify(context: Context, surah: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = (context.packageManager.getLeanbackLaunchIntentForPackage(context.packageName) ?: Intent())
            .putExtra(AdhanActivity.EXTRA_OPEN, "reader:$surah")
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(if (surah == ReminderScheduler.SURAH_KAHF) R.string.reminder_kahf_title else R.string.reminder_mulk_title))
            .setContentText(context.getString(R.string.reminder_tap))
            .setContentIntent(PendingIntent.getActivity(context, surah, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE + surah, n) }
    }

    companion object {
        const val EXTRA_SURAH = "surah"
        private const val CHANNEL = "reminders"
        private const val NOTIFICATION_BASE = 9000
    }
}
