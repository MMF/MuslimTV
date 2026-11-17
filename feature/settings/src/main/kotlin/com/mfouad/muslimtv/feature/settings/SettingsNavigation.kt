package com.mfouad.muslimtv.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable data object SettingsRoute
@Serializable data class SettingsChoiceRoute(val key: String)
@Serializable data object SettingsCityRoute
@Serializable data object SettingsAdjustmentsRoute
@Serializable data object SettingsQuranFontRoute
@Serializable data object CreditsRoute
@Serializable data object ProfilesRoute

/** Keys for the single-choice screens. */
internal object Choice {
    const val LANGUAGE = "language"
    const val METHOD = "method"
    const val MADHAB = "madhab"
    const val HIGH_LATITUDE = "high_latitude"
    const val TIME_FORMAT = "time_format"
    const val HIJRI = "hijri"
    const val AMBIENT = "ambient"
    const val RAMADAN = "ramadan"
}

fun NavController.navigateToCitySettings() = navigate(SettingsCityRoute)

fun NavGraphBuilder.settingsGraph(navController: NavController, versionName: String, onDownloads: () -> Unit, onAmbientNow: () -> Unit) {
    val up: () -> Unit = { navController.popBackStack() }
    composable<SettingsRoute> {
        SettingsEntry(
            versionName = versionName,
            onChoice = { navController.navigate(SettingsChoiceRoute(it)) },
            onCity = { navController.navigate(SettingsCityRoute) },
            onAdjustments = { navController.navigate(SettingsAdjustmentsRoute) },
            onQuranFont = { navController.navigate(SettingsQuranFontRoute) },
            onCredits = { navController.navigate(CreditsRoute) },
            onDownloads = onDownloads,
            onAmbientNow = onAmbientNow,
            onProfiles = { navController.navigate(ProfilesRoute) },
        )
    }
    composable<SettingsChoiceRoute> { entry -> ChoiceEntry(entry.toRoute<SettingsChoiceRoute>().key, onDone = up) }
    composable<SettingsCityRoute> { CitySettingsEntry(onDone = up) }
    composable<SettingsAdjustmentsRoute> { AdjustmentsEntry() }
    composable<SettingsQuranFontRoute> { QuranFontEntry() }
    composable<CreditsRoute> { CreditsScreen() }
    composable<ProfilesRoute> { ProfilesEntry() }
}
