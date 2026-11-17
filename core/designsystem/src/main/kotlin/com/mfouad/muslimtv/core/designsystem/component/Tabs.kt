package com.mfouad.muslimtv.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme

/**
 * TV tabs: moving focus onto a tab selects it (no OK needed), Down goes into its content.
 * [selectedRequester] lets the screen put focus back on the selected tab.
 */
@Composable
fun TabChips(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedRequester: FocusRequester? = null,
) {
    val c = MtvTheme.colors
    // Start inset: keeps the tabs from being the "nearest thing to the left" of the content below,
    // so Left from the first column goes to the rail.
    // Scrolls sideways when tabs are wider than the screen; the focused tab is brought into view.
    Row(modifier.horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tabs.forEachIndexed { i, label ->
            val isSelected = i == selected
            Surface(
                selected = isSelected,
                onClick = { onSelect(i) },
                modifier = Modifier
                    .onFocusChanged { if (it.isFocused && !isSelected) onSelect(i) }
                    .then(if (isSelected && selectedRequester != null) Modifier.focusRequester(selectedRequester) else Modifier),
                shape = SelectableSurfaceDefaults.shape(MtvShapes.pill),
                colors = SelectableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = c.onBackgroundMuted,
                    focusedContainerColor = c.surfaceFocused,
                    focusedContentColor = c.onBackground,
                    selectedContainerColor = c.primary.copy(alpha = 0.4f),
                    selectedContentColor = c.goldSoft,
                    focusedSelectedContainerColor = c.gold,
                    focusedSelectedContentColor = c.onGold,
                ),
                border = SelectableSurfaceDefaults.border(
                    focusedBorder = Border(BorderStroke(2.dp, c.gold), shape = MtvShapes.pill),
                ),
                scale = SelectableSurfaceDefaults.scale(focusedScale = 1.05f),
            ) {
                Text(label, style = MtvTheme.type.label, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
            }
        }
    }
}
