package com.mfouad.muslimtv.feature.prayer.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.designsystem.component.ButtonStyle
import com.mfouad.muslimtv.core.designsystem.component.GeometricBackground
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.SafeArea
import com.mfouad.muslimtv.core.designsystem.focus.requestFocusSafely
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.designsystem.theme.MuslimTvTheme
import com.mfouad.muslimtv.core.media.QuranPlayer
import com.mfouad.muslimtv.core.model.PlannedAlert
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.ui.label
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import com.mfouad.muslimtv.feature.prayer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class Phase { ADHAN, IQAMAH, PRAY }

/**
 * Full-screen adhan, opened by the prayer alarm. Plays the adhan, then counts down to iqamah,
 * then offers the after-prayer azkar. Back or "Close" ends it at any point.
 */
@UnstableApi
@AndroidEntryPoint
class AdhanActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var scheduler: AdhanScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val prayer = intent.getStringExtra(EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() } ?: Prayer.DHUHR
        val at = intent.getLongExtra(EXTRA_AT, System.currentTimeMillis())
        val arabic = ConfigurationCompat.getLocales(resources.configuration)[0]?.language == "ar"

        // Plan the next alert right away, so it is set even if this screen is closed quickly.
        lifecycleScope.launch { scheduler.rescheduleNow() }

        var iqamahMinutes by mutableStateOf(15)
        lifecycleScope.launch { iqamahMinutes = settings.adhan.first().iqamah(prayer) }

        setContent {
            MuslimTvTheme(arabic = arabic) {
                AdhanScreen(
                    prayer = prayer,
                    adhanAt = at,
                    iqamahMinutes = iqamahMinutes,
                    isPlaying = { AdhanService.isPlaying.value },
                    onStop = { AdhanService.stop(this) },
                    onAzkar = {
                        startActivity(
                            (packageManager.getLeanbackLaunchIntentForPackage(packageName) ?: Intent())
                                .putExtra(EXTRA_OPEN, OPEN_AFTER_PRAYER_AZKAR)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        )
                        AdhanService.stop(this)
                        finish()
                    },
                    onClose = { AdhanService.stop(this); finish() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_PRAYER = "prayer"
        private const val EXTRA_AT = "at"
        const val EXTRA_OPEN = "open"
        const val OPEN_AFTER_PRAYER_AZKAR = "azkar_after_prayer"

        fun intent(context: Context, prayer: Prayer, at: Long): Intent =
            Intent(context, AdhanActivity::class.java)
                .putExtra(EXTRA_PRAYER, prayer.name)
                .putExtra(EXTRA_AT, at)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun AdhanScreen(
    prayer: Prayer,
    adhanAt: Long,
    iqamahMinutes: Int,
    isPlaying: () -> Boolean,
    onStop: () -> Unit,
    onAzkar: () -> Unit,
    onClose: () -> Unit,
) {
    var phase by remember { mutableStateOf(Phase.ADHAN) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val f = rememberTimeFormatter()
    val primary = remember { FocusRequester() }
    val iqamahAt = adhanAt + iqamahMinutes * 60_000L

    LaunchedEffect(Unit) {
        delay(ADHAN_START_GRACE_MS)
        while (true) {
            now = System.currentTimeMillis()
            if (phase == Phase.ADHAN && !isPlaying()) phase = Phase.IQAMAH
            if (phase == Phase.IQAMAH && now >= iqamahAt) phase = Phase.PRAY
            delay(500)
        }
    }
    LaunchedEffect(phase) { primary.requestFocusSafely() }

    GeometricBackground(patternAlpha = 0.08f) {
        SafeArea {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MtvSpacing.l, Alignment.CenterVertically)) {
                Text(stringResource(R.string.adhan_now, prayer.label()), style = MtvTheme.type.title, color = MtvTheme.colors.gold)
                when (phase) {
                    Phase.ADHAN -> {
                        Text("اللَّهُ أَكْبَرُ", style = MtvTheme.type.display.copy(fontFamily = com.mfouad.muslimtv.core.designsystem.theme.QuranFamily), color = MtvTheme.colors.onBackground)
                        MtvButton(stringResource(R.string.adhan_stop), { onStop(); phase = Phase.IQAMAH }, Modifier.focusRequester(primary))
                    }
                    Phase.IQAMAH -> {
                        Text(stringResource(R.string.iqamah_in), style = MtvTheme.type.title, color = MtvTheme.colors.onBackgroundMuted)
                        Text(f.countdown(iqamahAt - now), style = MtvTheme.type.numeric.copy(fontSize = MtvTheme.type.display.fontSize * 1.6f))
                        MtvButton(stringResource(R.string.close), onClose, Modifier.focusRequester(primary), style = ButtonStyle.Secondary)
                    }
                    Phase.PRAY -> {
                        Text(stringResource(R.string.iqamah_now), style = MtvTheme.type.display)
                        Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                            MtvButton(stringResource(R.string.open_after_prayer_azkar), onAzkar, Modifier.focusRequester(primary))
                            MtvButton(stringResource(R.string.close), onClose, style = ButtonStyle.Secondary)
                        }
                    }
                }
            }
        }
    }
}

private const val ADHAN_START_GRACE_MS = 2_000L
