package com.mfouad.muslimtv.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.mfouad.muslimtv.R
import com.mfouad.muslimtv.feature.azkar.AzkarRoute
import com.mfouad.muslimtv.feature.home.HomeRoute
import com.mfouad.muslimtv.feature.tools.ToolsRoute
import com.mfouad.muslimtv.feature.prayer.PrayerRoute
import com.mfouad.muslimtv.feature.quran.QuranRoute
import com.mfouad.muslimtv.feature.settings.SettingsRoute
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable


/** The six sections on the navigation rail, in order. */
enum class TopLevel(val route: Any, val routeClass: KClass<*>, @StringRes val label: Int, val icon: ImageVector) {
    HOME(HomeRoute, HomeRoute::class, R.string.nav_home, Icons.Rounded.Home),
    QURAN(QuranRoute, QuranRoute::class, R.string.nav_quran, Icons.AutoMirrored.Rounded.MenuBook),
    AZKAR(AzkarRoute, AzkarRoute::class, R.string.nav_azkar, Icons.Rounded.AutoAwesome),
    PRAYER(PrayerRoute, PrayerRoute::class, R.string.nav_prayer, Icons.Rounded.Mosque),
    TOOLS(ToolsRoute, ToolsRoute::class, R.string.nav_tools, Icons.Rounded.Widgets),
    SETTINGS(SettingsRoute, SettingsRoute::class, R.string.nav_settings, Icons.Rounded.Settings),
    ;

    companion object {
        fun of(destination: NavDestination?): TopLevel? =
            destination?.let { d -> entries.firstOrNull { d.hasRoute(it.routeClass) } }
    }
}
