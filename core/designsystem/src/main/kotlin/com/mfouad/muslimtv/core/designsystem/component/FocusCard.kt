package com.mfouad.muslimtv.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme

/**
 * The one focusable building block. Focus is always obvious: the card scales up, gets a gold
 * border and a soft gold glow, and its surface brightens.
 */
@Composable
fun FocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MtvShapes.card,
    focusedScale: Float = 1.05f,
    containerColor: Color = MtvTheme.colors.surface,
    focusedContainerColor: Color = MtvTheme.colors.surfaceFocused,
    contentPadding: Dp = MtvSpacing.l,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MtvTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = colors.onBackground,
            focusedContainerColor = focusedContainerColor,
            focusedContentColor = colors.onBackground,
            pressedContainerColor = focusedContainerColor,
            pressedContentColor = colors.onBackground,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = colors.onBackgroundFaint,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale, pressedScale = focusedScale - 0.02f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, colors.onBackground.copy(alpha = 0.06f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.5.dp, colors.gold), shape = shape),
            pressedBorder = Border(BorderStroke(2.5.dp, colors.goldSoft), shape = shape),
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(colors.gold.copy(alpha = 0.45f), 14.dp)),
    ) {
        Box(Modifier.padding(contentPadding)) { content() }
    }
}

/** Content color inside a card, for icons drawn with tint = LocalContentColor. */
@Composable
fun contentColor(): Color = LocalContentColor.current
