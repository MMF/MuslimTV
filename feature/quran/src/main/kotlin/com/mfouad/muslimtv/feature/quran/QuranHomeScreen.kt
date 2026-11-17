package com.mfouad.muslimtv.feature.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.data.quran.QuranSearchResult
import com.mfouad.muslimtv.core.designsystem.component.ChoiceRow
import com.mfouad.muslimtv.core.designsystem.component.FocusCard
import com.mfouad.muslimtv.core.designsystem.component.ListRow
import com.mfouad.muslimtv.core.designsystem.component.QuranText
import com.mfouad.muslimtv.core.designsystem.component.TabChips
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.Surah
import com.mfouad.muslimtv.core.ui.SearchField
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter

@Composable
fun QuranHomeEntry(
    onOpen: (AyahRef) -> Unit,
    onNewTahfeez: () -> Unit,
    onKids: () -> Unit,
    onResume: (Long) -> Unit,
    viewModel: QuranHomeViewModel = hiltViewModel(),
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val focus = rememberFocusRestoreState()
    val tabs = listOf(
        stringResource(R.string.tab_surahs),
        stringResource(R.string.tab_juz),
        stringResource(R.string.tab_bookmarks),
        stringResource(R.string.tab_memorize),
        stringResource(R.string.tab_khatmah),
        stringResource(R.string.tab_reciters),
        stringResource(R.string.tab_search),
    )
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        // The rail already says "Quran"; the tabs are the header.
        TabChips(tabs, tab, viewModel::selectTab)
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> {
                    val downloaded by viewModel.downloadedSurahs.collectAsStateWithLifecycle()
                    SurahGrid(viewModel.quran.surahs, downloaded, focus) { onOpen(AyahRef(it.number, 1)) }
                }
                1 -> JuzGrid(viewModel, focus, onOpen)
                2 -> BookmarksList(viewModel, focus, onOpen)
                3 -> com.mfouad.muslimtv.feature.quran.tahfeez.TahfeezTab(focus, onNewTahfeez, onKids, onResume)
                4 -> KhatmahTab(focus, onOpen)
                5 -> RecitersList(viewModel, focus)
                else -> SearchTab(viewModel, onOpen)
            }
        }
    }
    // Opening the Quran section lands on the first surah; coming back lands where you were.
    RestoreFocusEffect(focus, if (tab == 0) "s1" else null)
}

@Composable
private fun SurahGrid(surahs: List<Surah>, downloaded: Set<Int>, focus: com.mfouad.muslimtv.core.designsystem.focus.FocusRestoreState, onOpen: (Surah) -> Unit) {
    val f = rememberTimeFormatter()
    val arabic = isArabicUi()
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = rememberLazyGridState(),
        verticalArrangement = Arrangement.spacedBy(MtvSpacing.m),
        horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(surahs, key = { it.number }) { s ->
            FocusCard(
                onClick = { onOpen(s) },
                modifier = Modifier.height(168.dp).restorableFocus(focus, "s${s.number}"),
                contentPadding = MtvSpacing.m,
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                    Box(
                        Modifier.size(48.dp).background(MtvTheme.colors.primary.copy(alpha = 0.5f), MtvShapes.small),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(f.number(s.number), style = MtvTheme.type.label, color = MtvTheme.colors.goldSoft)
                        // Downloaded badge on the number tile, so it costs no text space.
                        if (s.number in downloaded) {
                            androidx.tv.material3.Icon(
                                Icons.Rounded.DownloadDone, stringResource(R.string.downloaded_all),
                                Modifier.size(20.dp).align(Alignment.BottomEnd).background(MtvTheme.colors.background, MtvShapes.pill),
                                tint = MtvTheme.colors.success,
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        QuranText(s.arabicName, sizeSp = 28, lineHeightEm = 1.2f, textAlign = TextAlign.Start, color = MtvTheme.colors.goldSoft, modifier = Modifier.fillMaxWidth())
                        if (!arabic) Text(s.transliteration, style = MtvTheme.type.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${f.number(s.ayahCount)} · ${s.revelation.label()}",
                            style = MtvTheme.type.caption,
                            color = MtvTheme.colors.onBackgroundMuted,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JuzGrid(vm: QuranHomeViewModel, focus: com.mfouad.muslimtv.core.designsystem.focus.FocusRestoreState, onOpen: (AyahRef) -> Unit) {
    val f = rememberTimeFormatter()
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        verticalArrangement = Arrangement.spacedBy(MtvSpacing.m),
        horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(vm.quran.juzs, key = { it.number }) { j ->
            val surah = vm.quran.surah(j.start.surah)
            FocusCard(onClick = { onOpen(j.start) }, modifier = Modifier.height(120.dp).restorableFocus(focus, "j${j.number}"), contentPadding = MtvSpacing.m) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.juz_number, f.number(j.number)), style = MtvTheme.type.title, color = MtvTheme.colors.goldSoft)
                    Text(
                        stringResource(R.string.juz_starts, surah.displayName(), f.number(j.start.ayah)),
                        style = MtvTheme.type.caption,
                        color = MtvTheme.colors.onBackgroundMuted,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarksList(vm: QuranHomeViewModel, focus: com.mfouad.muslimtv.core.designsystem.focus.FocusRestoreState, onOpen: (AyahRef) -> Unit) {
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    if (bookmarks.isEmpty()) {
        Text(stringResource(R.string.no_bookmarks), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted, modifier = Modifier.widthIn(max = 720.dp))
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(MtvSpacing.s), contentPadding = PaddingValues(8.dp)) {
        items(bookmarks, key = { it.ref.toString() }) { b ->
            val s = vm.quran.surah(b.ref.surah)
            ListRow(
                title = stringResource(R.string.ayah_ref, s.displayName(), f.number(b.ref.ayah)),
                onClick = { onOpen(b.ref) },
                modifier = Modifier.restorableFocus(focus, "b${b.ref}"),
            )
        }
    }
}

@Composable
private fun RecitersList(vm: QuranHomeViewModel, focus: com.mfouad.muslimtv.core.designsystem.focus.FocusRestoreState) {
    val rows by vm.reciters.collectAsStateWithLifecycle()
    val current by vm.reciterId.collectAsStateWithLifecycle()
    val arabic = isArabicUi()
    val f = rememberTimeFormatter()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(MtvSpacing.s), contentPadding = PaddingValues(8.dp), modifier = Modifier.widthIn(max = 1000.dp)) {
        items(rows, key = { it.reciter.id }) { row ->
            val r = row.reciter
            ChoiceRow(
                title = r.displayName(arabic),
                selected = r.id == current,
                badge = when {
                    row.downloadedSurahs == 114 -> stringResource(R.string.downloaded_all)
                    row.downloadedSurahs > 0 -> stringResource(R.string.downloaded_count, f.number(row.downloadedSurahs))
                    else -> null
                },
                trailing = r.style.label() + if (r.hasWordTimings) " · " + stringResource(R.string.word_timing) else "",
                onClick = { vm.chooseReciter(r) },
                modifier = Modifier.restorableFocus(focus, "r${r.id}"),
            )
        }
    }
}

@Composable
private fun SearchTab(vm: QuranHomeViewModel, onOpen: (AyahRef) -> Unit) {
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    Column(Modifier.widthIn(max = 1000.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        SearchField(query, vm::setQuery, stringResource(R.string.search_hint))
        if (query.isNotBlank() && results.isEmpty()) {
            Text(stringResource(R.string.search_empty, query), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(MtvSpacing.s), contentPadding = PaddingValues(4.dp)) {
            items(results, key = { it.toString() }) { r ->
                when (r) {
                    is QuranSearchResult.SurahMatch -> ListRow(
                        title = r.surah.displayName(),
                        subtitle = stringResource(R.string.ayah_count, f.number(r.surah.ayahCount)),
                        onClick = { onOpen(AyahRef(r.surah.number, 1)) },
                    )
                    is QuranSearchResult.AyahMatch -> ListRow(
                        title = stringResource(R.string.ayah_ref, r.surah.displayName(), f.number(r.ayah)),
                        onClick = { onOpen(AyahRef(r.surah.number, r.ayah)) },
                    )
                }
            }
        }
    }
}
