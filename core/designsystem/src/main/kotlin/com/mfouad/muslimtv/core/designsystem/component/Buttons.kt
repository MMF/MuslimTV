package com.mfouad.muslimtv.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme

enum class ButtonStyle { Primary, Secondary }

/** Large, high-contrast button. Primary = gold, the one main action on a screen. */
@Composable
fun MtvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = MtvTheme.colors
    val primary = style == ButtonStyle.Primary
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(MtvShapes.button),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (primary) c.gold.copy(alpha = 0.88f) else c.surface,
            contentColor = if (primary) c.onGold else c.onBackground,
            focusedContainerColor = if (primary) c.goldSoft else c.surfaceFocused,
            focusedContentColor = if (primary) c.onGold else c.onBackground,
            pressedContainerColor = if (primary) c.gold else c.surfaceFocused,
            pressedContentColor = if (primary) c.onGold else c.onBackground,
            disabledContainerColor = c.surface.copy(alpha = 0.5f),
            disabledContentColor = c.onBackgroundFaint,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.5.dp, if (primary) c.onBackground else c.gold), shape = MtvShapes.button),
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(c.gold.copy(alpha = 0.45f), 12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Text(text, style = MtvTheme.type.label)
        }
    }
}

/** Square icon button for secondary actions such as voice search. */
@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MtvTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(MtvShapes.button),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = c.surface,
            contentColor = c.onBackground,
            focusedContainerColor = c.surfaceFocused,
            focusedContentColor = c.gold,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.5.dp, c.gold), shape = MtvShapes.button),
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(c.gold.copy(alpha = 0.45f), 12.dp)),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.padding(16.dp).size(32.dp))
    }
}
