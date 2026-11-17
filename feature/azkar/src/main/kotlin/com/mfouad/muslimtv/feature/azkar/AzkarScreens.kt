package com.mfouad.muslimtv.feature.azkar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.ButtonStyle
import com.mfouad.muslimtv.core.designsystem.component.DialogInitialFocus
import com.mfouad.muslimtv.core.designsystem.component.DialogPanel
import com.mfouad.muslimtv.core.designsystem.component.FocusCard
import com.mfouad.muslimtv.core.designsystem.component.ListRow
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.Pill
import com.mfouad.muslimtv.core.designsystem.component.QuranText
import com.mfouad.muslimtv.core.designsystem.component.ScreenHeader
import com.mfouad.muslimtv.core.designsystem.component.TabChips
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.requestFocusSafely
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.designsystem.theme.NaskhFamily
import com.mfouad.muslimtv.core.model.AzkarCategory
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.labelRes
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class HomeTile(val key: String, val title: String, val icon: ImageVector, val count: Int?, val onClick: () -> Unit)

@Composable
fun AzkarHomeEntry(
    onCategory: (AzkarCategory) -> Unit,
    onOccasions: () -> Unit,
    onTasbeeh: () -> Unit,
    onAsma: () -> Unit,
    onRuqyah: () -> Unit,
    viewModel: AzkarHomeViewModel = hiltViewModel(),
) {
    val suggested by viewModel.suggested.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val focus = rememberFocusRestoreState()
    val f = rememberTimeFormatter()
    val daily = listOf(
        AzkarCategory.MORNING to Icons.Rounded.WbSunny,
        AzkarCategory.EVENING to Icons.Rounded.WbTwilight,
        AzkarCategory.AFTER_PRAYER to Icons.Rounded.Mosque,
        AzkarCategory.SLEEP to Icons.Rounded.Bedtime,
        AzkarCategory.WAKING to Icons.Rounded.NightsStay,
    )
    val tiles = daily.map { (c, icon) -> HomeTile(c.name, stringResource(c.labelRes()), icon, counts[c]) { onCategory(c) } } + listOf(
        HomeTile("occasions", stringResource(R.string.occasions), Icons.Rounded.Favorite, null, onOccasions),
        HomeTile("tasbeeh", stringResource(R.string.tasbeeh), Icons.Rounded.TouchApp, null, onTasbeeh),
        HomeTile("asma", stringResource(R.string.asma), Icons.AutoMirrored.Rounded.MenuBook, 99, onAsma),
        HomeTile("ruqyah", stringResource(R.string.ruqyah), Icons.Rounded.HealthAndSafety, null, onRuqyah),
    )
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.azkar_title))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(MtvSpacing.m),
            horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(tiles, key = { it.key }) { t ->
                FocusCard(onClick = t.onClick, modifier = Modifier.height(158.dp).restorableFocus(focus, t.key), contentPadding = MtvSpacing.m) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(t.icon, null, Modifier.size(30.dp), tint = MtvTheme.colors.gold)
                            Box(Modifier.weight(1f))
                            if (t.key == suggested?.name) Pill(stringResource(R.string.suggested_short))
                        }
                        Column {
                            Text(t.title, style = MtvTheme.type.title.copy(fontSize = 26.sp, lineHeight = 32.sp), maxLines = 2)
                            t.count?.let { Text(stringResource(R.string.items_count, f.number(it)), style = MtvTheme.type.caption, color = MtvTheme.colors.onBackgroundMuted) }
                        }
                    }
                }
            }
        }
    }
    // Wait for the suggestion so first focus lands on the azkar that fit the time of day.
    RestoreFocusEffect(focus, suggested?.name, enabled = suggested != null)
}

@Composable
fun OccasionsEntry(onChapter: (Int) -> Unit, viewModel: AzkarHomeViewModel = hiltViewModel()) {
    val chapters by viewModel.occasions.collectAsStateWithLifecycle()
    val focus = rememberFocusRestoreState()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.occasions))
        LazyColumn(Modifier.widthIn(max = 1000.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs), contentPadding = PaddingValues(6.dp)) {
            items(chapters, key = { it.id }) { c ->
                ListRow(
                    title = c.title,
                    onClick = { onChapter(c.id) },
                    modifier = Modifier.restorableFocus(focus, "c${c.id}"),
                )
            }
        }
    }
    if (chapters.isNotEmpty()) RestoreFocusEffect(focus, "c${chapters.first().id}")
}

/**
 * One zikr per card. OK counts one repetition; when the count is complete the card moves on by
 * itself after a short pause. Left/Right go back and forward (mirrored in Arabic).
 */
@Composable
fun ZikrEntry(onBack: () -> Unit, viewModel: ZikrViewModel = hiltViewModel()) {
    val chapter by viewModel.chapter.collectAsStateWithLifecycle()
    val index by viewModel.index.collectAsStateWithLifecycle()
    val counted by viewModel.counted.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    val c = chapter ?: return
    val zikr = c.items.getOrNull(index) ?: return
    val f = rememberTimeFormatter()
    val scope = rememberCoroutineScope()
    val card = remember { FocusRequester() }
    val pulse = remember { Animatable(1f) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    if (finished) {
        CompletedPanel(onAgain = viewModel::restart, onBack = onBack)
        return
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.title, style = MtvTheme.type.title.copy(fontFamily = NaskhFamily), modifier = Modifier.weight(1f), maxLines = 1)
            Text(stringResource(R.string.zikr_progress, f.number(index + 1), f.number(c.items.size)), style = MtvTheme.type.label, color = MtvTheme.colors.onBackgroundMuted)
        }
        FocusCard(
            onClick = {
                val complete = viewModel.count()
                scope.launch {
                    pulse.snapTo(1.04f)
                    pulse.animateTo(1f, tween(260))
                    if (complete) {
                        delay(AUTO_ADVANCE_MS)
                        viewModel.go(1)
                    }
                }
            },
            focusedScale = 1f,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .scale(pulse.value)
                .focusRequester(card)
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val forward = if (rtl) Key.DirectionLeft else Key.DirectionRight
                    val backward = if (rtl) Key.DirectionRight else Key.DirectionLeft
                    when (e.key) {
                        forward -> { viewModel.go(1); true }
                        backward -> { viewModel.go(-1); true }
                        else -> false
                    }
                },
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
                // Arabic dhikr text, never cut: it shrinks to fit the card instead.
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                BasicText(
                    text = zikr.text,
                    style = MtvTheme.type.headline.copy(
                        fontFamily = NaskhFamily,
                        color = MtvTheme.colors.onBackground,
                        textAlign = TextAlign.Center,
                        textDirection = TextDirection.Rtl,
                        // Relative line height, so it shrinks together with auto-sized text.
                        lineHeight = 1.6.em,
                    ),
                    autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 38.sp, stepSize = 1.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
                }
                Text(
                    stringResource(R.string.zikr_source, zikr.reference),
                    style = MtvTheme.type.caption.copy(fontFamily = NaskhFamily),
                    color = MtvTheme.colors.onBackgroundMuted,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
            CountDots(counted, zikr.count)
            Text(
                if (zikr.count == 1) stringResource(R.string.zikr_count_once) else stringResource(R.string.zikr_count, f.number(zikr.count)),
                style = MtvTheme.type.body,
                color = MtvTheme.colors.goldSoft,
            )
            Box(Modifier.weight(1f))
            Text(stringResource(R.string.zikr_press_ok), style = MtvTheme.type.caption, color = MtvTheme.colors.onBackgroundMuted)
        }
    }
    LaunchedEffect(index) { card.requestFocusSafely() }
}

@Composable
private fun CountDots(done: Int, total: Int) {
    val f = rememberTimeFormatter()
    if (total > MAX_DOTS) {
        Text(stringResource(R.string.zikr_counter, f.number(done), f.number(total)), style = MtvTheme.type.title, color = MtvTheme.colors.gold)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            Box(Modifier.size(18.dp).background(if (i < done) MtvTheme.colors.gold else MtvTheme.colors.surfaceFocused, CircleShape))
        }
    }
}

@Composable
private fun CompletedPanel(onAgain: () -> Unit, onBack: () -> Unit) {
    val first = remember { FocusRequester() }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MtvSpacing.l, Alignment.CenterVertically)) {
        Text(stringResource(R.string.completed_title), style = MtvTheme.type.display, color = MtvTheme.colors.gold)
        Text(stringResource(R.string.completed_body), style = MtvTheme.type.title)
        Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
            MtvButton(stringResource(R.string.back), onBack, Modifier.focusRequester(first))
            MtvButton(stringResource(R.string.again), onAgain, style = ButtonStyle.Secondary, icon = Icons.Rounded.Refresh)
        }
    }
    LaunchedEffect(Unit) { first.requestFocusSafely() }
}

private val PHRASES = listOf("سُبْحَانَ اللَّهِ", "الْحَمْدُ لِلَّهِ", "اللَّهُ أَكْبَرُ", "لَا إِلَٰهَ إِلَّا اللَّهُ", "أَسْتَغْفِرُ اللَّهَ")

@Composable
fun TasbeehEntry(viewModel: TasbeehViewModel = hiltViewModel()) {
    val t by viewModel.tasbeeh.collectAsStateWithLifecycle()
    val phrase by viewModel.phrase.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    val counter = remember { FocusRequester() }
    val glow = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val c = MtvTheme.colors
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        TabChips(PHRASES, phrase, viewModel::setPhrase)
        // The big counter: OK counts.
        FocusCard(
            onClick = {
                viewModel.tap()
                if (viewModel.tasbeeh.value.justCompleted) scope.launch {
                    glow.snapTo(1f)
                    glow.animateTo(0f, tween(1_200))
                }
            },
            shape = CircleShape,
            focusedScale = 1.03f,
            modifier = Modifier.size(280.dp).focusRequester(counter),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(BorderStroke((2 + glow.value * 10).dp, c.gold.copy(alpha = 0.3f + glow.value * 0.7f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    QuranText(PHRASES[phrase], sizeSp = 30, lineHeightEm = 1.4f, color = c.goldSoft)
                    Text(f.number(if (t.target > 0) t.inRound else t.count), style = MtvTheme.type.numeric.copy(fontSize = 72.sp))
                    Text(
                        if (t.target > 0) "/ ${f.number(t.target)}" else stringResource(R.string.tasbeeh_hint),
                        style = MtvTheme.type.caption,
                        color = c.onBackgroundMuted,
                    )
                }
            }
        }
        if (t.rounds > 0) Text(stringResource(R.string.rounds, f.number(t.rounds)), style = MtvTheme.type.body, color = c.goldSoft)
        Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.target), style = MtvTheme.type.label, color = c.onBackgroundMuted)
            listOf(33, 100, 1000, 0).forEach { target ->
                MtvButton(
                    if (target == 0) stringResource(R.string.target_free) else f.number(target),
                    { viewModel.setTarget(target) },
                    style = if (t.target == target) ButtonStyle.Primary else ButtonStyle.Secondary,
                )
            }
            MtvButton(stringResource(R.string.reset), viewModel::reset, style = ButtonStyle.Secondary, icon = Icons.Rounded.Refresh)
        }
    }
    LaunchedEffect(Unit) { counter.requestFocusSafely() }
}

@Composable
fun AsmaEntry(viewModel: AzkarHomeViewModel = hiltViewModel()) {
    val names by viewModel.names.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    val focus = rememberFocusRestoreState()
    val arabic = isArabicUi()
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.asma))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(MtvSpacing.s),
            horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(names, key = { it.number }) { n ->
                FocusCard(onClick = { open = n.number }, modifier = Modifier.height(164.dp).restorableFocus(focus, "n${n.number}"), contentPadding = MtvSpacing.s) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                        QuranText(n.arabic, sizeSp = 30, lineHeightEm = 1.3f, color = MtvTheme.colors.goldSoft)
                        Text(n.transliteration, style = MtvTheme.type.bodyStrong, maxLines = 1)
                        if (!arabic) Text(n.english, style = MtvTheme.type.caption, color = MtvTheme.colors.onBackgroundMuted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        else Text(f.number(n.number), style = MtvTheme.type.caption, color = MtvTheme.colors.onBackgroundMuted)
                    }
                }
            }
        }
    }
    if (names.isNotEmpty()) RestoreFocusEffect(focus, "n1")
    open?.let { num ->
        val n = names.firstOrNull { it.number == num } ?: return@let
        val close = remember { FocusRequester() }
        DialogPanel({ open = null }) {
            QuranText(n.arabic, sizeSp = 56, color = MtvTheme.colors.gold, modifier = Modifier.fillMaxWidth())
            Text("${f.number(n.number)} · ${n.transliteration} · ${n.english}", style = MtvTheme.type.title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(n.explanation, style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
            MtvButton(stringResource(R.string.back), { open = null }, Modifier.focusRequester(close))
        }
        DialogInitialFocus(close)
    }
}

@Composable
fun RuqyahEntry(onPlaying: () -> Unit, onDownloads: () -> Unit, viewModel: RuqyahViewModel = hiltViewModel()) {
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    val focus = rememberFocusRestoreState()
    val arabic = isArabicUi()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.ruqyah), subtitle = stringResource(R.string.ruqyah_hint))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
            MtvButton(stringResource(R.string.play_all), { viewModel.playAll(onPlaying) }, Modifier.restorableFocus(focus, "play"), icon = Icons.Rounded.PlayArrow)
            if (offline) {
                Text(stringResource(R.string.ruqyah_offline), style = MtvTheme.type.body, color = MtvTheme.colors.error, modifier = Modifier.weight(1f, fill = false))
                MtvButton(stringResource(R.string.go_downloads), onDownloads, style = ButtonStyle.Secondary)
            }
        }
        LazyColumn(Modifier.widthIn(max = 1000.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs), contentPadding = PaddingValues(6.dp)) {
            items(viewModel.passages, key = { "${it.first}" }) { (from, to) ->
                val s = viewModel.surah(from.surah)
                val range = if (from.ayah == to.ayah) f.number(from.ayah) else "${f.number(from.ayah)}–${f.number(to.ayah)}"
                Row(
                    Modifier.fillMaxWidth().background(MtvTheme.colors.surface, MtvShapes.small).padding(horizontal = MtvSpacing.m, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (arabic) s.arabicName else s.transliteration, style = MtvTheme.type.bodyStrong, modifier = Modifier.weight(1f))
                    Text(range, style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
                }
            }
        }
    }
    RestoreFocusEffect(focus, "play")
}

private const val AUTO_ADVANCE_MS = 700L
private const val MAX_DOTS = 10
