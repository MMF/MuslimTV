package com.mfouad.muslimtv.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object MtvTheme {
    val colors: MtvColors
        @Composable @ReadOnlyComposable get() = LocalMtvColors.current
    val type: MtvTypography
        @Composable @ReadOnlyComposable get() = LocalMtvTypography.current
}

object MtvShapes {
    val card = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(50)
    val small = RoundedCornerShape(12.dp)
}

/** Soft 200–300 ms transitions everywhere. */
object MtvMotion {
    const val SHORT = 200
    const val MEDIUM = 250
    const val LONG = 300
    val Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    fun <T> standard(durationMillis: Int = MEDIUM): FiniteAnimationSpec<T> = tween(durationMillis, easing = Easing)
}

object MtvSpacing {
    val xs = 8.dp
    val s = 12.dp
    val m = 20.dp
    val l = 28.dp
    val xl = 40.dp
}

@Composable
fun MuslimTvTheme(arabic: Boolean, content: @Composable () -> Unit) {
    val colors = remember { MtvColors() }
    val type = remember(arabic) { mtvTypography(arabic) }
    val scheme = darkColorScheme(
        primary = colors.primary,
        onPrimary = colors.onBackground,
        secondary = colors.gold,
        onSecondary = colors.onGold,
        background = colors.background,
        onBackground = colors.onBackground,
        surface = colors.surface,
        onSurface = colors.onBackground,
        surfaceVariant = colors.surface,
        onSurfaceVariant = colors.onBackgroundMuted,
        border = colors.gold,
        error = colors.error,
    )
    CompositionLocalProvider(
        LocalMtvColors provides colors,
        LocalMtvTypography provides type,
    ) {
        MaterialTheme(colorScheme = scheme) {
            // Text outside a Surface would otherwise default to black.
            CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
        }
    }
}
