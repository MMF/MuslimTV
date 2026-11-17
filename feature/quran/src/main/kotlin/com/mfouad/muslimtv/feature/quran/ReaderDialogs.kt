package com.mfouad.muslimtv.feature.quran

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.DialogInitialFocus
import com.mfouad.muslimtv.core.designsystem.component.DialogPanel
import com.mfouad.muslimtv.core.designsystem.component.ListRow
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.designsystem.theme.NaskhFamily
import com.mfouad.muslimtv.core.media.PlaybackProblem
import com.mfouad.muslimtv.core.model.Reciter
import com.mfouad.muslimtv.core.ui.isArabicUi
import kotlinx.coroutines.launch

@Composable
internal fun AyahMenu(
    title: String,
    bookmarked: Boolean,
    translationShown: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onBookmark: () -> Unit,
    onMemorize: () -> Unit,
    onTafsir: () -> Unit,
    onTranslation: () -> Unit,
    onOpenPlayer: () -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    DialogPanel(onDismiss) {
        Text(title, style = MtvTheme.type.title)
        ListRow(stringResource(R.string.menu_play_here), onPlay, Modifier.focusRequester(first), icon = Icons.Rounded.PlayArrow, showChevron = false)
        if (isPlaying) ListRow(stringResource(R.string.ctl_full_screen), onOpenPlayer, icon = Icons.Rounded.Fullscreen, showChevron = false)
        ListRow(
            stringResource(if (bookmarked) R.string.menu_remove_bookmark else R.string.menu_bookmark),
            onBookmark,
            icon = if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            showChevron = false,
        )
        ListRow(stringResource(R.string.menu_memorize), onMemorize, icon = Icons.Rounded.Psychology, showChevron = false)
        ListRow(stringResource(R.string.menu_tafsir), onTafsir, icon = Icons.AutoMirrored.Rounded.MenuBook, showChevron = false)
        ListRow(
            stringResource(if (translationShown) R.string.menu_hide_translation else R.string.menu_show_translation),
            onTranslation,
            icon = Icons.Rounded.Translate,
            showChevron = false,
        )
    }
    DialogInitialFocus(first)
}

@Composable
internal fun TafsirDialog(title: String, text: String, onClose: () -> Unit) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val body = remember { FocusRequester() }
    DialogPanel(onClose, Modifier.heightIn(max = 460.dp)) {
        Text(title, style = MtvTheme.type.title)
        // The text area itself takes focus, and Up/Down scroll it.
        Text(
            text,
            style = MtvTheme.type.body.copy(fontFamily = NaskhFamily, textDirection = TextDirection.Rtl),
            color = MtvTheme.colors.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .focusRequester(body)
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (e.key) {
                        Key.DirectionDown -> { scope.launch { scroll.animateScrollBy(160f) }; scroll.canScrollForward }
                        Key.DirectionUp -> { scope.launch { scroll.animateScrollBy(-160f) }; scroll.canScrollBackward }
                        else -> false
                    }
                }
                .focusable()
                .verticalScroll(scroll),
        )
        MtvButton(stringResource(R.string.close), onClose)
    }
    DialogInitialFocus(body)
}

@Composable
internal fun PlaybackProblemDialog(
    problem: PlaybackProblem,
    surahName: String,
    onPlayWith: (Reciter) -> Unit,
    onRetry: () -> Unit,
    onDownloads: () -> Unit,
    onDismiss: () -> Unit,
) {
    val action = remember { FocusRequester() }
    val arabic = isArabicUi()
    DialogPanel(onDismiss) {
        when (problem) {
            is PlaybackProblem.NotDownloaded -> {
                Text(stringResource(R.string.problem_not_downloaded_title), style = MtvTheme.type.title)
                Text(
                    stringResource(R.string.problem_not_downloaded, surahName, problem.reciter.displayName(arabic)),
                    style = MtvTheme.type.body,
                    color = MtvTheme.colors.onBackgroundMuted,
                )
                val alt = problem.alternatives.firstOrNull()
                if (alt != null) {
                    MtvButton(stringResource(R.string.problem_play_with, alt.displayName(arabic)), { onPlayWith(alt) }, Modifier.focusRequester(action))
                } else {
                    MtvButton(stringResource(R.string.problem_go_downloads), onDownloads, Modifier.focusRequester(action))
                }
            }
            is PlaybackProblem.Network -> {
                Text(stringResource(R.string.problem_network_title), style = MtvTheme.type.title)
                Text(stringResource(R.string.problem_network), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
                MtvButton(stringResource(com.mfouad.muslimtv.core.ui.R.string.action_retry), onRetry, Modifier.focusRequester(action))
            }
        }
    }
    DialogInitialFocus(action)
}
