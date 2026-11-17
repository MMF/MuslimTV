package com.mfouad.muslimtv.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.ChoiceRow
import com.mfouad.muslimtv.core.designsystem.component.ConfirmDialog
import com.mfouad.muslimtv.core.designsystem.component.FocusCard
import com.mfouad.muslimtv.core.designsystem.component.GeometricBackground
import com.mfouad.muslimtv.core.designsystem.component.GoldDivider
import com.mfouad.muslimtv.core.designsystem.component.SafeArea
import com.mfouad.muslimtv.core.designsystem.component.StepIndicator
import com.mfouad.muslimtv.core.designsystem.focus.requestFocusSafely
import com.mfouad.muslimtv.core.designsystem.theme.MtvMotion
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.CalculationMethod
import com.mfouad.muslimtv.core.model.PrayerDefaults
import com.mfouad.muslimtv.core.model.UserSettings
import com.mfouad.muslimtv.core.ui.AppLanguage
import com.mfouad.muslimtv.core.ui.CityPicker
import com.mfouad.muslimtv.core.ui.CityPickerState
import com.mfouad.muslimtv.core.ui.countryName
import com.mfouad.muslimtv.core.ui.labelRes
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import com.mfouad.muslimtv.core.ui.R as UiR

private const val STEP_COUNT = 3

@Composable
fun OnboardingEntry(onFinished: () -> Unit, onExit: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cityState by viewModel.cityState.collectAsStateWithLifecycle()
    var confirmExit by rememberSaveable { mutableStateOf(false) }

    BackHandler { if (!viewModel.back()) confirmExit = true }

    OnboardingScreen(
        step = step,
        settings = settings,
        cityState = cityState,
        onLanguage = viewModel::chooseLanguage,
        onQueryChange = viewModel::onQueryChange,
        onCity = { viewModel.chooseCity(it) },
        onMethod = { viewModel.chooseMethod(it, onFinished) },
    )

    if (confirmExit) {
        ConfirmDialog(
            title = stringResource(UiR.string.exit_title),
            message = stringResource(UiR.string.exit_message),
            confirmLabel = stringResource(UiR.string.action_exit),
            dismissLabel = stringResource(UiR.string.action_stay),
            onConfirm = onExit,
            onDismiss = { confirmExit = false },
        )
    }
}

@Composable
private fun OnboardingScreen(
    step: Int,
    settings: UserSettings,
    cityState: CityPickerState,
    onLanguage: (AppLanguage) -> Unit,
    onQueryChange: (String) -> Unit,
    onCity: (com.mfouad.muslimtv.core.model.City) -> Unit,
    onMethod: (CalculationMethod) -> Unit,
) {
    val f = rememberTimeFormatter()
    GeometricBackground(patternAlpha = 0.06f) {
        SafeArea {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(56.dp)) {
                // Left: what this step is about.
                Column(
                    Modifier.weight(0.85f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(UiR.string.app_name), style = MtvTheme.type.title, color = MtvTheme.colors.gold)
                    Spacer(Modifier.height(MtvSpacing.l))
                    val (title, body) = when (step) {
                        0 -> stringResource(R.string.onb_welcome) to stringResource(R.string.onb_welcome_body)
                        1 -> stringResource(R.string.onb_city_title) to stringResource(R.string.onb_city_body)
                        else -> stringResource(R.string.onb_method_title) to stringResource(
                            R.string.onb_method_body,
                            settings.city?.let { countryName(it.countryCode, f.locale) }.orEmpty(),
                        )
                    }
                    Text(title, style = MtvTheme.type.headline)
                    Spacer(Modifier.height(MtvSpacing.s))
                    GoldDivider()
                    Spacer(Modifier.height(MtvSpacing.m))
                    Text(body, style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
                    Spacer(Modifier.height(MtvSpacing.xl))
                    StepIndicator(
                        current = step,
                        total = STEP_COUNT,
                        label = stringResource(R.string.onb_step, f.number(step + 1), f.number(STEP_COUNT)),
                    )
                }
                // Right: the one thing to choose.
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn(MtvMotion.standard(MtvMotion.LONG)) togetherWith fadeOut(MtvMotion.standard(MtvMotion.SHORT)) },
                    modifier = Modifier.weight(1.15f).fillMaxHeight(),
                    label = "onboardingStep",
                ) { s ->
                    when (s) {
                        0 -> LanguageStep(onLanguage)
                        1 -> CityPicker(
                            state = cityState,
                            selectedCityId = settings.city?.id,
                            onQueryChange = onQueryChange,
                            onPick = onCity,
                            modifier = Modifier.fillMaxSize().padding(top = MtvSpacing.l),
                        )
                        else -> MethodStep(settings, onMethod)
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageStep(onLanguage: (AppLanguage) -> Unit) {
    val current = AppLanguage.current()
    val first = remember { FocusRequester() }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MtvSpacing.l, Alignment.CenterVertically),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
            Icon(Icons.Rounded.Translate, null, Modifier.size(32.dp), tint = MtvTheme.colors.gold)
            Text(stringResource(R.string.onb_language_title), style = MtvTheme.type.title)
        }
        listOf(AppLanguage.ARABIC, AppLanguage.ENGLISH).forEach { lang ->
            FocusCard(
                onClick = { onLanguage(lang) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .then(if (lang == current) Modifier.focusRequester(first) else Modifier),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(if (lang == AppLanguage.ARABIC) UiR.string.language_arabic else UiR.string.language_english),
                        style = if (lang == AppLanguage.ARABIC) {
                            com.mfouad.muslimtv.core.designsystem.theme.mtvTypography(arabic = true).headline
                        } else {
                            com.mfouad.muslimtv.core.designsystem.theme.mtvTypography(arabic = false).headline
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Text(stringResource(R.string.onb_language_body), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
    }
    LaunchedEffect(Unit) { first.requestFocusSafely() }
}

@Composable
private fun MethodStep(settings: UserSettings, onMethod: (CalculationMethod) -> Unit) {
    val country = settings.city?.countryCode.orEmpty()
    val recommended = PrayerDefaults.methodFor(country)
    // Recommended first, then the rest in a stable order.
    val methods = remember(recommended) { listOf(recommended) + CalculationMethod.entries.filter { it != recommended } }
    val first = remember { FocusRequester() }
    Column(Modifier.fillMaxSize().padding(top = MtvSpacing.l), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        LazyColumn(
            modifier = Modifier.weight(1f).focusRestorer(first),
            verticalArrangement = Arrangement.spacedBy(MtvSpacing.s),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        ) {
            items(methods, key = { it.name }) { method ->
                ChoiceRow(
                    title = stringResource(method.labelRes()),
                    selected = method == settings.prayer.method,
                    badge = if (method == recommended) stringResource(UiR.string.recommended) else null,
                    onClick = { onMethod(method) },
                    modifier = if (method == recommended) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
        Text(
            stringResource(R.string.onb_method_note, stringResource(settings.prayer.madhab.labelRes())),
            style = MtvTheme.type.caption,
            color = MtvTheme.colors.onBackgroundMuted,
        )
    }
    LaunchedEffect(Unit) { first.requestFocusSafely() }
}

