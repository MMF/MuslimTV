package com.mfouad.muslimtv.feature.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.QuranText
import com.mfouad.muslimtv.core.designsystem.focus.requestFocusSafely
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.QuranText as QText
import com.mfouad.muslimtv.core.model.ReaderAyah
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import kotlinx.coroutines.launch

/** After this long without a key press, follow-along may move focus to the recited ayah. */
private const val FOLLOW_IDLE_MS = 4_000L

@OptIn(UnstableApi::class)
@Composable
fun ReaderEntry(
    onOpenPlayer: () -> Unit,
    onMemorize: (Int, Int) -> Unit,
    onOpenDownloads: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val follow by viewModel.followAlong.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val tafsir by viewModel.tafsir.collectAsStateWithLifecycle()
    val problem by viewModel.problem.collectAsStateWithLifecycle()
    val surah = state.surah ?: return
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val requesters = remember(state.ayahs.size) { List(state.ayahs.size) { FocusRequester() } }
    var focused by rememberSaveable { mutableStateOf(state.startAyah) }
    var menuFor by rememberSaveable { mutableStateOf<Int?>(null) }
    var lastKeyAt by remember { mutableLongStateOf(0L) }
    val f = rememberTimeFormatter()
    val density = LocalDensity.current

    fun focusAyah(ayah: Int, center: Boolean = true) {
        val index = ayah - 1
        if (index !in requesters.indices) return
        scope.launch {
            val viewport = listState.layoutInfo.viewportSize.height
            if (center || listState.layoutInfo.visibleItemsInfo.none { it.index == index + 1 }) {
                listState.animateScrollToItem(index + 1, -viewport / 4)
            }
            requesters[index].requestFocusSafely()
        }
    }

    // Open at the requested ayah (or where the user was, after Back).
    LaunchedEffect(state.loaded) {
        if (state.loaded && state.ayahs.isNotEmpty()) {
            listState.scrollToItem(focused.coerceIn(1, state.ayahs.size), -with(density) { 120.dp.roundToPx() })
            requesters[(focused - 1).coerceIn(0, requesters.lastIndex)].requestFocusSafely()
        }
    }

    // Follow-along: keep the recited ayah in view, and move focus to it when the remote is idle.
    LaunchedEffect(follow.ayah) {
        val ayah = follow.ayah ?: return@LaunchedEffect
        if (System.currentTimeMillis() - lastKeyAt > FOLLOW_IDLE_MS && menuFor == null) focusAyah(ayah)
    }

    Column(
        Modifier.fillMaxSize().onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown) lastKeyAt = System.currentTimeMillis(); false },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = MtvSpacing.s, vertical = MtvSpacing.m),
            verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs),
        ) {
            item(key = "header") { SurahHeader(surah, follow.isBasmala) }
            itemsIndexed(state.ayahs, key = { _, a -> a.ayah }) { index, ayah ->
                AyahItem(
                    ayah = ayah,
                    fontSp = state.settings.quranFontSizeSp,
                    isRecited = follow.ayah == ayah.ayah && !follow.isBasmala,
                    recitedWord = if (follow.ayah == ayah.ayah) follow.word else null,
                    bookmarked = ayah.ayah in state.bookmarked,
                    onClick = { menuFor = ayah.ayah },
                    modifier = Modifier
                        .focusRequester(requesters[index])
                        .onFocusChanged {
                            if (it.isFocused) {
                                focused = ayah.ayah
                                viewModel.onAyahFocused(ayah.ayah)
                            }
                        }
                        .readerKeys(
                            listState = listState,
                            itemIndex = index + 1,
                            // Quran text runs right to left: Left is the next ayah, Right the previous.
                            onNext = { focusAyah(ayah.ayah + 1, center = false) },
                            onPrevious = { focusAyah(ayah.ayah - 1, center = false) },
                        ),
                )
            }
        }
        if (player.isActive) NowPlayingBar(player.reciter.displayName(isArabicUi()), player.surah.let { if (it > 0) it else surah.number }, player.current?.ayah, player.isPlaying)
    }

    menuFor?.let { ayah ->
        AyahMenu(
            title = stringResource(R.string.ayah_ref, surah.displayName(), f.number(ayah)),
            bookmarked = ayah in state.bookmarked,
            translationShown = state.settings.showTranslation,
            isPlaying = player.isPlaying,
            onPlay = { menuFor = null; viewModel.playFrom(ayah) },
            onBookmark = { menuFor = null; viewModel.setBookmark(ayah, ayah !in state.bookmarked) },
            onMemorize = { menuFor = null; onMemorize(surah.number, ayah) },
            onTafsir = { menuFor = null; viewModel.showTafsir(ayah) },
            onTranslation = { menuFor = null; viewModel.toggleTranslation() },
            onOpenPlayer = { menuFor = null; onOpenPlayer() },
            onDismiss = { menuFor = null; focusAyah(ayah, center = false) },
        )
    }
    tafsir?.let { (ref, text) ->
        TafsirDialog(
            title = stringResource(R.string.tafsir_title, stringResource(R.string.ayah_ref, surah.displayName(), f.number(ref.ayah))),
            text = text ?: stringResource(R.string.tafsir_missing),
            onClose = { viewModel.closeTafsir(); focusAyah(ref.ayah, center = false) },
        )
    }
    problem?.let { p ->
        PlaybackProblemDialog(
            problem = p,
            surahName = surah.displayName(),
            onPlayWith = { r -> viewModel.dismissProblem(); viewModel.playFrom(focused, r) },
            onRetry = viewModel::retry,
            onDownloads = { viewModel.dismissProblem(); onOpenDownloads() },
            onDismiss = viewModel::dismissProblem,
        )
    }
}

@Composable
private fun SurahHeader(surah: com.mfouad.muslimtv.core.model.Surah, basmalaRecited: Boolean) {
    val f = rememberTimeFormatter()
    Column(Modifier.fillMaxWidth().padding(bottom = MtvSpacing.m), horizontalAlignment = Alignment.CenterHorizontally) {
        QuranText("سُورَةُ " + surah.arabicName, sizeSp = 44, lineHeightEm = 1.5f, color = MtvTheme.colors.gold)
        Text(
            "${surah.transliteration} · ${stringResource(R.string.ayah_count, f.number(surah.ayahCount))} · ${surah.revelation.label()}",
            style = MtvTheme.type.caption,
            color = MtvTheme.colors.onBackgroundMuted,
        )
        if (QText.hasBasmalaHeader(surah.number)) {
            QuranText(
                QText.BASMALA,
                sizeSp = 40,
                lineHeightEm = 1.8f,
                color = if (basmalaRecited) MtvTheme.colors.gold else MtvTheme.colors.onBackground,
                modifier = Modifier.padding(top = MtvSpacing.s),
            )
        }
    }
}

@Composable
private fun AyahItem(
    ayah: ReaderAyah,
    fontSp: Int,
    isRecited: Boolean,
    recitedWord: Int?,
    bookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val c = MtvTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(MtvShapes.card),
        // No scaling on Quran text: it stays crisp; focus shows as a gold frame and a lighter surface.
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isRecited) c.primary.copy(alpha = 0.22f) else Color.Transparent,
            contentColor = c.onBackground,
            focusedContainerColor = c.surfaceFocused,
            focusedContentColor = c.onBackground,
            pressedContainerColor = c.surfaceFocused,
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (isRecited) Border(BorderStroke(1.5.dp, c.gold.copy(alpha = 0.5f)), shape = MtvShapes.card) else Border.None,
            focusedBorder = Border(BorderStroke(2.5.dp, c.gold), shape = MtvShapes.card),
        ),
    ) {
        Column(Modifier.padding(horizontal = MtvSpacing.l, vertical = MtvSpacing.s)) {
            Row(verticalAlignment = Alignment.Top) {
                QuranText(
                    text = ayahAnnotated(ayah, recitedWord, isRecited),
                    sizeSp = fontSp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                if (bookmarked) Icon(Icons.Rounded.Bookmark, null, Modifier.size(28.dp), tint = c.gold)
            }
            ayah.translation?.let {
                Text(it, style = MtvTheme.type.body, color = c.onBackgroundMuted, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
            }
        }
    }
}

/** Ayah text with the recited word in gold (when timing exists) and the ayah number ornament. */
@Composable
private fun ayahAnnotated(ayah: ReaderAyah, word: Int?, recited: Boolean): AnnotatedString {
    val gold = MtvTheme.colors.gold
    val goldBg = MtvTheme.colors.gold.copy(alpha = 0.16f)
    return remember(ayah.text, word, recited) {
        buildAnnotatedString {
            QText.tokens(ayah.text).forEachIndexed { i, t ->
                if (i > 0) append(' ')
                if (word != null && t.wordIndex == word) withStyle(SpanStyle(color = gold, background = goldBg)) { append(t.text) }
                else append(t.text)
            }
            append(' ')
            withStyle(SpanStyle(color = gold)) { append(QText.ayahMarker(ayah.ayah)) }
        }
    }
}

/**
 * Up/Down scroll within an ayah taller than the screen before moving to the next one;
 * Left/Right jump between ayahs.
 */
private fun Modifier.readerKeys(
    listState: LazyListState,
    itemIndex: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
): Modifier = onPreviewKeyEvent { e ->
    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (e.key) {
        Key.DirectionLeft -> { onNext(); true }
        Key.DirectionRight -> { onPrevious(); true }
        Key.DirectionDown, Key.DirectionUp -> {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == itemIndex } ?: return@onPreviewKeyEvent false
            val viewportEnd = info.viewportEndOffset
            val step = (info.viewportSize.height * 0.6f)
            val down = e.key == Key.DirectionDown
            val canScroll = if (down) item.offset + item.size > viewportEnd else item.offset < info.viewportStartOffset
            if (canScroll) {
                listState.dispatchRawDelta(if (down) step else -step)
                true
            } else {
                false
            }
        }
        else -> false
    }
}

@Composable
private fun NowPlayingBar(reciter: String, surah: Int, ayah: Int?, playing: Boolean) {
    val f = rememberTimeFormatter()
    Row(
        Modifier
            .fillMaxWidth()
            .background(MtvTheme.colors.surface, MtvShapes.small)
            .padding(horizontal = MtvSpacing.m, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s),
    ) {
        Icon(Icons.Rounded.GraphicEq, null, Modifier.size(26.dp), tint = if (playing) MtvTheme.colors.gold else MtvTheme.colors.onBackgroundMuted)
        Text(
            stringResource(R.string.now_playing, reciter) + (ayah?.let { " · ${f.number(surah)}:${f.number(it)}" } ?: ""),
            style = MtvTheme.type.caption,
            color = MtvTheme.colors.onBackgroundMuted,
        )
        Box(Modifier.weight(1f))
    }
}
