package com.mfouad.muslimtv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.ListRow
import com.mfouad.muslimtv.core.designsystem.component.ScreenHeader
import com.mfouad.muslimtv.core.designsystem.focus.FocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.model.TimeFormat
import com.mfouad.muslimtv.core.model.UserSettings
import com.mfouad.muslimtv.core.ui.AppLanguage
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.labelRes
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import com.mfouad.muslimtv.core.ui.R as UiR

@Composable
internal fun SettingsEntry(
    versionName: String,
    onChoice: (String) -> Unit,
    onCity: () -> Unit,
    onAdjustments: () -> Unit,
    onQuranFont: () -> Unit,
    onCredits: () -> Unit,
    onDownloads: () -> Unit,
    onAmbientNow: () -> Unit,
    onProfiles: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val s = settings ?: return
    val focus = rememberFocusRestoreState()
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.settings_title))
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(MtvSpacing.s),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        ) {
            settingsRows(s, focus, versionName, onChoice, onCity, onAdjustments, onQuranFont, onCredits, onDownloads, onAmbientNow, onProfiles, profileName, viewModel)
        }
    }
    RestoreFocusEffect(focus, Choice.LANGUAGE)
}

private fun LazyListScope.section(title: Int) {
    item(key = "section_$title") {
        Text(
            stringResource(title),
            style = MtvTheme.type.label,
            color = MtvTheme.colors.gold,
            modifier = Modifier.padding(top = MtvSpacing.m, bottom = 4.dp),
        )
    }
}

private fun LazyListScope.row(key: String, content: @Composable () -> Unit) = item(key = key) { content() }

private fun LazyListScope.settingsRows(
    s: UserSettings,
    focus: FocusRestoreState,
    versionName: String,
    onChoice: (String) -> Unit,
    onCity: () -> Unit,
    onAdjustments: () -> Unit,
    onQuranFont: () -> Unit,
    onCredits: () -> Unit,
    onDownloads: () -> Unit,
    onAmbientNow: () -> Unit,
    onProfiles: () -> Unit,
    profileName: String?,
    vm: SettingsViewModel,
) {
    section(R.string.settings_section_general)
    row("profile") {
        ListRow(
            title = stringResource(R.string.settings_profile),
            value = profileName ?: stringResource(R.string.profile_family),
            icon = Icons.Rounded.People,
            onClick = onProfiles,
            modifier = Modifier.restorableFocus(focus, "profile"),
        )
    }
    row(Choice.LANGUAGE) {
        ListRow(
            title = stringResource(R.string.settings_language),
            value = stringResource(if (AppLanguage.current() == AppLanguage.ARABIC) UiR.string.language_arabic else UiR.string.language_english),
            icon = Icons.Rounded.Translate,
            onClick = { onChoice(Choice.LANGUAGE) },
            modifier = Modifier.restorableFocus(focus, Choice.LANGUAGE),
        )
    }
    row("city") {
        ListRow(
            title = stringResource(R.string.settings_city),
            value = s.city?.displayName(isArabicUi()).orEmpty(),
            icon = Icons.Rounded.LocationCity,
            onClick = onCity,
            modifier = Modifier.restorableFocus(focus, "city"),
        )
    }
    row(Choice.TIME_FORMAT) {
        ListRow(
            title = stringResource(R.string.settings_time_format),
            value = stringResource(s.timeFormat.labelRes()),
            icon = Icons.Rounded.Schedule,
            onClick = { onChoice(Choice.TIME_FORMAT) },
            modifier = Modifier.restorableFocus(focus, Choice.TIME_FORMAT),
        )
    }

    row(Choice.AMBIENT) {
        val f = rememberTimeFormatter()
        ListRow(
            title = stringResource(R.string.settings_ambient),
            value = if (s.ambientTimeoutMin == 0) stringResource(R.string.ambient_never) else stringResource(R.string.ambient_after, f.number(s.ambientTimeoutMin)),
            icon = Icons.Rounded.Landscape,
            onClick = { onChoice(Choice.AMBIENT) },
            modifier = Modifier.restorableFocus(focus, Choice.AMBIENT),
        )
    }
    row("ambient_now") {
        ListRow(
            title = stringResource(R.string.ambient_now),
            icon = Icons.Rounded.Landscape,
            onClick = onAmbientNow,
            modifier = Modifier.restorableFocus(focus, "ambient_now"),
            showChevron = false,
        )
    }

    section(R.string.settings_section_prayer)
    row(Choice.METHOD) {
        ListRow(
            title = stringResource(R.string.settings_method),
            value = stringResource(s.prayer.method.labelRes()),
            icon = Icons.Rounded.Calculate,
            onClick = { onChoice(Choice.METHOD) },
            modifier = Modifier.restorableFocus(focus, Choice.METHOD),
        )
    }
    row(Choice.MADHAB) {
        ListRow(
            title = stringResource(R.string.settings_madhab),
            value = stringResource(s.prayer.madhab.labelRes()),
            icon = Icons.Rounded.WbSunny,
            onClick = { onChoice(Choice.MADHAB) },
            modifier = Modifier.restorableFocus(focus, Choice.MADHAB),
        )
    }
    row("adjustments") {
        val count = Prayer.entries.count { s.prayer.adjustments[it] != 0 }
        val f = rememberTimeFormatter()
        ListRow(
            title = stringResource(R.string.settings_adjustments),
            value = if (count == 0) stringResource(R.string.settings_adjustments_none) else stringResource(R.string.settings_adjustments_some, f.number(count)),
            icon = Icons.Rounded.Tune,
            onClick = onAdjustments,
            modifier = Modifier.restorableFocus(focus, "adjustments"),
        )
    }
    row(Choice.HIGH_LATITUDE) {
        ListRow(
            title = stringResource(R.string.settings_high_latitude),
            value = stringResource(s.prayer.highLatitude.labelRes()),
            icon = Icons.Rounded.NightsStay,
            onClick = { onChoice(Choice.HIGH_LATITUDE) },
            modifier = Modifier.restorableFocus(focus, Choice.HIGH_LATITUDE),
        )
    }
    row(Choice.HIJRI) {
        val f = rememberTimeFormatter()
        ListRow(
            title = stringResource(R.string.settings_hijri),
            value = if (s.hijriOffset == 0) stringResource(R.string.no_change) else stringResource(R.string.days_value, signed(f.number(s.hijriOffset), s.hijriOffset)),
            icon = Icons.Rounded.CalendarMonth,
            onClick = { onChoice(Choice.HIJRI) },
            modifier = Modifier.restorableFocus(focus, Choice.HIJRI),
        )
    }

    row(Choice.RAMADAN) {
        ListRow(
            title = stringResource(R.string.settings_ramadan),
            value = stringResource(
                when (s.ramadanMode) {
                    com.mfouad.muslimtv.core.model.RamadanMode.AUTO -> R.string.ramadan_auto
                    com.mfouad.muslimtv.core.model.RamadanMode.ON -> R.string.ramadan_on
                    com.mfouad.muslimtv.core.model.RamadanMode.OFF -> R.string.ramadan_off
                },
            ),
            icon = Icons.Rounded.NightsStay,
            onClick = { onChoice(Choice.RAMADAN) },
            modifier = Modifier.restorableFocus(focus, Choice.RAMADAN),
        )
    }

    section(R.string.settings_section_quran)
    row("kahf") {
        ListRow(stringResource(R.string.settings_kahf), { vm.setKahf(!s.kahfReminder) }, Modifier.restorableFocus(focus, "kahf"), value = stringResource(if (s.kahfReminder) R.string.on else R.string.off), showChevron = false)
    }
    row("mulk") {
        ListRow(stringResource(R.string.settings_mulk), { vm.setMulk(!s.mulkReminder) }, Modifier.restorableFocus(focus, "mulk"), value = stringResource(if (s.mulkReminder) R.string.on else R.string.off), showChevron = false)
    }
    row("quran_font") {
        val f = rememberTimeFormatter()
        ListRow(
            title = stringResource(R.string.settings_quran_font),
            value = stringResource(R.string.font_size_value, f.number(s.quranFontSizeSp)),
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            onClick = onQuranFont,
            modifier = Modifier.restorableFocus(focus, "quran_font"),
        )
    }

    row("downloads") {
        ListRow(
            title = stringResource(R.string.settings_downloads),
            subtitle = stringResource(R.string.settings_downloads_hint),
            icon = Icons.Rounded.CloudDownload,
            onClick = onDownloads,
            modifier = Modifier.restorableFocus(focus, "downloads"),
        )
    }

    section(R.string.settings_section_about)
    row("credits") {
        ListRow(
            title = stringResource(R.string.settings_credits),
            subtitle = stringResource(R.string.settings_version, versionName),
            icon = Icons.Rounded.Info,
            onClick = onCredits,
            modifier = Modifier.restorableFocus(focus, "credits"),
        )
    }
}

internal fun TimeFormat.labelRes(): Int = when (this) {
    TimeFormat.SYSTEM -> R.string.time_format_system
    TimeFormat.H12 -> R.string.time_format_12
    TimeFormat.H24 -> R.string.time_format_24
}

/** "+3" / "-2" with locale digits; the sign stays ASCII so it reads the same in both languages. */
internal fun signed(formatted: String, value: Int): String = when {
    value > 0 -> "+$formatted"
    value < 0 -> "-" + formatted.trimStart('-', '؜', '‏', '‎')
    else -> formatted
}

