package com.mfouad.muslimtv.feature.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.ButtonStyle
import com.mfouad.muslimtv.core.designsystem.component.FocusCard
import com.mfouad.muslimtv.core.designsystem.component.ListRow
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.ScreenHeader
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.AlertMode
import com.mfouad.muslimtv.core.model.Muezzins
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.ui.format
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.label
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import java.time.format.DateTimeFormatter

@Composable
fun MonthEntry(viewModel: PrayerExtrasViewModel = hiltViewModel()) {
    val days by viewModel.month.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter(days.firstOrNull()?.settings?.timeFormat ?: com.mfouad.muslimtv.core.model.TimeFormat.SYSTEM)
    val focus = rememberFocusRestoreState()
    val listState = rememberLazyListState()
    val first = days.firstOrNull() ?: return
    val todayIndex = days.indexOfFirst { it.date == java.time.LocalDate.now(first.zone) }
    val monthName = DateTimeFormatter.ofPattern("MMMM yyyy", f.locale).format(first.date)
    val prayers = Prayer.entries

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
            ScreenHeader(stringResource(R.string.month_title), Modifier.weight(1f), subtitle = "$monthName · ${first.city.displayName(isArabicUi())}")
            MtvButton(stringResource(R.string.month_prev), { viewModel.shiftMonth(-1) }, Modifier.restorableFocus(focus, "prev"), style = ButtonStyle.Secondary)
            MtvButton(stringResource(R.string.month_next), { viewModel.shiftMonth(1) }, Modifier.restorableFocus(focus, "next"), style = ButtonStyle.Secondary)
        }
        // Header row
        Row(Modifier.fillMaxWidth().padding(horizontal = MtvSpacing.m)) {
            Text(stringResource(R.string.date_col), style = MtvTheme.type.caption, color = MtvTheme.colors.gold, modifier = Modifier.weight(1.6f))
            prayers.forEach { Text(it.label(), style = MtvTheme.type.caption, color = MtvTheme.colors.gold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
        }
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(days, key = { it.date.toString() }) { d ->
                val today = d.date == java.time.LocalDate.now(d.zone)
                // Rows are focusable so the remote can scroll the month; there is nothing to open.
                FocusCard(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().restorableFocus(focus, d.date.toString()),
                    shape = MtvShapes.small,
                    focusedScale = 1.01f,
                    contentPadding = 0.dp,
                    containerColor = if (today) MtvTheme.colors.primary.copy(alpha = 0.35f) else MtvTheme.colors.surface,
                ) {
                    Row(Modifier.padding(horizontal = MtvSpacing.m, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1.6f)) {
                            Text(f.gregorianShort(d.date), style = MtvTheme.type.caption, color = if (today) MtvTheme.colors.gold else MtvTheme.colors.onBackground)
                            Text(d.hijri.format(), style = MtvTheme.type.caption, color = MtvTheme.colors.onBackgroundMuted, maxLines = 1)
                        }
                        prayers.forEach { p ->
                            Text(f.timeParts(d.today[p], d.zone).first, style = MtvTheme.type.caption, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(todayIndex) { if (todayIndex > 2) listState.scrollToItem(todayIndex - 2) }
    RestoreFocusEffect(focus, if (todayIndex >= 0) days[todayIndex].date.toString() else first.date.toString())
}

@Composable
fun AdhanSettingsEntry(viewModel: PrayerExtrasViewModel = hiltViewModel()) {
    val adhan by viewModel.adhan.collectAsStateWithLifecycle()
    val previewing by viewModel.previewing.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    val arabic = isArabicUi()
    val focus = rememberFocusRestoreState()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.adhan_settings), subtitle = stringResource(R.string.adhan_settings_hint))
        LazyColumn(Modifier.widthIn(max = 1100.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs), contentPadding = PaddingValues(6.dp)) {
            if (!viewModel.exactAlarmsAllowed) item(key = "warn") {
                Text(stringResource(R.string.exact_alarm_warning), style = MtvTheme.type.body, color = MtvTheme.colors.goldSoft)
            }
            items(Prayer.entries.filter { it.isObligatory }, key = { it.name }) { p ->
                Row(
                    Modifier.fillMaxWidth().background(MtvTheme.colors.surface, MtvShapes.small).padding(horizontal = MtvSpacing.m, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s),
                ) {
                    Text(p.label(), style = MtvTheme.type.bodyStrong, modifier = Modifier.weight(0.7f))
                    // OK cycles: adhan on screen → silent notification → off.
                    MtvButton(
                        stringResource(
                            when (adhan.mode(p)) {
                                AlertMode.ADHAN -> R.string.mode_adhan
                                AlertMode.NOTIFICATION -> R.string.mode_notification
                                AlertMode.OFF -> R.string.mode_off
                            },
                        ),
                        { viewModel.cycleMode(p) },
                        Modifier.restorableFocus(focus, "m${p.name}"),
                        style = if (adhan.mode(p) == AlertMode.OFF) ButtonStyle.Secondary else ButtonStyle.Primary,
                    )
                    Text(stringResource(R.string.iqamah_minutes, f.number(adhan.iqamah(p))), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                    com.mfouad.muslimtv.core.designsystem.component.IconButton(Icons.Rounded.Remove, stringResource(R.string.iqamah_setting), { viewModel.changeIqamah(p, -1) }, Modifier.restorableFocus(focus, "i-${p.name}"))
                    com.mfouad.muslimtv.core.designsystem.component.IconButton(Icons.Rounded.Add, stringResource(R.string.iqamah_setting), { viewModel.changeIqamah(p, 1) }, Modifier.restorableFocus(focus, "i+${p.name}"))
                }
            }
            items(Muezzins.all, key = { "mz" + it.id }) { m ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
                    com.mfouad.muslimtv.core.designsystem.component.ChoiceRow(
                        title = stringResource(R.string.muezzin) + " · " + if (arabic) m.nameAr else m.nameEn,
                        selected = adhan.muezzinId == m.id,
                        onClick = { viewModel.setMuezzin(m.id) },
                        modifier = Modifier.weight(1f).restorableFocus(focus, "mz" + m.id),
                    )
                    MtvButton(
                        stringResource(if (previewing) R.string.muezzin_stop else R.string.muezzin_preview),
                        { viewModel.togglePreview(m.id) },
                        Modifier.restorableFocus(focus, "pv" + m.id),
                        style = ButtonStyle.Secondary,
                    )
                }
            }
            item(key = "overlay") {
                val allowed by viewModel.overlay.collectAsStateWithLifecycle()
                var noScreen by remember { mutableStateOf(false) }
                LifecycleResumeEffect(Unit) { viewModel.refreshOverlay(); onPauseOrDispose { } }
                ListRow(
                    title = stringResource(R.string.overlay_title),
                    subtitle = stringResource(
                        when {
                            allowed -> R.string.overlay_on
                            noScreen -> R.string.overlay_manual
                            else -> R.string.overlay_off
                        },
                    ),
                    value = stringResource(if (allowed) R.string.on else R.string.off),
                    onClick = { if (!allowed) noScreen = !viewModel.requestOverlay() },
                    modifier = Modifier.restorableFocus(focus, "overlay"),
                    showChevron = !allowed,
                )
            }
            item(key = "test") {
                val scheduled by viewModel.testScheduled.collectAsStateWithLifecycle()
                ListRow(
                    title = stringResource(R.string.test_adhan),
                    subtitle = stringResource(if (scheduled) R.string.test_adhan_scheduled else R.string.test_adhan_hint),
                    onClick = viewModel::testAdhan,
                    modifier = Modifier.restorableFocus(focus, "test"),
                    showChevron = false,
                )
            }
            item(key = "boot") {
                ListRow(
                    title = stringResource(R.string.start_on_boot),
                    value = stringResource(if (adhan.startOnBoot) R.string.on else R.string.off),
                    onClick = viewModel::toggleStartOnBoot,
                    modifier = Modifier.restorableFocus(focus, "boot"),
                    showChevron = false,
                )
            }
        }
    }
    RestoreFocusEffect(focus, "m${Prayer.FAJR.name}")
}
