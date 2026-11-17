package com.mfouad.muslimtv.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.azkar.AzkarRepository
import com.mfouad.muslimtv.core.data.prayer.PrayerSchedule
import com.mfouad.muslimtv.core.data.prayer.PrayerScheduleRepository
import com.mfouad.muslimtv.core.data.prayer.PrayerTimesCalculator
import com.mfouad.muslimtv.core.data.quran.AyahOfTheDaySelector
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.designsystem.component.NatureBackground
import com.mfouad.muslimtv.core.designsystem.focus.requestFocusSafely
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.designsystem.theme.NaskhFamily
import com.mfouad.muslimtv.core.designsystem.theme.QuranFamily
import com.mfouad.muslimtv.core.model.AzkarCategory
import com.mfouad.muslimtv.core.model.QuranText
import com.mfouad.muslimtv.core.model.UserSettings
import com.mfouad.muslimtv.core.ui.format
import com.mfouad.muslimtv.core.ui.label
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object AmbientRoute

fun NavGraphBuilder.ambientScreen(onExit: () -> Unit) {
    composable<AmbientRoute> { AmbientEntry(onExit) }
}

/** A line shown on the ambient screen: Quran text (Quran font) or a short zikr (Naskh). */
data class AmbientLine(val text: String, val isQuran: Boolean, val source: String)

@HiltViewModel
class AmbientViewModel @Inject constructor(
    schedules: PrayerScheduleRepository,
    settings: SettingsRepository,
    time: TimeProvider,
    private val quran: QuranRepository,
    private val azkar: AzkarRepository,
) : ViewModel() {
    val schedule: StateFlow<PrayerSchedule?> = schedules.schedule.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val settings: StateFlow<UserSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val now: StateFlow<Long> = time.ticker().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), time.nowMillis())

    private val _lines = MutableStateFlow<List<AmbientLine>>(emptyList())
    val lines: StateFlow<List<AmbientLine>> = _lines.asStateFlow()

    init {
        viewModelScope.launch {
            val ayahs = AyahOfTheDaySelector.refs.mapNotNull { ref ->
                quran.ayah(ref)?.let { AmbientLine(QuranText.displayText(it.surah, it.ayah, it.text), true, "${quran.surah(ref.surah).arabicName} · ${QuranText.arabicNumber(ref.ayah)}") }
            }
            // Short daily azkar only, so they fit the screen in large type.
            val zikr = listOf(AzkarCategory.MORNING, AzkarCategory.EVENING, AzkarCategory.AFTER_PRAYER).flatMap { c ->
                azkar.forCategory(c)?.items.orEmpty().filter { it.text.length in 20..SHORT_ZIKR }.map { AmbientLine(it.text, false, it.reference) }
            }
            // Alternate ayah and zikr, shuffled so each visit feels fresh.
            val a = ayahs.shuffled()
            val z = zikr.shuffled()
            _lines.value = (0 until maxOf(a.size, z.size)).flatMap { i -> listOfNotNull(a.getOrNull(i), z.getOrNull(i)) }
        }
    }

    private companion object { const val SHORT_ZIKR = 140 }
}

/**
 * Screensaver for a TV left on: large clock, next prayer countdown, Hijri date and a rotating ayah
 * or zikr over nature scenes. Everything drifts slightly every minute to avoid burn-in. Any key
 * returns to the previous screen.
 */
@Composable
fun AmbientEntry(onExit: () -> Unit, viewModel: AmbientViewModel = hiltViewModel()) {
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter(settings?.timeFormat ?: com.mfouad.muslimtv.core.model.TimeFormat.SYSTEM)
    val focus = remember { FocusRequester() }
    var lineIndex by remember { mutableIntStateOf(0) }
    var drift by remember { mutableStateOf(0 to 0) }
    val dx by animateDpAsState(drift.first.dp, tween(DRIFT_ANIM_MS), label = "dx")
    val dy by animateDpAsState(drift.second.dp, tween(DRIFT_ANIM_MS), label = "dy")

    // Keep the TV awake while the ambient screen is showing.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(Unit) { focus.requestFocusSafely() }
    LaunchedEffect(lines.size) {
        while (lines.isNotEmpty()) {
            delay(LINE_MS)
            lineIndex = (lineIndex + 1) % lines.size
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(DRIFT_EVERY_MS)
            drift = Random.nextInt(-DRIFT_DP, DRIFT_DP + 1) to Random.nextInt(-DRIFT_DP / 2, DRIFT_DP / 2 + 1)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            // Any key wakes the TV back to where the family was.
            .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyUp) onExit(); true },
        contentAlignment = Alignment.Center,
    ) {
        NatureBackground(settings?.natureScene ?: -1, dim = 0.68f)
        Column(
            Modifier.fillMaxSize().offset(dx, dy).padding(vertical = MtvSpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            val s = schedule
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (s != null) {
                    val (t, suffix) = f.clock(now, s.zone)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(t, style = MtvTheme.type.numeric.copy(fontSize = 120.sp, lineHeight = 130.sp))
                        if (suffix != null) Text(suffix, style = MtvTheme.type.title, color = MtvTheme.colors.onBackgroundMuted, modifier = Modifier.padding(start = 8.dp, bottom = 22.dp))
                    }
                    Text(s.hijri.format(), style = MtvTheme.type.title, color = MtvTheme.colors.goldSoft)
                    Text(f.gregorian(s.date), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
                }
            }
            val line = lines.getOrNull(lineIndex)
            Crossfade(line, animationSpec = tween(1_500), modifier = Modifier.widthIn(max = 1100.dp).weight(1f, fill = false), label = "line") { l ->
                if (l != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        BasicText(
                            l.text,
                            style = MtvTheme.type.headline.copy(
                                fontFamily = if (l.isQuran) QuranFamily else NaskhFamily,
                                color = MtvTheme.colors.onBackground,
                                textAlign = TextAlign.Center,
                                textDirection = TextDirection.Rtl,
                                lineHeight = 1.7.em,
                            ),
                            autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 46.sp, stepSize = 2.sp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(l.source, style = MtvTheme.type.caption.copy(fontFamily = NaskhFamily), color = MtvTheme.colors.onBackgroundMuted, maxLines = 1)
                    }
                }
            }
            if (s != null) {
                val next = PrayerTimesCalculator.nextPrayer(s.today, s.tomorrow, now)
                Text(
                    stringResource(R.string.ambient_next, next.prayer.label(if (next.isTomorrow) s.date.plusDays(1) else s.date), f.time(next.atMillis, s.zone), f.countdown(next.atMillis - now)),
                    style = MtvTheme.type.title,
                    color = MtvTheme.colors.onBackground,
                )
            }
        }
    }
}

private const val LINE_MS = 40_000L
private const val DRIFT_EVERY_MS = 60_000L
private const val DRIFT_ANIM_MS = 3_000
private const val DRIFT_DP = 24
