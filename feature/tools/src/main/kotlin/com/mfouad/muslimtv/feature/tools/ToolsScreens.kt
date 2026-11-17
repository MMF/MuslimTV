package com.mfouad.muslimtv.feature.tools

import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.data.tools.FastingNote
import com.mfouad.muslimtv.core.data.tools.Occasion
import com.mfouad.muslimtv.core.data.tools.QiblaCalculator
import com.mfouad.muslimtv.core.designsystem.component.ButtonStyle
import com.mfouad.muslimtv.core.designsystem.component.FocusCard
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
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
import com.mfouad.muslimtv.core.ui.format
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter

private data class ToolTile(val key: String, val title: Int, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun ToolsHomeEntry(
    onCalendar: () -> Unit, onQibla: () -> Unit, onHadith: () -> Unit, onNawawi: () -> Unit,
    onLive: () -> Unit, onKids: () -> Unit, onHajj: () -> Unit,
) {
    val focus = rememberFocusRestoreState()
    val tiles = listOf(
        ToolTile("cal", R.string.t_calendar, Icons.Rounded.CalendarMonth, onCalendar),
        ToolTile("qibla", R.string.t_qibla, Icons.Rounded.Explore, onQibla),
        ToolTile("hadith", R.string.t_hadith, Icons.Rounded.FormatQuote, onHadith),
        ToolTile("nawawi", R.string.t_nawawi, Icons.AutoMirrored.Rounded.MenuBook, onNawawi),
        ToolTile("live", R.string.t_live, Icons.Rounded.LiveTv, onLive),
        ToolTile("kids", R.string.t_kids, Icons.Rounded.ChildCare, onKids),
        ToolTile("hajj", R.string.t_hajj, Icons.Rounded.Mosque, onHajj),
    )
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.tools_title))
        LazyVerticalGrid(GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m), horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m), contentPadding = PaddingValues(8.dp)) {
            items(tiles, key = { it.key }) { t ->
                FocusCard(onClick = t.onClick, modifier = Modifier.height(140.dp).restorableFocus(focus, t.key), contentPadding = MtvSpacing.m) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Icon(t.icon, null, Modifier.size(34.dp), tint = MtvTheme.colors.gold)
                        Text(stringResource(t.title), style = MtvTheme.type.title.copy(fontSize = 26.sp, lineHeight = 32.sp), maxLines = 2)
                    }
                }
            }
        }
    }
    RestoreFocusEffect(focus, "cal")
}

@Composable
fun CalendarEntry(viewModel: CalendarViewModel = hiltViewModel()) {
    val days by viewModel.days.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    val focus = rememberFocusRestoreState()
    val first = days.firstOrNull() ?: return
    val c = MtvTheme.colors
    val title = first.hijri.copy(day = 1).format().replaceFirst(Regex("^\\S+\\s"), "")
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
            ScreenHeader(title, Modifier.weight(1f), subtitle = "${f.gregorianShort(first.gregorian)} – ${f.gregorianShort(days.last().gregorian)}")
            MtvButton(stringResource(R.string.cal_prev), { viewModel.shift(-1) }, Modifier.restorableFocus(focus, "prev"), style = ButtonStyle.Secondary)
            MtvButton(stringResource(R.string.cal_next), { viewModel.shift(1) }, Modifier.restorableFocus(focus, "next"), style = ButtonStyle.Secondary)
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MtvSpacing.l)) {
            // Month grid: 7 columns, starting on the right weekday.
            val lead = (first.gregorian.dayOfWeek.value % 7) // Sunday = 0
            val cells: List<CalendarDay?> = List(lead) { null } + days
            LazyVerticalGrid(GridCells.Fixed(7), Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Weekday headings, Sunday first, in the UI language.
                items(7) { d ->
                    Text(
                        java.time.DayOfWeek.of(if (d == 0) 7 else d).getDisplayName(java.time.format.TextStyle.SHORT, f.locale),
                        style = MtvTheme.type.caption.copy(fontSize = 16.sp),
                        color = c.onBackgroundMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                items(cells.size) { i ->
                    val d = cells[i]
                    if (d == null) Box(Modifier.height(52.dp)) else {
                        FocusCard(
                            onClick = {},
                            modifier = Modifier.height(52.dp).restorableFocus(focus, d.gregorian.toString()),
                            shape = MtvShapes.small,
                            focusedScale = 1.06f,
                            contentPadding = 4.dp,
                            containerColor = when {
                                d.isToday -> c.primary.copy(alpha = 0.6f)
                                d.occasions.isNotEmpty() -> c.gold.copy(alpha = 0.25f)
                                else -> c.surface
                            },
                        ) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(f.number(d.hijri.day), style = MtvTheme.type.bodyStrong.copy(lineHeight = 26.sp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(f.number(d.gregorian.dayOfMonth), style = MtvTheme.type.caption.copy(fontSize = 16.sp), color = c.onBackgroundMuted)
                                    if (d.fasting == FastingNote.RECOMMENDED) Box(Modifier.size(8.dp).background(c.success, CircleShape))
                                }
                            }
                        }
                    }
                }
            }
            // Occasions and fasting this month.
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs)) {
                Text(stringResource(R.string.cal_occasions), style = MtvTheme.type.label, color = c.gold)
                val notable = days.filter { it.occasions.isNotEmpty() }
                if (notable.isEmpty()) Text(stringResource(R.string.cal_none), style = MtvTheme.type.body, color = c.onBackgroundMuted)
                notable.distinctBy { it.occasions }.take(6).forEach { d ->
                    Text("${f.number(d.hijri.day)} · ${d.occasions.map { occasionLabel(it) }.joinToString(" · ")}", style = MtvTheme.type.body)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = MtvSpacing.s)) {
                    Box(Modifier.size(12.dp).background(c.success, CircleShape))
                    Text(stringResource(R.string.cal_fast), style = MtvTheme.type.caption, color = c.onBackgroundMuted)
                }
            }
        }
    }
    RestoreFocusEffect(focus, days.firstOrNull { it.isToday }?.gregorian?.toString() ?: first.gregorian.toString())
}

@Composable
private fun occasionLabel(o: Occasion) = stringResource(
    when (o) {
        Occasion.HIJRI_NEW_YEAR -> R.string.occ_new_year
        Occasion.ASHURA -> R.string.occ_ashura
        Occasion.RAMADAN_START -> R.string.occ_ramadan
        Occasion.LAST_TEN_NIGHTS -> R.string.occ_last_ten
        Occasion.EID_AL_FITR -> R.string.occ_eid_fitr
        Occasion.DAY_OF_ARAFAH -> R.string.occ_arafah
        Occasion.EID_AL_ADHA -> R.string.occ_eid_adha
        Occasion.TASHREEQ -> R.string.occ_tashreeq
        Occasion.DHUL_HIJJAH_TEN -> R.string.occ_dhul_hijjah
    },
)

@Composable
fun QiblaEntry(viewModel: QiblaViewModel = hiltViewModel()) {
    val city by viewModel.city.collectAsStateWithLifecycle()
    val rings by viewModel.rings.collectAsStateWithLifecycle()
    val cty = city ?: return
    val q = QiblaCalculator.forCity(cty)
    val f = rememberTimeFormatter()
    val arabic = isArabicUi()
    val c = MtvTheme.colors
    val focus = remember { FocusRequester() }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MtvSpacing.l), verticalAlignment = Alignment.CenterVertically) {
        // World map (equirectangular) with the shortest route to Makkah.
        FocusCard(onClick = {}, focusedScale = 1f, modifier = Modifier.weight(1.4f).aspectRatio(2f).focusRequester(focus), contentPadding = 8.dp) {
            Canvas(Modifier.fillMaxSize()) {
                fun x(lng: Double) = ((lng + 180) / 360 * size.width).toFloat()
                fun y(lat: Double) = ((90 - lat) / 180 * size.height).toFloat()
                rings.forEach { r ->
                    val p = Path()
                    var i = 0
                    while (i + 1 < r.size) {
                        val px = x(r[i].toDouble()); val py = y(r[i + 1].toDouble())
                        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                        i += 2
                    }
                    p.close()
                    drawPath(p, c.primary.copy(alpha = 0.55f))
                }
                // Break the path where it crosses the date line.
                val route = Path()
                q.path.forEachIndexed { i, (lat, lng) ->
                    val prev = q.path.getOrNull(i - 1)
                    if (i == 0 || (prev != null && kotlin.math.abs(prev.second - lng) > 180)) route.moveTo(x(lng), y(lat)) else route.lineTo(x(lng), y(lat))
                }
                drawPath(route, c.gold, style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
                drawCircle(c.onBackground, 8f, Offset(x(cty.longitude), y(cty.latitude)))
                drawCircle(c.gold, 10f, Offset(x(QiblaCalculator.KAABA_LNG), y(QiblaCalculator.KAABA_LAT)))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
            Text(stringResource(R.string.t_qibla), style = MtvTheme.type.headline)
            Text(stringResource(R.string.qibla_from, cty.displayName(arabic)), style = MtvTheme.type.body, color = c.onBackgroundMuted)
            // Compass dial showing the bearing from north.
            Canvas(Modifier.size(170.dp).padding(8.dp)) {
                val r = size.minDimension / 2
                drawCircle(c.surfaceFocused, r)
                drawCircle(c.gold.copy(alpha = 0.6f), r, style = Stroke(3f))
                val a = Math.toRadians(q.bearingDegrees - 90)
                val end = Offset(center.x + (r * 0.85f * kotlin.math.cos(a)).toFloat(), center.y + (r * 0.85f * kotlin.math.sin(a)).toFloat())
                drawLine(c.gold, center, end, strokeWidth = 8f)
                drawCircle(c.onBackground, 6f, center)
                drawLine(c.onBackgroundMuted, Offset(center.x, center.y - r), Offset(center.x, center.y - r * 0.8f), strokeWidth = 4f)
            }
            Text(stringResource(R.string.qibla_bearing, String.format(f.locale, "%.0f", q.bearingDegrees)), style = MtvTheme.type.title, color = c.gold)
            Text(stringResource(R.string.qibla_point, QiblaCalculator.compassPoint(q.bearingDegrees)), style = MtvTheme.type.body)
            Text(stringResource(R.string.qibla_distance, String.format(f.locale, "%,.0f", q.distanceKm)), style = MtvTheme.type.body, color = c.onBackgroundMuted)
            Text(stringResource(R.string.qibla_hint), style = MtvTheme.type.caption, color = c.onBackgroundMuted)
        }
    }
    LaunchedEffect(Unit) { focus.requestFocusSafely() }
}

/** Arabic text card that never cuts the text: it shrinks to fit instead. */
@Composable
private fun ArabicCard(text: String, source: String, modifier: Modifier, onKey: ((Int) -> Unit)? = null) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    FocusCard(
        onClick = { onKey?.invoke(1) },
        focusedScale = 1f,
        modifier = modifier.onPreviewKeyEvent { e ->
            if (onKey == null || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (e.key) {
                (if (rtl) Key.DirectionLeft else Key.DirectionRight) -> { onKey(1); true }
                (if (rtl) Key.DirectionRight else Key.DirectionLeft) -> { onKey(-1); true }
                else -> false
            }
        },
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                BasicText(
                    text,
                    style = MtvTheme.type.headline.copy(fontFamily = NaskhFamily, color = MtvTheme.colors.onBackground, textAlign = TextAlign.Center, textDirection = TextDirection.Rtl, lineHeight = 1.6.em),
                    autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 36.sp, stepSize = 2.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(source, style = MtvTheme.type.caption.copy(fontFamily = NaskhFamily), color = MtvTheme.colors.goldSoft)
        }
    }
}

@Composable
fun HadithEntry(onNawawi: () -> Unit, viewModel: HadithViewModel = hiltViewModel()) {
    val h by viewModel.today.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    val arabic = isArabicUi()
    val focus = remember { FocusRequester() }
    val hadith = h ?: return
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.t_hadith), style = MtvTheme.type.headline, modifier = Modifier.weight(1f))
            MtvButton(stringResource(R.string.hadith_open_nawawi), onNawawi, style = ButtonStyle.Secondary)
        }
        ArabicCard(hadith.text, stringResource(R.string.hadith_number, if (arabic) hadith.collection else hadith.collectionEn, f.number(hadith.number)), Modifier.weight(1f).fillMaxWidth().focusRequester(focus))
    }
    LaunchedEffect(Unit) { focus.requestFocusSafely() }
}

@Composable
fun NawawiEntry(viewModel: HadithViewModel = hiltViewModel()) {
    val list by viewModel.nawawi.collectAsStateWithLifecycle()
    val index by viewModel.index.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    val focus = remember { FocusRequester() }
    val h = list.getOrNull(index) ?: return
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.t_nawawi), style = MtvTheme.type.headline, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.nawawi_count, f.number(index + 1), f.number(list.size)), style = MtvTheme.type.label, color = MtvTheme.colors.onBackgroundMuted)
        }
        ArabicCard(h.text, "${h.collection} · ${f.number(h.number)}", Modifier.weight(1f).fillMaxWidth().focusRequester(focus)) { viewModel.go(it) }
    }
    LaunchedEffect(Unit) { focus.requestFocusSafely() }
}

@Composable
fun LiveListEntry(onPlay: (String) -> Unit, viewModel: LiveListViewModel = hiltViewModel()) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    val arabic = isArabicUi()
    val focus = rememberFocusRestoreState()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
        ScreenHeader(stringResource(R.string.t_live), subtitle = if (!online) stringResource(R.string.offline_disabled) else null)
        LazyColumn(Modifier.widthIn(max = 1100.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs), contentPadding = PaddingValues(6.dp)) {
            item(key = "h1") { Text(stringResource(R.string.live_section), style = MtvTheme.type.label, color = MtvTheme.colors.gold) }
            items(config.live, key = { it.id }) { s ->
                com.mfouad.muslimtv.core.designsystem.component.ListRow(
                    s.name(arabic), { onPlay(s.id) }, Modifier.restorableFocus(focus, s.id),
                    icon = Icons.Rounded.LiveTv, enabled = online,
                )
            }
            item(key = "h2") { Text(stringResource(R.string.radio_section), style = MtvTheme.type.label, color = MtvTheme.colors.gold, modifier = Modifier.padding(top = MtvSpacing.s)) }
            items(config.radio, key = { it.id }) { s ->
                com.mfouad.muslimtv.core.designsystem.component.ListRow(
                    s.name(arabic), { onPlay(s.id) }, Modifier.restorableFocus(focus, s.id),
                    icon = Icons.Rounded.Radio, enabled = online,
                )
            }
        }
    }
    if (config.live.isNotEmpty()) RestoreFocusEffect(focus, config.live.first().id, enabled = online)
}

@OptIn(UnstableApi::class)
@Composable
fun LivePlayerEntry(viewModel: LivePlayerViewModel = hiltViewModel()) {
    val stream by viewModel.stream.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val arabic = isArabicUi()
    val retry = remember { FocusRequester() }
    val s = stream ?: return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (s.video) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.player
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                Icon(Icons.Rounded.Radio, null, Modifier.size(96.dp), tint = MtvTheme.colors.gold)
                Text(s.name(arabic), style = MtvTheme.type.headline)
            }
        }
        if (error) {
            Column(Modifier.background(MtvTheme.colors.scrim, MtvShapes.card).padding(MtvSpacing.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                Text(stringResource(R.string.live_error), style = MtvTheme.type.body)
                MtvButton(stringResource(R.string.retry), viewModel::play, Modifier.focusRequester(retry))
            }
            LaunchedEffect(Unit) { retry.requestFocusSafely() }
        }
    }
}

/** Step-by-step cards: OK or Right goes on, Left goes back (mirrored in Arabic). */
@Composable
fun StepPager(steps: List<String>, modifier: Modifier = Modifier) {
    var i by rememberSaveable(steps) { mutableIntStateOf(0) }
    val f = rememberTimeFormatter()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val c = MtvTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
        FocusCard(
            onClick = { if (i < steps.lastIndex) i++ },
            focusedScale = 1f,
            modifier = Modifier.weight(1f).fillMaxWidth().onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    (if (rtl) Key.DirectionLeft else Key.DirectionRight) -> { if (i < steps.lastIndex) i++; true }
                    (if (rtl) Key.DirectionRight else Key.DirectionLeft) -> { if (i > 0) { i--; true } else false }
                    else -> false
                }
            },
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.step_of, f.number(i + 1), f.number(steps.size)), style = MtvTheme.type.label, color = c.gold)
                Text(steps[i], style = MtvTheme.type.headline.copy(fontSize = 34.sp, lineHeight = 46.sp), modifier = Modifier.padding(top = MtvSpacing.m))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            steps.indices.forEach { k -> Box(Modifier.size(width = if (k == i) 28.dp else 10.dp, height = 10.dp).background(if (k <= i) c.gold else c.surfaceFocused, MtvShapes.pill)) }
        }
    }
}

@Composable
private fun DuaChapters(titles: List<String>, modifier: Modifier, viewModel: DuaListViewModel = hiltViewModel()) {
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    LaunchedEffect(titles) { viewModel.load(titles) }
    val items = chapters.flatMap { ch -> ch.items.take(2).map { ch.title to it } }
    if (items.isEmpty()) return
    var i by rememberSaveable { mutableIntStateOf(0) }
    val (title, zikr) = items[i.coerceIn(items.indices)]
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
        Text(title, style = MtvTheme.type.title.copy(fontFamily = NaskhFamily), color = MtvTheme.colors.gold)
        ArabicCard(zikr.text, zikr.reference, Modifier.weight(1f).fillMaxWidth()) { d -> i = (i + d).coerceIn(items.indices) }
    }
}

private val KIDS_DUAS = listOf("الدعاءقبلالطعام", "الدعاءعندالفراغمنالطعام", "دعاءدخولالخلاء", "دعاءالخروجمنالخلاء", "الذكرعندالخروجمنالمنزل", "الذكرعنددخولالمنزل", "دعاءالعطاس", "دعاءالركوب")
private val HAJJ_DUAS = listOf("كيفيلبيالمحرمفيالحجأوالعمرة", "التكبيرإذاأتىالركنالأسود", "الدعاءبينالركناليمانيوالحجرالأسود", "دعاءالوقوفعلىالصفاوالمروة", "الدعاءيومعرفة", "الذكرعندالمشعرالحرام", "التكبيرعندرميالجمارمعكلحصاة", "دعاءالسفر")

@Composable
fun KidsEntry() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        TabChips(listOf(stringResource(R.string.kids_wudu), stringResource(R.string.kids_prayer), stringResource(R.string.kids_duas)), tab, { tab = it })
        when (tab) {
            0 -> StepPager(stringArrayResource(R.array.wudu_steps).toList(), Modifier.weight(1f))
            1 -> StepPager(stringArrayResource(R.array.prayer_steps).toList(), Modifier.weight(1f))
            else -> DuaChapters(KIDS_DUAS, Modifier.weight(1f))
        }
    }
}

@Composable
fun HajjEntry() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        TabChips(listOf(stringResource(R.string.hajj_umrah), stringResource(R.string.hajj_hajj), stringResource(R.string.hajj_duas)), tab, { tab = it })
        when (tab) {
            0 -> StepPager(stringArrayResource(R.array.umrah_steps).toList(), Modifier.weight(1f))
            1 -> StepPager(stringArrayResource(R.array.hajj_steps).toList(), Modifier.weight(1f))
            else -> DuaChapters(HAJJ_DUAS, Modifier.weight(1f))
        }
        Text(stringResource(R.string.guide_note), style = MtvTheme.type.caption, color = MtvTheme.colors.onBackgroundMuted)
    }
}

