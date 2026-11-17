package com.mfouad.muslimtv.feature.downloads

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable data object DownloadsRoute
@Serializable data object DownloadPickerRoute
@Serializable data class ReciterStorageRoute(val reciterId: String)
@Serializable data object ExtrasRoute

fun NavGraphBuilder.downloadsGraph(nav: NavController) {
    composable<DownloadsRoute> {
        DownloadsEntry(
            onAdd = { nav.navigate(DownloadPickerRoute) },
            onReciter = { nav.navigate(ReciterStorageRoute(it)) },
            onExtras = { nav.navigate(ExtrasRoute) },
        )
    }
    composable<DownloadPickerRoute> { DownloadPickerEntry(onDone = { nav.popBackStack() }) }
    composable<ReciterStorageRoute> { ReciterStorageEntry(it.toRoute<ReciterStorageRoute>().reciterId) }
    composable<ExtrasRoute> { ExtrasEntry() }
}
