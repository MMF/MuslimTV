package com.mfouad.muslimtv.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit, onExit: () -> Unit) {
    composable<OnboardingRoute> { OnboardingEntry(onFinished = onFinished, onExit = onExit) }
}
