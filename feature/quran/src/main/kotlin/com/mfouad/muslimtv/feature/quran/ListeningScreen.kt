package com.mfouad.muslimtv.feature.quran

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.ButtonStyle
import com.mfouad.muslimtv.core.designsystem.component.IconButton
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.QuranText
import com.mfouad.muslimtv.core.designsystem.focus.FocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvMotion
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.media.SleepTimer
import com.mfouad.muslimtv.core.model.QuranText as QText
import com.mfouad.muslimtv.core.model.RepeatMode
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter

/**
 * Full-screen listening: the recited ayah large in the middle, following the audio, with the
 * controls below. [background] lets the nature mode draw scenery behind it.
 */
@OptIn(UnstableApi::class)
@Composable
fun ListeningEntry(
    onChooseSurah: () -> Unit,
    background: @Composable () -> Unit = {},
    extraControls: @Composable androidx.compose.foundation.layout.RowScope.(FocusRestoreState) -> Unit = {},
    viewModel: ListeningViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    val arabic = isArabicUi()
    val focus = rememberFocusRestoreState()
    val c = MtvTheme.colors

    Box(Modifier.fillMaxSize()) {
        background()
        if (!state.isActive) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                Text(stringResource(R.string.player_idle), style = MtvTheme.type.body, color = c.onBackgroundMuted)
                MtvButton(stringResource(R.string.player_choose_surah), onChooseSurah, Modifier.restorableFocus(focus, "choose"))
            }
            RestoreFocusEffect(focus, "choose")
            return@Box
        }
        val surah = viewModel.surahName(state.surah.coerceAtLeast(1))
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QuranText("سُورَةُ " + surah.arabicName, sizeSp = 36, lineHeightEm = 1.4f, color = c.gold)
                Text(
                    state.reciter.displayName(arabic) + (now.ayah?.let { " · " + stringResource(R.string.ayah_ref, surah.displayName(), f.number(it.ayah)) } ?: ""),
                    style = MtvTheme.type.body,
                    color = c.onBackgroundMuted,
                )
            }
            Crossfade(
                targetState = now.ayah?.ayah to now.isBasmala,
                animationSpec = MtvMotion.standard(MtvMotion.LONG),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = MtvSpacing.m),
                label = "ayah",
            ) { (_, basmala) ->
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val a = now.ayah
                    if (basmala) {
                        QuranText(QText.BASMALA, sizeSp = 56, color = c.onBackground)
                    } else if (a != null) {
                        val gold = c.gold
                        val text = remember(a.text, now.word) {
                            buildAnnotatedString {
                                QText.tokens(a.text).forEachIndexed { i, t ->
                                    if (i > 0) append(' ')
                                    if (now.word != null && t.wordIndex == now.word) withStyle(SpanStyle(color = gold)) { append(t.text) } else append(t.text)
                                }
                                append(' ')
                                withStyle(SpanStyle(color = gold)) { append(QText.ayahMarker(a.ayah)) }
                            }
                        }
                        QuranText(text, sizeSp = (settings.quranFontSizeSp + 8).coerceAtMost(72), textAlign = TextAlign.Center, lineHeightEm = 1.75f, minSizeSp = 28, modifier = Modifier.fillMaxWidth().weight(1f, fill = false))
                        a.translation?.let { Text(it, style = MtvTheme.type.body, color = c.onBackgroundMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = MtvSpacing.s)) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                IconButton(Icons.Rounded.SkipPrevious, stringResource(R.string.ctl_prev_surah), { viewModel.player.previousSurah() }, Modifier.restorableFocus(focus, "ps"))
                // Arrows point the way the Quran text flows: right to left.
                IconButton(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.ctl_prev_ayah), { viewModel.player.previousAyah() }, Modifier.restorableFocus(focus, "pa"))
                MtvButton(
                    stringResource(if (state.isPlaying) R.string.ctl_pause else R.string.ctl_play),
                    { viewModel.player.togglePlayPause() },
                    Modifier.restorableFocus(focus, "play"),
                    icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                )
                IconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.ctl_next_ayah), { viewModel.player.nextAyah() }, Modifier.restorableFocus(focus, "na"))
                IconButton(Icons.Rounded.SkipNext, stringResource(R.string.ctl_next_surah), { viewModel.player.nextSurah() }, Modifier.restorableFocus(focus, "ns"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = MtvSpacing.s)) {
                MtvButton(
                    stringResource(R.string.ctl_speed, state.speed.toString().removeSuffix(".0")),
                    viewModel::cycleSpeed, Modifier.restorableFocus(focus, "speed"), style = ButtonStyle.Secondary, icon = Icons.Rounded.Speed,
                )
                MtvButton(
                    stringResource(
                        when (state.repeat) {
                            RepeatMode.OFF -> R.string.ctl_repeat_off
                            RepeatMode.AYAH -> R.string.ctl_repeat_ayah
                            RepeatMode.SURAH -> R.string.ctl_repeat_surah
                        },
                    ),
                    viewModel::cycleRepeat, Modifier.restorableFocus(focus, "repeat"), style = ButtonStyle.Secondary,
                    icon = if (state.repeat == RepeatMode.AYAH) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                )
                MtvButton(
                    when (state.sleep) {
                        SleepTimer.OFF -> stringResource(R.string.ctl_sleep_off)
                        SleepTimer.END_OF_SURAH -> stringResource(R.string.ctl_sleep_surah)
                        else -> stringResource(R.string.ctl_sleep_min, f.number(state.sleep.minutes))
                    },
                    viewModel::cycleSleep, Modifier.restorableFocus(focus, "sleep"), style = ButtonStyle.Secondary, icon = Icons.Rounded.Bedtime,
                )
                extraControls(focus)
            }
        }
        RestoreFocusEffect(focus, "play")
    }
}
