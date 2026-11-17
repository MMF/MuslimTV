package com.mfouad.muslimtv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Calm, premium palette: near-black green surfaces, deep green, warm gold accent. */
@Immutable
data class MtvColors(
    val background: Color = Color(0xFF0B1512),
    val backgroundElevated: Color = Color(0xFF0F1D19),
    val surface: Color = Color(0xFF13241F),
    val surfaceFocused: Color = Color(0xFF1B3A31),
    val primary: Color = Color(0xFF1F6F5B),
    val primaryBright: Color = Color(0xFF2E8B71),
    val gold: Color = Color(0xFFD4AF61),
    val goldSoft: Color = Color(0xFFE8CF95),
    val onBackground: Color = Color(0xFFF3EEDF),
    val onBackgroundMuted: Color = Color(0xFFB9B4A6),
    val onBackgroundFaint: Color = Color(0xFF7D7A70),
    val onGold: Color = Color(0xFF1A1407),
    val error: Color = Color(0xFFE59A8C),
    val success: Color = Color(0xFF8FD1B4),
    val scrim: Color = Color(0xCC07100D),
)

val LocalMtvColors = staticCompositionLocalOf { MtvColors() }
