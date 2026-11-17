package com.mfouad.muslimtv.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.FocusCard
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.OfflineBadge
import com.mfouad.muslimtv.core.designsystem.component.QuranText
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.AzkarCategory
import com.mfouad.muslimtv.core.ui.TimeFormatter
import com.mfouad.muslimtv.core.ui.format
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.label
import com.mfouad.muslimtv.core.ui.labelRes
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import com.mfouad.muslimtv.core.ui.R as UiR

@Composable
fun HomeEntry(
    onOpenPrayer: () -> Unit,
    onContinueQuran: (AyahRef) -> Unit,
    onOpenAzkar: (AzkarCategory) -> Unit,
    onOpenAyah: (AyahRef) -> Unit,
    onChooseCity: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now = viewModel.now.collectAsStateWithLifecycle()
    HomeScreen(state, now, onOpenPrayer, onContinueQuran, onOpenAzkar, onOpenAyah, onChooseCity)
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    now: State<Long>,
    onOpenPrayer: () -> Unit,
    onContinueQuran: (AyahRef) -> Unit,
    onOpenAzkar: (AzkarCategory) -> Unit,
    onOpenAyah: (AyahRef) -> Unit,
    onChooseCity: () -> Unit,
) {
    if (!state.loaded) return // Settings load in a few ms; avoid a flash of empty cards.
    val f = rememberTimeFormatter(state.schedule?.settings?.timeFormat ?: com.mfouad.muslimtv.core.model.TimeFormat.SYSTEM)
    val focus = rememberFocusRestoreState()
    val schedule = state.schedule

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        Header(state, now, f, onOpenAyah, focus)
        if (schedule == null) {
            Column(verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                Text(stringResource(R.string.home_no_city), style = MtvTheme.type.body)
                MtvButton(stringResource(R.string.home_choose_city), onChooseCity, Modifier.restorableFocus(focus, KEY_PRAYER))
            }
            RestoreFocusEffect(focus, KEY_PRAYER)
            return@Column
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
            NextPrayerCard(state, now, f, onOpenPrayer, Modifier.weight(1.25f).fillMaxHeight().restorableFocus(focus, KEY_PRAYER))
            ContinueCard(state, f, { onContinueQuran(AyahRef(state.continueSurah?.number ?: 1, state.continueAyah)) }, Modifier.weight(1f).fillMaxHeight().restorableFocus(focus, KEY_CONTINUE))
            state.khatmahPortion?.let { portion ->
                KhatmahCard(state, portion, f, { onContinueQuran(portion.start) }, Modifier.weight(1f).fillMaxHeight().restorableFocus(focus, KEY_KHATMAH))
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
            AzkarCard(state.azkar, { onOpenAzkar(state.azkar) }, Modifier.weight(1f).fillMaxHeight().restorableFocus(focus, KEY_AZKAR))
            AyahCard(state, f, onOpenAyah, Modifier.weight(1.25f).fillMaxHeight().restorableFocus(focus, KEY_AYAH))
        }
    }
    if (schedule != null) RestoreFocusEffect(focus, KEY_PRAYER)
}

@Composable
private fun Header(state: HomeUiState, now: State<Long>, f: TimeFormatter, onOpenAyah: (AyahRef) -> Unit, focus: com.mfouad.muslimtv.core.designsystem.focus.FocusRestoreState) {
    val schedule = state.schedule
    val arabic = isArabicUi()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (schedule != null) {
                Text(schedule.hijri.format(), style = MtvTheme.type.title, color = MtvTheme.colors.gold)
                Text(
                    "${f.gregorian(schedule.date)}  ·  ${schedule.city.displayName(arabic)}",
                    style = MtvTheme.type.body,
                    color = MtvTheme.colors.onBackgroundMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        state.reminder?.let { r ->
            MtvButton(
                stringResource(if (r == SurahReminder.KAHF) R.string.reminder_kahf else R.string.reminder_mulk),
                { onOpenAyah(AyahRef(r.surah, 1)) },
                Modifier.padding(end = MtvSpacing.m).restorableFocus(focus, "reminder"),
                style = com.mfouad.muslimtv.core.designsystem.component.ButtonStyle.Secondary,
                icon = Icons.AutoMirrored.Rounded.MenuBook,
            )
        }
        if (!state.isOnline) OfflineBadge(stringResource(UiR.string.offline), Modifier.padding(end = MtvSpacing.m))
        if (schedule != null) Clock(now, f, schedule.zone)
    }
}

@Composable
private fun Clock(now: State<Long>, f: TimeFormatter, zone: java.time.ZoneId) {
    val (time, suffix) = f.clock(now.value, zone)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(time, style = MtvTheme.type.numeric)
        if (suffix != null) Text(suffix, style = MtvTheme.type.label, color = MtvTheme.colors.onBackgroundMuted, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
    }
}

@Composable
private fun CardLabel(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, Modifier.size(26.dp), tint = MtvTheme.colors.gold)
        Text(text, style = MtvTheme.type.label, color = MtvTheme.colors.onBackgroundMuted, maxLines = 1)
    }
}

@Composable
private fun NextPrayerCard(state: HomeUiState, now: State<Long>, f: TimeFormatter, onClick: () -> Unit, modifier: Modifier) {
    val schedule = state.schedule ?: return
    val next = state.next ?: return
    if (state.ramadan) {
        RamadanCard(state, now, f, onClick, modifier)
        return
    }
    FocusCard(onClick = onClick, modifier = modifier, contentPadding = MtvSpacing.m) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            CardLabel(Icons.Rounded.Schedule, stringResource(R.string.home_next_prayer) + if (next.isTomorrow) " · " + stringResource(R.string.home_tomorrow) else "")
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                Column(Modifier.weight(1f)) {
                    Text(next.prayer.label(if (next.isTomorrow) schedule.date.plusDays(1) else schedule.date), style = MtvTheme.type.headline.copy(fontSize = 34.sp), maxLines = 1, softWrap = false)
                    Text(f.time(next.atMillis, schedule.zone), style = MtvTheme.type.bodyStrong, color = MtvTheme.colors.goldSoft)
                }
                Text(
                    f.countdown(next.atMillis - now.value),
                    style = MtvTheme.type.numeric.copy(fontSize = 46.sp, lineHeight = 52.sp),
                    color = MtvTheme.colors.onBackground,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ContinueCard(state: HomeUiState, f: TimeFormatter, onClick: () -> Unit, modifier: Modifier) {
    val arabic = isArabicUi()
    FocusCard(onClick = onClick, modifier = modifier, contentPadding = MtvSpacing.m) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            CardLabel(
                Icons.AutoMirrored.Rounded.MenuBook,
                stringResource(
                    when {
                        state.continueIsListening -> R.string.home_continue_listening
                        state.hasReadingHistory -> R.string.home_continue_reading
                        else -> R.string.home_start_reading
                    },
                ),
            )
            val surah = state.continueSurah
            if (surah != null) {
                Column {
                    QuranText(surah.arabicName, sizeSp = 36, lineHeightEm = 1.3f, textAlign = TextAlign.Start, color = MtvTheme.colors.goldSoft, modifier = Modifier.fillMaxWidth())
                    Text(
                        stringResource(
                            R.string.home_continue_position,
                            if (arabic) surah.arabicName else surah.transliteration,
                            f.number(state.continueAyah),
                        ),
                        style = MtvTheme.type.body,
                        color = MtvTheme.colors.onBackgroundMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun AzkarCard(category: AzkarCategory, onClick: () -> Unit, modifier: Modifier) {
    FocusCard(onClick = onClick, modifier = modifier, contentPadding = MtvSpacing.m) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            CardLabel(Icons.Rounded.AutoAwesome, stringResource(R.string.home_suggested_now))
            Column {
                Text(stringResource(category.labelRes()), style = MtvTheme.type.title)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.WbTwilight, null, Modifier.size(24.dp), tint = MtvTheme.colors.onBackgroundMuted)
                    Text(stringResource(R.string.home_azkar_open), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
                }
            }
        }
    }
}

@Composable
private fun AyahCard(state: HomeUiState, f: TimeFormatter, onOpen: (AyahRef) -> Unit, modifier: Modifier) {
    val ayah = state.ayahOfTheDay
    val arabic = isArabicUi()
    FocusCard(
        onClick = { ayah?.let { onOpen(AyahRef(it.ayah.surah, it.ayah.ayah)) } },
        modifier = modifier,
        contentPadding = MtvSpacing.m,
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            CardLabel(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.home_ayah_of_day))
            if (ayah != null) {
                QuranText(ayah.ayah.text, sizeSp = 32, minSizeSp = 22, lineHeightEm = 1.7f, modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp))
                Text(
                    stringResource(
                        R.string.home_ayah_ref,
                        if (arabic) ayah.surah.arabicName else ayah.surah.transliteration,
                        f.number(ayah.ayah.ayah),
                    ),
                    style = MtvTheme.type.caption,
                    color = MtvTheme.colors.onBackgroundMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** In Ramadan: countdown to iftar (Maghrib), or after it to the end of suhoor (next Fajr). */
@Composable
private fun RamadanCard(state: HomeUiState, now: State<Long>, f: TimeFormatter, onClick: () -> Unit, modifier: Modifier) {
    val s = state.schedule ?: return
    val maghrib = s.today[com.mfouad.muslimtv.core.model.Prayer.MAGHRIB]
    val beforeIftar = now.value < maghrib
    val target = if (beforeIftar) maghrib else s.tomorrow[com.mfouad.muslimtv.core.model.Prayer.FAJR]
    FocusCard(onClick = onClick, modifier = modifier, contentPadding = MtvSpacing.m) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            CardLabel(Icons.Rounded.NightsStay, stringResource(R.string.ramadan_day, f.number(s.hijri.day)))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(if (beforeIftar) R.string.iftar else R.string.suhoor_ends), style = MtvTheme.type.headline, maxLines = 1)
                    Text(f.time(target, s.zone), style = MtvTheme.type.bodyStrong, color = MtvTheme.colors.goldSoft)
                }
                Text(f.countdown(target - now.value), style = MtvTheme.type.numeric.copy(fontSize = 46.sp, lineHeight = 52.sp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun KhatmahCard(state: HomeUiState, portion: com.mfouad.muslimtv.core.model.KhatmahPortion, f: TimeFormatter, onClick: () -> Unit, modifier: Modifier) {
    val plan = state.khatmah ?: return
    FocusCard(onClick = onClick, modifier = modifier, contentPadding = MtvSpacing.m) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            CardLabel(Icons.Rounded.AutoStories, stringResource(R.string.khatmah_card, f.number(plan.completed + 1), f.number(plan.days)))
            Column {
                Text(stringResource(R.string.khatmah_pages, f.number(portion.pages.first), f.number(portion.pages.last)), style = MtvTheme.type.title, maxLines = 1)
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(8.dp).background(MtvTheme.colors.surfaceFocused, com.mfouad.muslimtv.core.designsystem.theme.MtvShapes.pill)) {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxHeight().fillMaxWidth(plan.progress).background(MtvTheme.colors.gold, com.mfouad.muslimtv.core.designsystem.theme.MtvShapes.pill))
                }
            }
        }
    }
}

private const val KEY_KHATMAH = "khatmah"
private const val KEY_PRAYER = "prayer"
private const val KEY_CONTINUE = "continue"
private const val KEY_AZKAR = "azkar"
private const val KEY_AYAH = "ayah"
