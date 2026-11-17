package com.mfouad.muslimtv.feature.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.data.prayer.PrayerTimesCalculator
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.ScreenHeader
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.ui.format
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.label
import com.mfouad.muslimtv.core.ui.labelRes
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import kotlinx.serialization.Serializable

@Serializable data object PrayerRoute
@Serializable data object PrayerMonthRoute
@Serializable data object AdhanSettingsRoute

fun NavGraphBuilder.prayerScreen(onOpenSettings: () -> Unit, onMonth: () -> Unit, onAdhan: () -> Unit) {
    composable<PrayerRoute> { PrayerTodayEntry(onOpenSettings, onMonth, onAdhan) }
    composable<PrayerMonthRoute> { MonthEntry() }
    composable<AdhanSettingsRoute> { AdhanSettingsEntry() }
}

@Composable
fun PrayerTodayEntry(onOpenSettings: () -> Unit, onMonth: () -> Unit, onAdhan: () -> Unit, viewModel: PrayerTodayViewModel = hiltViewModel()) {
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val s = schedule ?: return
    val f = rememberTimeFormatter(s.settings.timeFormat)
    val arabic = isArabicUi()
    val focus = rememberFocusRestoreState()
    val next = PrayerTimesCalculator.nextPrayer(s.today, s.tomorrow, now)
    val current = PrayerTimesCalculator.currentPrayer(s.today, now)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(
            title = stringResource(R.string.prayer_title),
            subtitle = stringResource(R.string.prayer_subtitle, s.city.displayName(arabic), s.hijri.format()),
        )
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(56.dp)) {
            Column(Modifier.weight(0.9f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.prayer_next, next.prayer.label(s.date)), style = MtvTheme.type.title, color = MtvTheme.colors.goldSoft)
                Text(f.countdown(next.atMillis - now), style = MtvTheme.type.display.copy(fontFamily = MtvTheme.type.numeric.fontFamily))
                Spacer(Modifier.height(MtvSpacing.s))
                Text(
                    stringResource(
                        R.string.prayer_method_line,
                        stringResource(s.settings.prayer.method.labelRes()),
                        stringResource(s.settings.prayer.madhab.labelRes()),
                    ),
                    style = MtvTheme.type.caption,
                    color = MtvTheme.colors.onBackgroundMuted,
                )
                Spacer(Modifier.height(MtvSpacing.l))
                MtvButton(stringResource(R.string.month_title), onMonth, Modifier.restorableFocus(focus, "month"), icon = Icons.Rounded.CalendarMonth)
                Spacer(Modifier.height(MtvSpacing.s))
                Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
                    MtvButton(stringResource(R.string.adhan_settings), onAdhan, Modifier.restorableFocus(focus, "adhan"), style = com.mfouad.muslimtv.core.designsystem.component.ButtonStyle.Secondary, icon = Icons.Rounded.NotificationsActive)
                    MtvButton(stringResource(R.string.prayer_adjust), onOpenSettings, Modifier.restorableFocus(focus, "settings"), style = com.mfouad.muslimtv.core.designsystem.component.ButtonStyle.Secondary, icon = Icons.Rounded.Tune)
                }
            }
            Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                Prayer.entries.forEach { p ->
                    val highlighted = p == current && !next.isTomorrow || (next.isTomorrow && p == Prayer.ISHA)
                    val isNext = p == next.prayer && !next.isTomorrow
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    isNext -> MtvTheme.colors.primary.copy(alpha = 0.45f)
                                    highlighted -> MtvTheme.colors.surface
                                    else -> MtvTheme.colors.surface.copy(alpha = 0.4f)
                                },
                                MtvShapes.small,
                            )
                            .padding(horizontal = MtvSpacing.l, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            p.label(s.date),
                            style = if (isNext) MtvTheme.type.bodyStrong else MtvTheme.type.body,
                            color = if (p.isObligatory) MtvTheme.colors.onBackground else MtvTheme.colors.onBackgroundMuted,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            f.time(s.today[p], s.zone),
                            style = MtvTheme.type.bodyStrong,
                            color = if (isNext) MtvTheme.colors.gold else MtvTheme.colors.onBackground,
                        )
                    }
                }
            }
        }
    }
    RestoreFocusEffect(focus, "month")
}
