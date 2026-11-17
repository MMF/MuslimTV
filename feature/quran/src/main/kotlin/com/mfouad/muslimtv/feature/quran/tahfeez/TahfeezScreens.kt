package com.mfouad.muslimtv.feature.quran.tahfeez

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.data.audio.Reciters
import com.mfouad.muslimtv.core.designsystem.component.ButtonStyle
import com.mfouad.muslimtv.core.designsystem.component.FocusCard
import com.mfouad.muslimtv.core.designsystem.component.IconButton
import com.mfouad.muslimtv.core.designsystem.component.ListRow
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.QuranText
import com.mfouad.muslimtv.core.designsystem.focus.FocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.requestFocusSafely
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.PauseMode
import com.mfouad.muslimtv.core.model.QuranText as QText
import com.mfouad.muslimtv.core.model.TahfeezState
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import com.mfouad.muslimtv.feature.quran.R
import com.mfouad.muslimtv.feature.quran.displayName
import kotlinx.coroutines.launch

/** The "Memorize" tab: new session, kids' preset, and saved sessions to resume. */
@Composable
fun TahfeezTab(
    focus: FocusRestoreState,
    onNew: () -> Unit,
    onKids: () -> Unit,
    onResume: (Long) -> Unit,
    viewModel: TahfeezListViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    LazyColumn(Modifier.widthIn(max = 1100.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s), contentPadding = PaddingValues(8.dp)) {
        item(key = "new") {
            Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                MtvButton(stringResource(R.string.tz_new), onNew, Modifier.restorableFocus(focus, "tz_new"), icon = Icons.Rounded.Add)
                MtvButton(stringResource(R.string.tz_kids), onKids, Modifier.restorableFocus(focus, "tz_kids"), style = ButtonStyle.Secondary, icon = Icons.Rounded.ChildCare)
            }
        }
        item(key = "h") { Text(stringResource(R.string.tz_saved), style = MtvTheme.type.label, color = MtvTheme.colors.gold, modifier = Modifier.padding(top = MtvSpacing.m)) }
        if (sessions.isEmpty()) item(key = "none") { Text(stringResource(R.string.tz_none), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted) }
        items(sessions, key = { it.id }) { s ->
            val surah = viewModel.surah(s.config.surah)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
                ListRow(
                    title = stringResource(R.string.tz_session_title, surah.displayName(), f.number(s.config.fromAyah), f.number(s.config.toAyah)),
                    subtitle = if (s.finished) stringResource(R.string.tz_finished) else stringResource(R.string.tz_resume, f.number(s.state.ayah)),
                    onClick = { onResume(s.id) },
                    modifier = Modifier.weight(1f).restorableFocus(focus, "tz${s.id}"),
                )
                IconButton(Icons.Rounded.Delete, stringResource(R.string.tz_delete), { viewModel.delete(s.id) }, Modifier.restorableFocus(focus, "tzd${s.id}"))
            }
        }
    }
}

@Composable
fun KidsPickerEntry(onStart: (Long) -> Unit, viewModel: TahfeezListViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val f = rememberTimeFormatter()
    val focus = rememberFocusRestoreState()
    // Kids usually start from the end of the Quran: An-Nas first.
    val surahs = remember { viewModel.juzAmma.reversed() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        Text(stringResource(R.string.tz_kids), style = MtvTheme.type.headline)
        Text(stringResource(R.string.tz_kids_hint), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
        LazyVerticalGrid(GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s), horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s), contentPadding = PaddingValues(8.dp)) {
            items(surahs, key = { it.number }) { s ->
                FocusCard(
                    onClick = { scope.launch { onStart(viewModel.startKids(s.number)) } },
                    modifier = Modifier.height(120.dp).restorableFocus(focus, "k${s.number}"),
                    contentPadding = MtvSpacing.s,
                ) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        QuranText(s.arabicName, sizeSp = 34, lineHeightEm = 1.3f, color = MtvTheme.colors.goldSoft)
                        Text("${f.number(s.number)} · ${s.transliteration}", style = MtvTheme.type.caption, color = MtvTheme.colors.onBackgroundMuted, maxLines = 1)
                    }
                }
            }
        }
    }
    RestoreFocusEffect(focus, "k114")
}

@Composable
fun TahfeezSetupEntry(onStart: (Long) -> Unit, viewModel: TahfeezSetupViewModel = hiltViewModel()) {
    val c by viewModel.config.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    val arabic = isArabicUi()
    val scope = rememberCoroutineScope()
    val focus = rememberFocusRestoreState()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.l)) {
            Text(stringResource(R.string.tz_setup_title, viewModel.surah.displayName()), style = MtvTheme.type.headline, modifier = Modifier.weight(1f))
            MtvButton(stringResource(R.string.tz_start), { scope.launch { onStart(viewModel.start()) } }, Modifier.restorableFocus(focus, "start"), icon = Icons.Rounded.PlayArrow)
        }
        LazyColumn(Modifier.widthIn(max = 1100.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs), contentPadding = PaddingValues(6.dp)) {
            item(key = "from") { Stepper(stringResource(R.string.tz_from), f.number(c.fromAyah), { viewModel.from(-1) }, { viewModel.from(1) }, focus, "from") }
            item(key = "to") { Stepper(stringResource(R.string.tz_to), f.number(c.toAyah), { viewModel.to(-1) }, { viewModel.to(1) }, focus, "to") }
            item(key = "rep") {
                Stepper(
                    stringResource(R.string.tz_repeats),
                    if (c.endless) stringResource(R.string.tz_endless) else stringResource(R.string.tz_times, f.number(c.repeatsPerAyah)),
                    { viewModel.repeats(-1) }, { viewModel.repeats(1) }, focus, "rep",
                )
            }
            item(key = "rounds") { Stepper(stringResource(R.string.tz_rounds), stringResource(R.string.tz_times, f.number(c.rangeRepeats)), { viewModel.rounds(-1) }, { viewModel.rounds(1) }, focus, "rounds") }
            item(key = "pause") {
                ListRow(stringResource(R.string.tz_pause), viewModel::cyclePause, Modifier.restorableFocus(focus, "pause"), value = pauseLabel(c.pause), showChevron = false)
            }
            item(key = "hide") {
                ListRow(
                    stringResource(R.string.tz_hide), viewModel::toggleHide, Modifier.restorableFocus(focus, "hide"),
                    subtitle = stringResource(R.string.tz_hide_hint),
                    value = stringResource(if (c.hideText) com.mfouad.muslimtv.feature.quran.R.string.on_label else com.mfouad.muslimtv.feature.quran.R.string.off_label),
                    showChevron = false,
                )
            }
            item(key = "reciter") {
                ListRow(stringResource(R.string.tz_reciter), viewModel::cycleReciter, Modifier.restorableFocus(focus, "reciter"), value = Reciters.byId(c.reciterId).displayName(arabic), showChevron = false)
            }
        }
    }
    RestoreFocusEffect(focus, "start")
}

@Composable
private fun pauseLabel(p: PauseMode): String {
    val f = rememberTimeFormatter()
    return when (p) {
        PauseMode.None -> stringResource(R.string.tz_pause_none)
        is PauseMode.Seconds -> stringResource(R.string.tz_pause_seconds, f.number(p.seconds))
        PauseMode.AyahLength -> stringResource(R.string.tz_pause_ayah)
    }
}

@Composable
private fun Stepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, focus: FocusRestoreState, key: String) {
    Row(
        Modifier.fillMaxWidth().background(MtvTheme.colors.surface, MtvShapes.small).padding(horizontal = MtvSpacing.m, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s),
    ) {
        Text(title, style = MtvTheme.type.bodyStrong, modifier = Modifier.weight(1f))
        Text(value, style = MtvTheme.type.body, color = MtvTheme.colors.goldSoft, modifier = Modifier.widthIn(min = 160.dp), textAlign = TextAlign.Center)
        IconButton(Icons.Rounded.Remove, title, onMinus, Modifier.restorableFocus(focus, "$key-"))
        IconButton(Icons.Rounded.Add, title, onPlus, Modifier.restorableFocus(focus, "$key+"))
    }
}

@OptIn(UnstableApi::class)
@Composable
fun TahfeezSessionEntry(onBack: () -> Unit, onDownloads: () -> Unit, viewModel: TahfeezSessionViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val state = ui.state ?: return
    val config = ui.config ?: return
    val surah = ui.surah ?: return
    val f = rememberTimeFormatter()
    val primary = remember { FocusRequester() }
    val textCard = remember { FocusRequester() }
    val c = MtvTheme.colors

    if (state.phase == TahfeezState.Phase.DONE) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MtvSpacing.l, Alignment.CenterVertically)) {
            Text(stringResource(R.string.tz_done_title), style = MtvTheme.type.display, color = c.gold)
            Text(stringResource(R.string.tz_done_body), style = MtvTheme.type.title, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                MtvButton(stringResource(R.string.close), onBack, Modifier.focusRequester(primary))
                MtvButton(stringResource(R.string.tz_again), viewModel::restart, style = ButtonStyle.Secondary, icon = Icons.Rounded.Refresh)
            }
        }
        LaunchedEffect(Unit) { primary.requestFocusSafely() }
        return
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        Column {
            QuranText("سُورَةُ " + surah.arabicName, sizeSp = 34, lineHeightEm = 1.4f, color = c.gold, modifier = Modifier.fillMaxWidth())
            Text(
                if (config.endless) stringResource(R.string.tz_progress_endless, f.number(state.ayah - config.fromAyah + 1), f.number(config.ayahCount), f.number(state.repeat))
                else stringResource(
                    R.string.tz_progress,
                    f.number(state.ayah - config.fromAyah + 1), f.number(config.ayahCount),
                    f.number(state.repeat), f.number(config.repeatsPerAyah),
                    f.number(state.round), f.number(config.rangeRepeats),
                ),
                style = MtvTheme.type.body,
                color = c.onBackgroundMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // The ayah card: OK reveals hidden text.
        FocusCard(
            onClick = viewModel::reveal,
            focusedScale = 1f,
            modifier = Modifier.weight(1f).fillMaxWidth().focusRequester(textCard),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (ui.hidden) {
                    Text(stringResource(R.string.tz_hidden), style = MtvTheme.type.title, color = c.onBackgroundMuted, textAlign = TextAlign.Center)
                } else {
                    QuranText(ui.ayahText + " " + QText.ayahMarker(state.ayah), sizeSp = 52, minSizeSp = 28, lineHeightEm = 1.75f, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        // Phase: listen, or your turn with a progress bar through the pause.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
            val reciting = state.phase == TahfeezState.Phase.RECITE
            Icon(if (reciting) Icons.Rounded.RecordVoiceOver else Icons.Rounded.Hearing, null, Modifier.size(32.dp), tint = c.gold)
            Text(stringResource(if (reciting) R.string.tz_your_turn else R.string.tz_listen), style = MtvTheme.type.title, color = if (reciting) c.gold else c.onBackground)
            Box(Modifier.weight(1f).height(10.dp).background(c.surfaceFocused, MtvShapes.pill)) {
                if (reciting) Box(Modifier.fillMaxHeight().fillMaxWidth(ui.reciteProgress).background(c.gold, MtvShapes.pill))
            }
            MtvButton(
                stringResource(if (ui.paused) R.string.tz_continue else R.string.tz_pause_session),
                viewModel::togglePause,
                Modifier.focusRequester(primary),
                icon = if (ui.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            )
            MtvButton(stringResource(R.string.tz_next_ayah), { viewModel.nextAyah() }, style = ButtonStyle.Secondary, icon = Icons.Rounded.SkipNext)
        }
        if (ui.offlineMissing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                Text(stringResource(R.string.problem_not_downloaded, surah.displayName(), Reciters.byId(config.reciterId).displayName(isArabicUi())), style = MtvTheme.type.body, color = c.error, modifier = Modifier.weight(1f))
                MtvButton(stringResource(R.string.problem_go_downloads), onDownloads)
            }
        }
    }
    LaunchedEffect(config.hideText) { if (config.hideText) textCard.requestFocusSafely() else primary.requestFocusSafely() }
}
