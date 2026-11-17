package com.mfouad.muslimtv.feature.azkar

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mfouad.muslimtv.core.model.AzkarCategory
import kotlinx.serialization.Serializable

@Serializable data object AzkarRoute
@Serializable data class ZikrRoute(val category: String? = null, val chapterId: Int = 0)
@Serializable data object OccasionsRoute
@Serializable data object TasbeehRoute
@Serializable data object AsmaRoute
@Serializable data object RuqyahRoute

fun NavController.openAzkar(category: AzkarCategory) = navigate(ZikrRoute(category = category.name))

fun NavGraphBuilder.azkarGraph(nav: NavController, onOpenPlayer: () -> Unit, onDownloads: () -> Unit) {
    composable<AzkarRoute> {
        AzkarHomeEntry(
            onCategory = { nav.openAzkar(it) },
            onOccasions = { nav.navigate(OccasionsRoute) },
            onTasbeeh = { nav.navigate(TasbeehRoute) },
            onAsma = { nav.navigate(AsmaRoute) },
            onRuqyah = { nav.navigate(RuqyahRoute) },
        )
    }
    composable<ZikrRoute> { ZikrEntry(onBack = { nav.popBackStack() }) }
    composable<OccasionsRoute> { OccasionsEntry(onChapter = { nav.navigate(ZikrRoute(chapterId = it)) }) }
    composable<TasbeehRoute> { TasbeehEntry() }
    composable<AsmaRoute> { AsmaEntry() }
    composable<RuqyahRoute> { RuqyahEntry(onPlaying = onOpenPlayer, onDownloads = onDownloads) }
}
