package com.mfouad.muslimtv.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.mfouad.muslimtv.core.model.AzkarCategory
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mfouad.muslimtv.R
import com.mfouad.muslimtv.core.designsystem.component.ConfirmDialog
import com.mfouad.muslimtv.core.designsystem.component.GeometricBackground
import com.mfouad.muslimtv.core.designsystem.component.NavRail
import com.mfouad.muslimtv.core.designsystem.component.NavRailDefaults
import com.mfouad.muslimtv.core.designsystem.component.RailItem
import com.mfouad.muslimtv.core.designsystem.component.SafeArea
import com.mfouad.muslimtv.core.designsystem.focus.LocalContentFocus
import com.mfouad.muslimtv.core.designsystem.focus.requestFocusSafely
import com.mfouad.muslimtv.core.designsystem.focus.trapVerticalFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvMotion
import com.mfouad.muslimtv.feature.azkar.azkarGraph
import com.mfouad.muslimtv.feature.azkar.openAzkar
import com.mfouad.muslimtv.feature.downloads.DownloadsRoute
import com.mfouad.muslimtv.feature.downloads.downloadsGraph
import com.mfouad.muslimtv.feature.home.AmbientRoute
import com.mfouad.muslimtv.feature.home.HomeRoute
import com.mfouad.muslimtv.feature.tools.toolsGraph
import com.mfouad.muslimtv.feature.tools.LivePlayerRoute
import com.mfouad.muslimtv.feature.home.ambientScreen
import com.mfouad.muslimtv.feature.home.homeScreen
import com.mfouad.muslimtv.feature.onboarding.OnboardingRoute
import com.mfouad.muslimtv.feature.onboarding.onboardingScreen
import com.mfouad.muslimtv.feature.prayer.AdhanSettingsRoute
import com.mfouad.muslimtv.feature.prayer.PrayerMonthRoute
import com.mfouad.muslimtv.feature.prayer.prayerScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import com.mfouad.muslimtv.feature.quran.ListeningRoute
import com.mfouad.muslimtv.feature.quran.NatureRoute
import com.mfouad.muslimtv.feature.quran.TahfeezSessionRoute
import com.mfouad.muslimtv.feature.quran.openReader
import com.mfouad.muslimtv.feature.quran.quranGraph
import com.mfouad.muslimtv.feature.settings.navigateToCitySettings
import com.mfouad.muslimtv.feature.settings.settingsGraph
import kotlinx.coroutines.launch
import com.mfouad.muslimtv.core.ui.R as UiR

/**
 * App shell: navigation rail + one NavHost.
 *
 * Back rules: on a section's content, Back moves focus to the rail; on the rail, Back goes to
 * Home; on Home's rail it asks before exiting. Detail screens simply go up one level.
 */
@Composable
fun MuslimTvApp(
    onboardingDone: Boolean,
    versionName: String,
    online: Boolean,
    pendingOpen: MutableStateFlow<String?>,
    idle: Boolean,
    onExit: () -> Unit,
    recitationActive: Boolean = false,
    recitationPlaying: Boolean = false,
    onTogglePlayback: () -> Unit = {},
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = TopLevel.of(entry?.destination)
    val railRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val scope = rememberCoroutineScope()
    var railFocused by remember { mutableStateOf(false) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }

    val contentFocus = LocalContentFocus.current
    val railItems = TopLevel.entries.map { RailItem(it.name, stringResource(it.label), it.icon) }

    GeometricBackground {
        SafeArea {
            // Back on a section screen is handled here, before the NavHost's own back handling
            // (which would pop straight to Home): content -> rail -> Home -> exit dialog.
            Box(
                Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { e ->
                        if (e.key != Key.Back || current == null) return@onPreviewKeyEvent false
                        if (e.type == KeyEventType.KeyUp) {
                            when {
                                !railFocused -> scope.launch { railRequester.requestFocusSafely() }
                                current != TopLevel.HOME -> nav.navigateTopLevel(TopLevel.HOME)
                                else -> confirmExit = true
                            }
                        }
                        true
                    },
            ) {
                NavHost(
                    navController = nav,
                    startDestination = if (onboardingDone) HomeRoute else OnboardingRoute,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = if (current != null) NavRailDefaults.CollapsedWidth + 24.dp else 0.dp)
                        .trapVerticalFocus(),
                    enterTransition = { fadeIn(MtvMotion.standard(MtvMotion.LONG)) },
                    exitTransition = { fadeOut(MtvMotion.standard(MtvMotion.SHORT)) },
                    popEnterTransition = { fadeIn(MtvMotion.standard(MtvMotion.LONG)) },
                    popExitTransition = { fadeOut(MtvMotion.standard(MtvMotion.SHORT)) },
                ) {
                    onboardingScreen(
                        onFinished = {
                            nav.navigate(HomeRoute) { popUpTo(OnboardingRoute) { inclusive = true } }
                        },
                        onExit = onExit,
                    )
                    homeScreen(
                        onOpenPrayer = { nav.navigateTopLevel(TopLevel.PRAYER) },
                        onContinueQuran = { nav.openReader(it) },
                        onOpenAzkar = { nav.openAzkar(it) },
                        onOpenAyah = { nav.openReader(it) },
                        onChooseCity = { nav.navigateToCitySettings() },
                    )
                    ambientScreen(onExit = { nav.popBackStack() })
                    prayerScreen(
                        onOpenSettings = { nav.navigateTopLevel(TopLevel.SETTINGS) },
                        onMonth = { nav.navigate(PrayerMonthRoute) },
                        onAdhan = { nav.navigate(AdhanSettingsRoute) },
                    )
                    settingsGraph(nav, versionName, onDownloads = { nav.navigate(DownloadsRoute) }, onAmbientNow = { nav.navigate(AmbientRoute) })
                    downloadsGraph(nav)
                    quranGraph(nav = nav, onOpenDownloads = { nav.navigate(DownloadsRoute) })
                    azkarGraph(nav, onOpenPlayer = { nav.navigate(ListeningRoute) }, onDownloads = { nav.navigate(DownloadsRoute) })
                    toolsGraph(nav)
                }

                if (current != null) {
                    NavRail(
                        items = railItems,
                        // Leaving the rail sideways returns to the item the screen last focused.
                        exitTarget = { contentFocus.active?.lastRequester() },
                        offlineLabel = if (online) null else stringResource(UiR.string.offline),
                        selectedKey = current.name,
                        selectedRequester = railRequester,
                        action = if (recitationActive) {
                            RailItem(
                                "playback",
                                stringResource(if (recitationPlaying) R.string.rail_pause_recitation else R.string.rail_resume_recitation),
                                if (recitationPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            )
                        } else null,
                        onAction = onTogglePlayback,
                        onFocusChanged = { railFocused = it },
                        onSelect = { item ->
                            val target = TopLevel.valueOf(item.key)
                            if (target == current) {
                                // Already here: just move into the content.
                                focusManager.moveFocus(if (rtl) FocusDirection.Left else FocusDirection.Right)
                            } else {
                                nav.navigateTopLevel(target)
                            }
                        },
                    )
                }
            }
        }
    }


    // Ambient screen after the idle timeout, except where the family is actively listening,
    // memorizing or already in a full-screen view.
    LaunchedEffect(idle) {
        val d = nav.currentBackStackEntry?.destination
        val busy = d == null || listOf(AmbientRoute::class, ListeningRoute::class, NatureRoute::class, TahfeezSessionRoute::class, OnboardingRoute::class, LivePlayerRoute::class)
            .any { d.hasRoute(it) }
        if (idle && !busy) nav.navigate(AmbientRoute)
    }

    val open by pendingOpen.collectAsState()
    LaunchedEffect(open, onboardingDone) {
        val o = open ?: return@LaunchedEffect
        if (!onboardingDone) return@LaunchedEffect
        when {
            o == "azkar_after_prayer" -> nav.openAzkar(AzkarCategory.AFTER_PRAYER)
            o.startsWith("reader:") -> o.substringAfter(':').toIntOrNull()?.let { nav.openReader(com.mfouad.muslimtv.core.model.AyahRef(it, 1)) }
        }
        pendingOpen.value = null
    }

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

/** Sections share one back stack rooted at Home, with each section's state saved and restored. */
private fun NavHostController.navigateTopLevel(target: TopLevel) {
    val options: NavOptionsBuilder.() -> Unit = {
        popUpTo(HomeRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    navigate(target.route, options)
}
