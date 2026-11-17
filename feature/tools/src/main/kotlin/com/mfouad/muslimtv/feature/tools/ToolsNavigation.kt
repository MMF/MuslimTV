package com.mfouad.muslimtv.feature.tools

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object ToolsRoute
@Serializable data object CalendarRoute
@Serializable data object QiblaRoute
@Serializable data object HadithRoute
@Serializable data object NawawiRoute
@Serializable data object LiveRoute
@Serializable data class LivePlayerRoute(val id: String)
@Serializable data object KidsRoute
@Serializable data object HajjRoute

fun NavGraphBuilder.toolsGraph(nav: NavController) {
    composable<ToolsRoute> {
        ToolsHomeEntry(
            onCalendar = { nav.navigate(CalendarRoute) },
            onQibla = { nav.navigate(QiblaRoute) },
            onHadith = { nav.navigate(HadithRoute) },
            onNawawi = { nav.navigate(NawawiRoute) },
            onLive = { nav.navigate(LiveRoute) },
            onKids = { nav.navigate(KidsRoute) },
            onHajj = { nav.navigate(HajjRoute) },
        )
    }
    composable<CalendarRoute> { CalendarEntry() }
    composable<QiblaRoute> { QiblaEntry() }
    composable<HadithRoute> { HadithEntry(onNawawi = { nav.navigate(NawawiRoute) }) }
    composable<NawawiRoute> { NawawiEntry() }
    composable<LiveRoute> { LiveListEntry(onPlay = { nav.navigate(LivePlayerRoute(it)) }) }
    composable<LivePlayerRoute> { LivePlayerEntry() }
    composable<KidsRoute> { KidsEntry() }
    composable<HajjRoute> { HajjEntry() }
}
