package com.mfouad.muslimtv.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.ButtonStyle
import com.mfouad.muslimtv.core.designsystem.component.ChoiceRow
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.QuranText
import com.mfouad.muslimtv.core.designsystem.component.ScreenHeader
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.AsrMadhab
import com.mfouad.muslimtv.core.model.CalculationMethod
import com.mfouad.muslimtv.core.model.HighLatitudeOption
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.model.PrayerDefaults
import com.mfouad.muslimtv.core.model.TimeFormat
import com.mfouad.muslimtv.core.model.UserSettings
import com.mfouad.muslimtv.core.ui.AppLanguage
import com.mfouad.muslimtv.core.ui.CityPicker
import com.mfouad.muslimtv.core.ui.format
import com.mfouad.muslimtv.core.ui.label
import com.mfouad.muslimtv.core.ui.labelRes
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import com.mfouad.muslimtv.core.ui.R as UiR

private data class Option(val key: String, val label: String, val selected: Boolean, val badge: String? = null, val onPick: () -> Unit)

/** One reusable single-choice screen; picking an option saves it and goes back one level. */
@Composable
internal fun ChoiceEntry(key: String, onDone: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val f = rememberTimeFormatter()
    val recommended = stringResource(UiR.string.recommended)

    fun pick(action: () -> Unit): () -> Unit = { action(); onDone() }

    val (title, hint, options) = when (key) {
        Choice.LANGUAGE -> Triple(
            stringResource(R.string.settings_language), null,
            AppLanguage.entries.map { lang ->
                Option(
                    lang.name,
                    stringResource(if (lang == AppLanguage.ARABIC) UiR.string.language_arabic else UiR.string.language_english),
                    lang == AppLanguage.current(),
                    onPick = { onDone(); AppLanguage.set(lang) },
                )
            },
        )
        Choice.METHOD -> {
            val rec = s.city?.countryCode?.let(PrayerDefaults::methodFor)
            Triple(
                stringResource(R.string.settings_method), null,
                CalculationMethod.entries.map { m ->
                    Option(m.name, stringResource(m.labelRes()), m == s.prayer.method, if (m == rec) recommended else null, pick { viewModel.setMethod(m) })
                },
            )
        }
        Choice.MADHAB -> Triple(
            stringResource(R.string.settings_madhab), null,
            AsrMadhab.entries.map { m -> Option(m.name, stringResource(m.labelRes()), m == s.prayer.madhab, onPick = pick { viewModel.setMadhab(m) }) },
        )
        Choice.HIGH_LATITUDE -> Triple(
            stringResource(R.string.settings_high_latitude), stringResource(R.string.settings_high_latitude_hint),
            HighLatitudeOption.entries.map { o -> Option(o.name, stringResource(o.labelRes()), o == s.prayer.highLatitude, onPick = pick { viewModel.setHighLatitude(o) }) },
        )
        Choice.TIME_FORMAT -> Triple(
            stringResource(R.string.settings_time_format), null,
            TimeFormat.entries.map { t -> Option(t.name, stringResource(t.labelRes()), t == s.timeFormat, onPick = pick { viewModel.setTimeFormat(t) }) },
        )
        Choice.RAMADAN -> Triple(
            stringResource(R.string.settings_ramadan), null,
            com.mfouad.muslimtv.core.model.RamadanMode.entries.map { m ->
                Option(
                    m.name,
                    stringResource(
                        when (m) {
                            com.mfouad.muslimtv.core.model.RamadanMode.AUTO -> R.string.ramadan_auto
                            com.mfouad.muslimtv.core.model.RamadanMode.ON -> R.string.ramadan_on
                            com.mfouad.muslimtv.core.model.RamadanMode.OFF -> R.string.ramadan_off
                        },
                    ),
                    s.ramadanMode == m,
                    onPick = pick { viewModel.setRamadanMode(m) },
                )
            },
        )
        Choice.AMBIENT -> Triple(
            stringResource(R.string.settings_ambient), stringResource(R.string.ambient_hint),
            listOf(0, 5, 10, 20, 30).map { m ->
                Option(
                    m.toString(),
                    if (m == 0) stringResource(R.string.ambient_never) else stringResource(R.string.ambient_after, f.number(m)),
                    s.ambientTimeoutMin == m,
                    onPick = pick { viewModel.setAmbientTimeout(m) },
                )
            },
        )
        else -> Triple(
            stringResource(R.string.settings_hijri), stringResource(R.string.settings_hijri_hint),
            (-UserSettings.MAX_HIJRI_OFFSET..UserSettings.MAX_HIJRI_OFFSET).map { d ->
                Option(
                    key = d.toString(),
                    label = if (d == 0) stringResource(R.string.no_change) else stringResource(R.string.days_value, signed(f.number(d), d)),
                    selected = d == s.hijriOffset,
                    badge = viewModel.hijriPreview(d).format(),
                    onPick = pick { viewModel.setHijriOffset(d) },
                )
            },
        )
    }

    val focus = rememberFocusRestoreState()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(title, subtitle = hint)
        LazyColumn(
            modifier = Modifier.widthIn(max = 820.dp),
            verticalArrangement = Arrangement.spacedBy(MtvSpacing.s),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        ) {
            items(options, key = { it.key }) { o ->
                ChoiceRow(o.label, o.selected, o.onPick, Modifier.restorableFocus(focus, o.key), badge = o.badge)
            }
        }
    }
    RestoreFocusEffect(focus, options.firstOrNull { it.selected }?.key ?: options.first().key)
}

@Composable
internal fun CitySettingsEntry(onDone: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.cityState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadCities() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.settings_city))
        CityPicker(
            state = state,
            selectedCityId = settings?.city?.id,
            onQueryChange = viewModel::onQueryChange,
            onPick = { viewModel.setCity(it); onDone() },
            modifier = Modifier.widthIn(max = 900.dp),
        )
    }
}

@Composable
internal fun AdjustmentsEntry(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val f = rememberTimeFormatter()
    val focus = rememberFocusRestoreState()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.settings_adjustments), subtitle = stringResource(R.string.settings_adjustments_hint))
        LazyColumn(
            modifier = Modifier.widthIn(max = 900.dp),
            verticalArrangement = Arrangement.spacedBy(MtvSpacing.s),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
        ) {
            items(Prayer.entries, key = { it.name }) { p ->
                val minutes = s.prayer.adjustments[p]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MtvTheme.colors.surface, MtvShapes.small)
                        .padding(horizontal = MtvSpacing.m, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m),
                ) {
                    Text(p.label(), style = MtvTheme.type.bodyStrong, modifier = Modifier.weight(1f))
                    Text(
                        if (minutes == 0) stringResource(R.string.no_change) else stringResource(R.string.minutes_value, signed(f.number(minutes), minutes)),
                        style = MtvTheme.type.body,
                        color = if (minutes == 0) MtvTheme.colors.onBackgroundMuted else MtvTheme.colors.goldSoft,
                        modifier = Modifier.widthIn(min = 150.dp),
                    )
                    MtvButton(
                        stringResource(R.string.decrease),
                        { viewModel.setAdjustment(p, minutes - 1) },
                        Modifier.restorableFocus(focus, "${p.name}-"),
                        style = ButtonStyle.Secondary,
                        icon = Icons.Rounded.Remove,
                    )
                    MtvButton(
                        stringResource(R.string.increase),
                        { viewModel.setAdjustment(p, minutes + 1) },
                        Modifier.restorableFocus(focus, "${p.name}+"),
                        style = ButtonStyle.Secondary,
                        icon = Icons.Rounded.Add,
                    )
                }
            }
        }
    }
    RestoreFocusEffect(focus, "${Prayer.FAJR.name}+")
}

@Composable
internal fun QuranFontEntry(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val ayah by viewModel.sampleAyah.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadSampleAyah() }
    val s = settings ?: return
    val f = rememberTimeFormatter()
    val focus = rememberFocusRestoreState()
    val size = s.quranFontSizeSp
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.l)) {
        ScreenHeader(stringResource(R.string.settings_quran_font), subtitle = stringResource(R.string.font_size_value, f.number(size)))
        Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
            MtvButton(
                stringResource(R.string.font_smaller),
                { viewModel.setQuranFontSize(size - UserSettings.QURAN_FONT_STEP_SP) },
                Modifier.restorableFocus(focus, "smaller"),
                style = ButtonStyle.Secondary,
                icon = Icons.Rounded.Remove,
                enabled = size > UserSettings.MIN_QURAN_FONT_SP,
            )
            MtvButton(
                stringResource(R.string.font_larger),
                { viewModel.setQuranFontSize(size + UserSettings.QURAN_FONT_STEP_SP) },
                Modifier.restorableFocus(focus, "larger"),
                icon = Icons.Rounded.Add,
                enabled = size < UserSettings.MAX_QURAN_FONT_SP,
            )
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).background(MtvTheme.colors.surface, MtvShapes.card).padding(MtvSpacing.l),
            contentAlignment = Alignment.Center,
        ) {
            ayah?.let { QuranText(it.text, sizeSp = size) }
        }
    }
    RestoreFocusEffect(focus, "larger")
}
