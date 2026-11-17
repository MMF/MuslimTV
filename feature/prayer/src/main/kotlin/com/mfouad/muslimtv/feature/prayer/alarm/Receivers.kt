package com.mfouad.muslimtv.feature.prayer.alarm

import android.Manifest
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
import com.mfouad.muslimtv.core.common.ApplicationScope
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.model.PlannedAlert
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.ui.labelRes
import com.mfouad.muslimtv.feature.prayer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires at prayer time. Adhan mode: the adhan plays from a foreground service (allowed from an
 * alarm even in the background) and the full-screen adhan opens. Notification mode: a quiet
 * notification only. Then the next alarm is planned.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@AndroidEntryPoint
class AlertReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: AdhanScheduler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra(EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
        val mode = intent.getStringExtra(EXTRA_MODE)
        if (prayer != null) {
            if (mode == com.mfouad.muslimtv.core.model.AlertMode.ADHAN.name) {
                val at = intent.getLongExtra(EXTRA_AT, System.currentTimeMillis())
                runCatching { AdhanService.start(context, prayer, at) }
                // Opens directly when Muslim TV is on screen; otherwise the full-screen intent does.
                runCatching { context.startActivity(AdhanActivity.intent(context, prayer, at)) }
            } else {
                notify(context, prayer)
            }
        }
        val pending = goAsync()
        scope.launch {
            try { scheduler.rescheduleNow() } finally { pending.finish() }
        }
    }

    private fun notify(context: Context, prayer: Prayer) {
        val nm = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.alert_channel), NotificationManager.IMPORTANCE_DEFAULT).apply { setSound(null, null) },
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = context.packageManager.getLeanbackLaunchIntentForPackage(context.packageName)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.alert_title, context.getString(prayer.labelRes())))
            .setSilent(true)
            .setAutoCancel(true)
            .apply { open?.let { setContentIntent(PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)) } }
            .build()
        runCatching { nm.notify(NOTIFICATION_ID, n) }
    }

    companion object {
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_MODE = "mode"
        const val EXTRA_AT = "at"
        private const val CHANNEL = "prayer_alerts"
        private const val NOTIFICATION_ID = 8001

        fun intent(context: Context, alert: PlannedAlert) =
            Intent(context, AlertReceiver::class.java)
                .putExtra(EXTRA_PRAYER, alert.prayer.name)
                .putExtra(EXTRA_MODE, alert.mode.name)
                .putExtra(EXTRA_AT, alert.atMillis)
    }
}

/** Alarms are lost on reboot and go stale when the clock or time zone changes: plan again. */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: AdhanScheduler
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var reminders: ReminderScheduler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                scheduler.rescheduleNow()
                reminders.reschedule()
                if (intent.action == Intent.ACTION_BOOT_COMPLETED && settings.adhan.first().startOnBoot) {
                    // Optional "start on boot". Some TVs block activity starts from the background.
                    context.packageManager.getLeanbackLaunchIntentForPackage(context.packageName)?.let {
                        runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
