package com.mfouad.muslimtv.core.designsystem.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.android.awaitFrame

/**
 * Remembers which item had focus on a screen, across navigation, so that coming back with
 * Back puts focus exactly where the user left it. The key is saved in the back-stack entry.
 */
@Stable
class FocusRestoreState internal constructor(var lastKey: String?) {
    private val requesters = HashMap<String, FocusRequester>()

    fun requester(key: String): FocusRequester = requesters.getOrPut(key) { FocusRequester() }

    /** The requester of the item focused last on this screen, if it is still on screen. */
    fun lastRequester(): FocusRequester? = lastKey?.let { requesters[it] }

    /** Focuses the remembered item, or [defaultKey] the first time the screen opens. */
    suspend fun restore(defaultKey: String?) {
        val key = lastKey?.takeIf { requesters.containsKey(it) } ?: defaultKey ?: return
        requester(key).requestFocusSafely()
    }

    internal companion object {
        val Saver = Saver<FocusRestoreState, String>(save = { it.lastKey ?: "" }, restore = { FocusRestoreState(it.ifEmpty { null }) })
    }
}

@Composable
fun rememberFocusRestoreState(): FocusRestoreState =
    rememberSaveable(saver = FocusRestoreState.Saver) { FocusRestoreState(null) }

fun Modifier.restorableFocus(state: FocusRestoreState, key: String): Modifier =
    this.focusRequester(state.requester(key)).onFocusChanged { if (it.isFocused) state.lastKey = key }

/** Put this once per screen, after the restorable items. */
@Composable
fun RestoreFocusEffect(state: FocusRestoreState, defaultKey: String?, enabled: Boolean = true) {
    LaunchedEffect(enabled) { if (enabled) state.restore(defaultKey) }
    val registry = LocalContentFocus.current
    DisposableEffect(state) {
        registry.active = state
        onDispose { if (registry.active === state) registry.active = null }
    }
}

/** Which screen's focus state is showing, so the rail can send focus back to it. */
class ContentFocusRegistry {
    var active: FocusRestoreState? = null
}

val LocalContentFocus = staticCompositionLocalOf { ContentFocusRegistry() }

/**
 * Requests focus once the target is attached. A requester that is not yet laid out throws,
 * so retry for a few frames instead of crashing or silently losing focus.
 */
suspend fun FocusRequester.requestFocusSafely(maxFrames: Int = 10): Boolean {
    repeat(maxFrames) {
        awaitFrame()
        if (runCatching { requestFocus() }.getOrDefault(false)) return true
    }
    return false
}

/**
 * Keeps Up/Down presses inside this area (a list, the rail, a screen's content). Focus can
 * still leave sideways, which is how the user moves between the rail and the content.
 */
fun Modifier.trapVerticalFocus(): Modifier = this
    .focusProperties {
        onExit = {
            if (requestedFocusDirection == FocusDirection.Up || requestedFocusDirection == FocusDirection.Down) {
                cancelFocusChange()
            }
        }
    }
    .focusGroup()
