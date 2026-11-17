package com.mfouad.muslimtv.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.AzkarCategory
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(
    onOpenPrayer: () -> Unit,
    onContinueQuran: (AyahRef) -> Unit,
    onOpenAzkar: (AzkarCategory) -> Unit,
    onOpenAyah: (AyahRef) -> Unit,
    onChooseCity: () -> Unit,
) {
    composable<HomeRoute> { HomeEntry(onOpenPrayer, onContinueQuran, onOpenAzkar, onOpenAyah, onChooseCity) }
}
