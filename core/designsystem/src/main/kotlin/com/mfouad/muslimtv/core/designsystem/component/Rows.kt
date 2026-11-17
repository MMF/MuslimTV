package com.mfouad.muslimtv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme

/** A settings-style row: title, optional subtitle, current value, and a chevron. */
@Composable
fun ListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
) {
    FocusCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = MtvShapes.small,
        focusedScale = 1.02f,
        contentPadding = MtvSpacing.m,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
            if (icon != null) Icon(icon, null, Modifier.size(32.dp), tint = MtvTheme.colors.gold)
            Column(Modifier.weight(1f)) {
                Text(title, style = MtvTheme.type.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (value != null) {
                Text(value, style = MtvTheme.type.body, color = MtvTheme.colors.goldSoft, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.8f, fill = false))
            }
            if (showChevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(32.dp), tint = MtvTheme.colors.onBackgroundMuted)
        }
    }
}

/** A row in a single-choice list; the chosen one shows a gold check. */
@Composable
fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    trailing: String? = null,
) {
    FocusCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MtvShapes.small,
        focusedScale = 1.02f,
        contentPadding = 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = MtvSpacing.m, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m),
        ) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                if (selected) Icon(Icons.Rounded.Check, null, Modifier.size(32.dp), tint = MtvTheme.colors.gold)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MtvTheme.type.bodyStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // Badges sit under the title so they never squeeze it.
                if (badge != null) Pill(badge, Modifier.padding(top = 4.dp))
                if (subtitle != null) Text(subtitle, style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) {
                Text(trailing, style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 230.dp))
            }
        }
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, emphasized: Boolean = true) {
    val c = MtvTheme.colors
    Text(
        text = text,
        style = MtvTheme.type.caption,
        color = if (emphasized) c.onGold else c.onBackground,
        modifier = modifier
            .background(if (emphasized) c.gold else c.surfaceFocused, MtvShapes.pill)
            .padding(horizontal = 14.dp, vertical = 2.dp),
    )
}

@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    Box(modifier.width(56.dp).height(3.dp).background(MtvTheme.colors.gold, MtvShapes.pill))
}
