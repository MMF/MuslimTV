package com.mfouad.muslimtv.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import com.mfouad.muslimtv.core.designsystem.focus.requestFocusSafely
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme

/**
 * Every error says what happened and offers exactly one clear action.
 */
@Composable
fun ErrorState(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val focus = remember { FocusRequester() }
    Column(
        modifier = modifier.widthIn(max = 640.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MtvSpacing.m),
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(56.dp), tint = MtvTheme.colors.error)
        if (title != null) Text(title, style = MtvTheme.type.title, textAlign = TextAlign.Center)
        Text(message, style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted, textAlign = TextAlign.Center)
        MtvButton(actionLabel, onAction, Modifier.focusRequester(focus))
    }
    LaunchedEffect(Unit) { focus.requestFocusSafely() }
}

/** Pulsing placeholder, used only while network content loads. */
@Composable
fun Skeleton(modifier: Modifier = Modifier, shape: Shape = MtvShapes.small) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha")
    Box(modifier.alpha(alpha).background(MtvTheme.colors.surfaceFocused, shape))
}

/** Small, calm indicator shown in headers when there is no network. */
@Composable
fun OfflineBadge(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MtvTheme.colors.surface, MtvShapes.pill)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.CloudOff, null, Modifier.size(22.dp), tint = MtvTheme.colors.onBackgroundMuted)
        Text(label, style = MtvTheme.type.caption, color = MtvTheme.colors.onBackgroundMuted)
    }
}

/**
 * Two-button confirmation. Focus starts on the safe choice ([dismissLabel]) so a stray OK
 * press never triggers the destructive one.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val safe = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(MtvTheme.colors.scrim), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .background(MtvTheme.colors.surface, MtvShapes.card)
                    .padding(MtvSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(MtvSpacing.m),
            ) {
                Text(title, style = MtvTheme.type.headline)
                Text(message, style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m), modifier = Modifier.padding(top = MtvSpacing.s)) {
                    MtvButton(dismissLabel, onDismiss, Modifier.focusRequester(safe), style = ButtonStyle.Primary)
                    MtvButton(confirmLabel, onConfirm, style = ButtonStyle.Secondary)
                }
            }
        }
    }
    // The dialog window takes focus a moment after it is attached; focus the safe button after that,
    // otherwise the window's own initial focus can land on the destructive one.
    LaunchedEffect(Unit) {
        safe.requestFocusSafely()
        delay(DIALOG_FOCUS_SETTLE_MS)
        safe.requestFocusSafely()
    }
}

private const val DIALOG_FOCUS_SETTLE_MS = 120L

/** "Step 2 of 3" dots for the first-launch setup. */
@Composable
fun StepIndicator(current: Int, total: Int, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(total) { i ->
            val active = i <= current
            Box(
                Modifier
                    .size(width = if (i == current) 36.dp else 12.dp, height = 12.dp)
                    .background(if (active) MtvTheme.colors.gold else MtvTheme.colors.surfaceFocused, MtvShapes.pill),
            )
        }
        Text(label, style = MtvTheme.type.caption, color = MtvTheme.colors.onBackgroundMuted, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Screen title with a short gold rule underneath. */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MtvTheme.type.headline)
            GoldDivider()
            if (subtitle != null) Text(subtitle, style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
        }
        trailing?.invoke()
    }
}

/** Full-screen scrim with a centered panel; the base for menus and info dialogs. */
@Composable
fun DialogPanel(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(MtvTheme.colors.scrim), contentAlignment = Alignment.Center) {
            Column(
                modifier = modifier
                    .widthIn(max = 720.dp)
                    .background(MtvTheme.colors.surface, MtvShapes.card)
                    .padding(MtvSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(MtvSpacing.s),
                content = content,
            )
        }
    }
}

/** Focus the first item of a dialog once its window has settled. */
@Composable
fun DialogInitialFocus(requester: FocusRequester) {
    LaunchedEffect(Unit) {
        requester.requestFocusSafely()
        delay(DIALOG_FOCUS_SETTLE_MS)
        requester.requestFocusSafely()
    }
}
