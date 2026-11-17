package com.mfouad.muslimtv.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.theme.MtvMotion
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme

data class RailItem(val key: String, val label: String, val icon: ImageVector)

object NavRailDefaults {
    val CollapsedWidth = 96.dp
    val ExpandedWidth = 280.dp
}

/**
 * Left navigation rail. Collapsed it shows icons only; when it holds focus it expands over the
 * content (no relayout of the screen behind it) and shows labels. Focus enters on the
 * selected item, and OK switches section.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavRail(
    items: List<RailItem>,
    selectedKey: String,
    onSelect: (RailItem) -> Unit,
    modifier: Modifier = Modifier,
    selectedRequester: FocusRequester = remember { FocusRequester() },
    onFocusChanged: (Boolean) -> Unit = {},
    exitTarget: () -> FocusRequester? = { null },
    /** When set, a small offline indicator sits at the bottom of the rail on every section. */
    offlineLabel: String? = null,
    header: @Composable () -> Unit = {},
    /** Optional entry above the sections (e.g. pause the recitation), reachable with Up. */
    action: RailItem? = null,
    onAction: () -> Unit = {},
) {
    var hasFocus by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        if (hasFocus) NavRailDefaults.ExpandedWidth else NavRailDefaults.CollapsedWidth,
        animationSpec = MtvMotion.standard(MtvMotion.MEDIUM),
        label = "railWidth",
    )
    val c = MtvTheme.colors
    Box(
        modifier
            .fillMaxHeight()
            .width(width)
            .background(
                // Opaque under the labels so card text never shows through; soft fade at the edge.
                Brush.horizontalGradient(
                    0f to c.background,
                    0.82f to c.background.copy(alpha = if (hasFocus) 1f else 0.85f),
                    1f to Color.Transparent,
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 20.dp, end = 12.dp, top = 28.dp, bottom = 28.dp)
                .onFocusChanged {
                    hasFocus = it.hasFocus
                    onFocusChanged(it.hasFocus)
                }
                .railFocusRules(selectedRequester, exitTarget)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            header()
            Spacer(Modifier.height(12.dp))
            if (action != null) {
                RailEntry(item = action, selected = false, expanded = hasFocus, onClick = onAction, modifier = Modifier)
                Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(1.dp).background(c.onBackgroundMuted.copy(alpha = 0.25f)))
            }
            items.forEach { item ->
                val selected = item.key == selectedKey
                RailEntry(
                    item = item,
                    selected = selected,
                    expanded = hasFocus,
                    onClick = { onSelect(item) },
                    modifier = if (selected) Modifier.focusRequester(selectedRequester) else Modifier,
                )
            }
            if (offlineLabel != null) {
                Spacer(Modifier.weight(1f))
                Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Rounded.CloudOff, contentDescription = offlineLabel, modifier = Modifier.size(28.dp), tint = c.goldSoft)
                    AnimatedVisibility(hasFocus) {
                        Text(offlineLabel, style = MtvTheme.type.caption, color = c.goldSoft, modifier = Modifier.padding(start = 14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RailEntry(item: RailItem, selected: Boolean, expanded: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = MtvTheme.colors
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = SelectableSurfaceDefaults.shape(MtvShapes.button),
        colors = SelectableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = c.onBackgroundMuted,
            focusedContainerColor = c.surfaceFocused,
            focusedContentColor = c.onBackground,
            selectedContainerColor = c.primary.copy(alpha = 0.35f),
            selectedContentColor = c.goldSoft,
            focusedSelectedContainerColor = c.surfaceFocused,
            focusedSelectedContentColor = c.gold,
        ),
        scale = SelectableSurfaceDefaults.scale(focusedScale = 1.04f),
        border = SelectableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, c.gold), shape = MtvShapes.button),
            focusedSelectedBorder = Border(BorderStroke(2.dp, c.gold), shape = MtvShapes.button),
        ),
        glow = SelectableSurfaceDefaults.glow(focusedGlow = Glow(c.gold.copy(alpha = 0.35f), 10.dp)),
    ) {
        // Fixed height: Arabic labels are taller than the icon, and when the rail expanded they
        // grew every item until the last one (Settings) was pushed off the bottom.
        Row(
            modifier = Modifier.height(52.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(32.dp))
            AnimatedVisibility(expanded, enter = fadeIn(MtvMotion.standard()), exit = fadeOut(MtvMotion.standard(MtvMotion.SHORT))) {
                Text(item.label, style = MtvTheme.type.label, maxLines = 1, modifier = Modifier.padding(start = 18.dp))
            }
        }
    }
}

/**
 * Entering the rail lands on the current section; Up/Down never leave it; leaving sideways goes
 * back to the screen's last focused item instead of whatever is geometrically nearest.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
private fun Modifier.railFocusRules(selected: FocusRequester, exitTarget: () -> FocusRequester?): Modifier = this
    .focusProperties {
        enter = { selected }
        exit = { direction ->
            when (direction) {
                FocusDirection.Up, FocusDirection.Down -> FocusRequester.Cancel
                else -> exitTarget() ?: FocusRequester.Default
            }
        }
    }
    .focusGroup()
