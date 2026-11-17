package com.mfouad.muslimtv.feature.prayer.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.media.QuranPlayer
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.ui.labelRes
import com.mfouad.muslimtv.feature.prayer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Plays the adhan in a foreground service, so it sounds even when another app is on screen and
 * Android blocks opening our screen. Its notification carries a full-screen intent for the adhan
 * screen and a Stop action.
 */
@UnstableApi
@AndroidEntryPoint
class AdhanService : Service() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var quranPlayer: QuranPlayer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAdhan()
            return START_NOT_STICKY
        }
        val prayer = intent?.getStringExtra(EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() } ?: Prayer.DHUHR
        val at = intent?.getLongExtra(EXTRA_AT, System.currentTimeMillis()) ?: System.currentTimeMillis()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(prayer, at), type)
        quranPlayer.pause()
        showScreenOverOtherApps(prayer, at)
        scope.launch {
            val muezzin = settings.adhan.first().muezzinId
            play(if (muezzin == "short") R.raw.adhan_short else R.raw.adhan_full)
        }
        return START_NOT_STICKY
    }

    /**
     * With "display over other apps" granted, Android lets us open the adhan screen from the
     * background, provided an overlay window is visible at that moment (Android 15+). A 1-pixel
     * transparent overlay is shown briefly for that.
     */
    private fun showScreenOverOtherApps(prayer: Prayer, at: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return
        val wm = getSystemService(WindowManager::class.java)
        val view = View(this)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            1, 1, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        )
        runCatching { wm.addView(view, params) }.onFailure { return }
        view.postDelayed({ runCatching { startActivity(AdhanActivity.intent(this, prayer, at)) } }, OVERLAY_SETTLE_MS)
        view.postDelayed({ runCatching { wm.removeView(view) } }, OVERLAY_SETTLE_MS * 5)
    }

    private fun play(res: Int) {
        player?.release()
        player = MediaPlayer.create(
            this,
            res,
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
        )?.apply {
            setOnCompletionListener { stopAdhan() }
            start()
        }
        playing.value = player != null
        if (player == null) stopAdhan()
    }

    private fun stopAdhan() {
        player?.run { runCatching { if (isPlaying) stop() }; release() }
        player = null
        playing.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(prayer: Prayer, at: Long): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.alert_channel), NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null) })
        }
        val screen = PendingIntent.getActivity(this, 1, AdhanActivity.intent(this, prayer, at), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 2, Intent(this, AdhanService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.adhan_now, getString(prayer.labelRes())))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(screen, true)
            .setContentIntent(screen)
            .addAction(0, getString(R.string.adhan_stop), stop)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        playing.value = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "adhan"
        private const val NOTIFICATION_ID = 8002
        private const val ACTION_STOP = "stop"
        private const val OVERLAY_SETTLE_MS = 300L
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_AT = "at"

        private val playing = MutableStateFlow(false)
        /** Lets the adhan screen know when the recording has finished. */
        val isPlaying: StateFlow<Boolean> = playing.asStateFlow()

        fun start(context: Context, prayer: Prayer, at: Long) {
            val i = Intent(context, AdhanService::class.java).putExtra(EXTRA_PRAYER, prayer.name).putExtra(EXTRA_AT, at)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AdhanService::class.java).setAction(ACTION_STOP))
        }
    }
}
