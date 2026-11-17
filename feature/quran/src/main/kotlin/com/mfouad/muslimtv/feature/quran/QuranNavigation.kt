package com.mfouad.muslimtv.feature.quran

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mfouad.muslimtv.core.model.AyahRef
import androidx.compose.material.icons.rounded.Landscape
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import kotlinx.serialization.Serializable

@Serializable data object QuranRoute
@Serializable data class ReaderRoute(val surah: Int, val ayah: Int = 1)
@Serializable data object ListeningRoute
@Serializable data class TahfeezSetupRoute(val surah: Int, val ayah: Int = 1)
@Serializable data class TahfeezSessionRoute(val sessionId: Long)
@Serializable data object KidsTahfeezRoute
@Serializable data object NatureRoute

fun NavController.openReader(ref: AyahRef) = navigate(ReaderRoute(ref.surah, ref.ayah))

fun NavGraphBuilder.quranGraph(
    nav: NavController,
    onOpenDownloads: () -> Unit,
    listeningBackground: @androidx.compose.runtime.Composable () -> Unit = {},
) {
    val onMemorize: (Int, Int) -> Unit = { s, a -> nav.navigate(TahfeezSetupRoute(s, a)) }
    val openSession: (Long) -> Unit = { id -> nav.navigate(TahfeezSessionRoute(id)) { popUpTo<QuranRoute>() } }
    composable<QuranRoute> {
        QuranHomeEntry(
            onOpen = { nav.openReader(it) },
            onNewTahfeez = { onMemorize(1, 1) },
            onKids = { nav.navigate(KidsTahfeezRoute) },
            onResume = { nav.navigate(TahfeezSessionRoute(it)) },
        )
    }
    composable<TahfeezSetupRoute> { com.mfouad.muslimtv.feature.quran.tahfeez.TahfeezSetupEntry(onStart = openSession) }
    composable<KidsTahfeezRoute> { com.mfouad.muslimtv.feature.quran.tahfeez.KidsPickerEntry(onStart = openSession) }
    composable<TahfeezSessionRoute> {
        com.mfouad.muslimtv.feature.quran.tahfeez.TahfeezSessionEntry(onBack = { nav.popBackStack() }, onDownloads = onOpenDownloads)
    }
    composable<ReaderRoute> {
        ReaderEntry(
            onOpenPlayer = { nav.navigate(ListeningRoute) { launchSingleTop = true } },
            onMemorize = onMemorize,
            onOpenDownloads = onOpenDownloads,
        )
    }
    composable<ListeningRoute> {
        ListeningEntry(
            onChooseSurah = { nav.navigate(QuranRoute) { launchSingleTop = true } },
            background = listeningBackground,
            extraControls = { focus ->
                com.mfouad.muslimtv.core.designsystem.component.MtvButton(
                    androidx.compose.ui.res.stringResource(R.string.nature_mode),
                    { nav.navigate(NatureRoute) },
                    androidx.compose.ui.Modifier.restorableFocus(focus, "nature"),
                    style = com.mfouad.muslimtv.core.designsystem.component.ButtonStyle.Secondary,
                    icon = androidx.compose.material.icons.Icons.Rounded.Landscape,
                )
            },
        )
    }
    composable<NatureRoute> { NatureEntry(onChooseSurah = { nav.navigate(QuranRoute) { launchSingleTop = true } }) }
}
