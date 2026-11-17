package com.mfouad.muslimtv.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.data.audio.Reciters
import com.mfouad.muslimtv.core.designsystem.component.ButtonStyle
import com.mfouad.muslimtv.core.designsystem.component.ChoiceRow
import com.mfouad.muslimtv.core.designsystem.component.ConfirmDialog
import com.mfouad.muslimtv.core.designsystem.component.DialogInitialFocus
import com.mfouad.muslimtv.core.designsystem.component.DialogPanel
import com.mfouad.muslimtv.core.designsystem.component.FocusCard
import com.mfouad.muslimtv.core.designsystem.component.ListRow
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.OfflineBadge
import com.mfouad.muslimtv.core.designsystem.component.Pill
import com.mfouad.muslimtv.core.designsystem.component.ScreenHeader
import com.mfouad.muslimtv.core.designsystem.component.TabChips
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.downloads.DownloadItem
import com.mfouad.muslimtv.core.downloads.DownloadKind
import com.mfouad.muslimtv.core.downloads.DownloadStatus
import com.mfouad.muslimtv.core.downloads.FailReason
import com.mfouad.muslimtv.core.downloads.StorageVerdict
import com.mfouad.muslimtv.core.model.Reciter
import com.mfouad.muslimtv.core.ui.isArabicUi
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter

/** "1.2 GB" / "340 MB" in the UI language. */
@Composable
internal fun bytes(n: Long): String {
    val locale = rememberTimeFormatter().locale
    val mb = n / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(locale, "%.1f GB", mb / 1024) else String.format(locale, "%.0f MB", mb.coerceAtLeast(1.0))
}

@Composable
fun DownloadsEntry(onAdd: () -> Unit, onReciter: (String) -> Unit, onExtras: () -> Unit, viewModel: DownloadsViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    val focus = rememberFocusRestoreState()
    val f = rememberTimeFormatter()
    val arabic = isArabicUi()
    val active = items.filter { it.status != DownloadStatus.COMPLETED }
    LaunchedEffect(items.count { it.status == DownloadStatus.COMPLETED }) { viewModel.refresh() }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.dl_title), trailing = { if (!online) OfflineBadge(stringResource(com.mfouad.muslimtv.core.ui.R.string.offline)) })
        LazyColumn(Modifier.widthIn(max = 1100.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s), contentPadding = PaddingValues(6.dp)) {
            item(key = "add") {
                MtvButton(stringResource(R.string.dl_add), onAdd, Modifier.restorableFocus(focus, "add"), icon = Icons.Rounded.CloudDownload)
            }
            if (!online && active.isNotEmpty()) item(key = "offline") {
                Text(stringResource(R.string.dl_offline), style = MtvTheme.type.body, color = MtvTheme.colors.goldSoft)
            }
            item(key = "h_active") { Section(stringResource(R.string.dl_active)) }
            if (active.isEmpty()) item(key = "none") { Text(stringResource(R.string.dl_none_active), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted) }
            items(active, key = { it.id }) { DownloadRow(it, viewModel, focus) }
            item(key = "h_storage") { Section(stringResource(R.string.dl_storage)) }
            item(key = "free") {
                Text(stringResource(R.string.dl_free, bytes(storage.availableBytes), bytes(storage.totalBytes)), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
            }
            if (usage.isEmpty()) item(key = "nostore") { Text(stringResource(R.string.dl_no_storage), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted) }
            items(usage, key = { "u" + it.reciter.id }) { u ->
                ListRow(
                    title = u.reciter.displayName(arabic),
                    value = stringResource(R.string.dl_reciter_usage, bytes(u.bytes), f.number(u.surahs.size)),
                    icon = Icons.Rounded.Storage,
                    onClick = { onReciter(u.reciter.id) },
                    modifier = Modifier.restorableFocus(focus, "u" + u.reciter.id),
                )
            }
            item(key = "h_extras") { Section(stringResource(R.string.dl_extras)) }
            item(key = "extras") {
                ListRow(stringResource(R.string.dl_extras), onExtras, Modifier.restorableFocus(focus, "extras"), subtitle = stringResource(R.string.dl_extras_hint), icon = Icons.Rounded.Translate)
            }
        }
    }
    RestoreFocusEffect(focus, "add")
}

@Composable
private fun Section(title: String) {
    Text(title, style = MtvTheme.type.label, color = MtvTheme.colors.gold, modifier = Modifier.padding(top = MtvSpacing.m))
}

@Composable
private fun DownloadRow(item: DownloadItem, vm: DownloadsViewModel, focus: com.mfouad.muslimtv.core.designsystem.focus.FocusRestoreState) {
    val f = rememberTimeFormatter()
    val status = when (item.status) {
        DownloadStatus.QUEUED -> stringResource(R.string.dl_status_queued)
        DownloadStatus.DOWNLOADING -> if (item.kind == DownloadKind.AUDIO) stringResource(R.string.dl_status_downloading, f.number(item.filesDone), f.number(item.filesTotal)) else "…"
        DownloadStatus.PAUSED -> stringResource(R.string.dl_status_paused, f.number(item.filesDone), f.number(item.filesTotal))
        DownloadStatus.FAILED -> stringResource(
            when (item.failReason) {
                FailReason.NETWORK -> R.string.dl_status_failed_network
                FailReason.STORAGE_FULL -> R.string.dl_status_failed_storage
                FailReason.NOT_FOUND -> R.string.dl_status_failed_missing
                else -> R.string.dl_status_failed
            },
        )
        DownloadStatus.COMPLETED -> stringResource(R.string.dl_status_done)
    }
    Row(
        Modifier.fillMaxWidth().background(MtvTheme.colors.surface, MtvShapes.small).padding(horizontal = MtvSpacing.m, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, style = MtvTheme.type.bodyStrong, maxLines = 1)
            Text(status, style = MtvTheme.type.caption, color = if (item.status == DownloadStatus.FAILED) MtvTheme.colors.error else MtvTheme.colors.onBackgroundMuted)
            ProgressBar(item.progress)
        }
        // One main action per row, plus cancel. The same button changes label so focus stays on it.
        if (item.status != DownloadStatus.COMPLETED) {
            val (label, action) = when (item.status) {
                DownloadStatus.PAUSED -> stringResource(R.string.dl_resume) to { vm.resume(item) }
                DownloadStatus.FAILED -> stringResource(R.string.dl_retry) to { vm.retry(item) }
                else -> stringResource(R.string.dl_pause) to { vm.pause(item) }
            }
            MtvButton(label, { action() }, Modifier.restorableFocus(focus, item.id + "p"), style = if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.QUEUED) ButtonStyle.Secondary else ButtonStyle.Primary)
        }
        MtvButton(stringResource(R.string.dl_cancel), { vm.cancel(item) }, Modifier.restorableFocus(focus, item.id + "c"), style = ButtonStyle.Secondary)
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(Modifier.fillMaxWidth().height(8.dp).background(MtvTheme.colors.surfaceFocused, MtvShapes.pill)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(MtvTheme.colors.gold, MtvShapes.pill))
    }
}

@Composable
fun DownloadPickerEntry(onDone: () -> Unit, viewModel: DownloadsViewModel = hiltViewModel()) {
    val offer by viewModel.offer.collectAsStateWithLifecycle()
    val reciterState = produceState<Reciter?>(null) { value = viewModel.currentReciter() }
    var reciterId by rememberSaveable { mutableStateOf<String?>(null) }
    val reciter = reciterId?.let { Reciters.byId(it) } ?: reciterState.value ?: return
    var tab by rememberSaveable { mutableStateOf(0) }
    var choosingReciter by rememberSaveable { mutableStateOf(false) }
    val arabic = isArabicUi()
    val f = rememberTimeFormatter()
    val focus = rememberFocusRestoreState()

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.l)) {
            Text(stringResource(R.string.dl_pick_title, reciter.displayName(arabic)), style = MtvTheme.type.title, modifier = Modifier.weight(1f, fill = false))
            MtvButton(stringResource(R.string.dl_change_reciter), { choosingReciter = true }, Modifier.restorableFocus(focus, "reciter"), style = ButtonStyle.Secondary)
        }
        TabChips(listOf(stringResource(R.string.dl_by_surah), stringResource(R.string.dl_by_juz), stringResource(R.string.dl_whole_quran)), tab, { tab = it })
        when (tab) {
            0 -> LazyVerticalGrid(GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s), horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s), contentPadding = PaddingValues(6.dp)) {
                items(viewModel.surahs, key = { it.number }) { s ->
                    val done = viewModel.isDownloaded(reciter, s)
                    FocusCard(
                        onClick = { if (!done) viewModel.offer(if (arabic) s.arabicName else s.transliteration, reciter, listOf(s.number)) },
                        modifier = Modifier.height(96.dp).restorableFocus(focus, "s${s.number}"),
                        contentPadding = MtvSpacing.s,
                    ) {
                        Column {
                            Text("${f.number(s.number)}. ${if (arabic) s.arabicName else s.transliteration}", style = MtvTheme.type.bodyStrong, maxLines = 1)
                            if (done) Pill(stringResource(R.string.dl_downloaded))
                        }
                    }
                }
            }
            1 -> LazyVerticalGrid(GridCells.Fixed(5), verticalArrangement = Arrangement.spacedBy(MtvSpacing.s), horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s), contentPadding = PaddingValues(6.dp)) {
                items(viewModel.juzs, key = { it.number }) { j ->
                    val next = viewModel.juzs.getOrNull(j.number)?.start
                    // Whole surahs that start inside this juz (recitations are downloaded per surah).
                    val surahs = (j.start.surah..(next?.let { if (it.ayah == 1) it.surah - 1 else it.surah } ?: 114)).toList()
                    val title = stringResource(R.string.dl_by_juz) + " " + f.number(j.number)
                    FocusCard(onClick = { viewModel.offer(title, reciter, surahs) }, modifier = Modifier.height(80.dp).restorableFocus(focus, "j${j.number}"), contentPadding = MtvSpacing.s) {
                        Text(title, style = MtvTheme.type.bodyStrong)
                    }
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                val title = stringResource(R.string.dl_whole_quran)
                MtvButton(title, { viewModel.offer(title, reciter, (1..114).toList()) }, Modifier.restorableFocus(focus, "all"), icon = Icons.Rounded.CloudDownload)
            }
        }
    }
    RestoreFocusEffect(focus, "s1")

    if (choosingReciter) {
        val first = remember { FocusRequester() }
        DialogPanel({ choosingReciter = false }) {
            LazyColumn(Modifier.height(420.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs)) {
                items(Reciters.all, key = { it.id }) { r ->
                    ChoiceRow(r.displayName(arabic), r.id == reciter.id, { reciterId = r.id; choosingReciter = false }, if (r.id == reciter.id) Modifier.focusRequester(first) else Modifier)
                }
            }
        }
        DialogInitialFocus(first)
    }
    offer?.let { o ->
        val confirm = remember { FocusRequester() }
        DialogPanel(viewModel::dismissOffer) {
            Text(stringResource(R.string.dl_confirm_title, o.title), style = MtvTheme.type.title)
            if (o.bytes == null || o.storage == null) {
                Text(stringResource(R.string.dl_measuring), style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
                MtvButton(stringResource(R.string.dl_back), viewModel::dismissOffer, Modifier.focusRequester(confirm), style = ButtonStyle.Secondary)
            } else {
                Text(stringResource(R.string.dl_confirm_size, bytes(o.bytes), bytes(o.storage.availableBytes)), style = MtvTheme.type.body)
                when (o.verdict) {
                    StorageVerdict.NOT_ENOUGH -> {
                        Text(stringResource(R.string.dl_confirm_full), style = MtvTheme.type.body, color = MtvTheme.colors.error)
                        MtvButton(stringResource(R.string.dl_go_storage), { viewModel.dismissOffer(); onDone() }, Modifier.focusRequester(confirm))
                    }
                    else -> {
                        if (o.verdict == StorageVerdict.LOW_AFTER) Text(stringResource(R.string.dl_confirm_low), style = MtvTheme.type.body, color = MtvTheme.colors.goldSoft)
                        MtvButton(stringResource(R.string.dl_start), { viewModel.confirmOffer(); onDone() }, Modifier.focusRequester(confirm), icon = Icons.Rounded.CloudDownload)
                    }
                }
            }
        }
        DialogInitialFocus(confirm)
    }
}

@Composable
fun ReciterStorageEntry(reciterId: String, viewModel: DownloadsViewModel = hiltViewModel()) {
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val u = usage.firstOrNull { it.reciter.id == reciterId }
    val arabic = isArabicUi()
    val f = rememberTimeFormatter()
    val focus = rememberFocusRestoreState()
    var confirmAll by rememberSaveable { mutableStateOf(false) }
    val reciter = Reciters.byId(reciterId)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(reciter.displayName(arabic), subtitle = u?.let { stringResource(R.string.dl_reciter_usage, bytes(it.bytes), f.number(it.surahs.size)) })
        if (u != null) {
            MtvButton(stringResource(R.string.dl_delete_all), { confirmAll = true }, Modifier.restorableFocus(focus, "all"), style = ButtonStyle.Secondary, icon = Icons.Rounded.Delete)
            LazyColumn(Modifier.widthIn(max = 900.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs), contentPadding = PaddingValues(6.dp)) {
                items(u.surahs, key = { it }) { n ->
                    val s = viewModel.surahs[n - 1]
                    ListRow(
                        title = "${f.number(n)}. ${if (arabic) s.arabicName else s.transliteration}",
                        value = stringResource(R.string.dl_delete),
                        icon = Icons.Rounded.CheckCircle,
                        showChevron = false,
                        onClick = { viewModel.deleteSurah(reciterId, n) },
                        modifier = Modifier.restorableFocus(focus, "s$n"),
                    )
                }
            }
        }
    }
    RestoreFocusEffect(focus, "all")
    if (confirmAll) {
        ConfirmDialog(
            title = stringResource(R.string.dl_delete_all),
            message = stringResource(R.string.dl_delete_all_confirm, reciter.displayName(arabic)),
            confirmLabel = stringResource(R.string.dl_delete),
            dismissLabel = stringResource(R.string.dl_keep),
            onConfirm = { confirmAll = false; viewModel.deleteReciter(reciterId) },
            onDismiss = { confirmAll = false },
        )
    }
}

@Composable
fun ExtrasEntry(viewModel: DownloadsViewModel = hiltViewModel()) {
    val installed by viewModel.installedTranslations.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val focus = rememberFocusRestoreState()
    LaunchedEffect(items.count { it.status == DownloadStatus.COMPLETED }) { viewModel.refresh() }
    val all = listOf("en.pickthall" to "English · Pickthall") + viewModel.extras.map { it.id to it.title }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.dl_extras), subtitle = stringResource(R.string.dl_extras_hint))
        LazyColumn(Modifier.widthIn(max = 900.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs), contentPadding = PaddingValues(6.dp)) {
            items(all, key = { it.first }) { (id, title) ->
                val ready = id in installed
                val pending = items.firstOrNull { it.id.endsWith(":$id") && it.status != DownloadStatus.COMPLETED }
                ChoiceRow(
                    title = title,
                    selected = settings.translationId == id,
                    badge = when {
                        settings.translationId == id -> stringResource(R.string.dl_in_use)
                        ready -> stringResource(R.string.dl_translation_ready)
                        pending != null -> stringResource(R.string.dl_status_queued)
                        else -> null
                    },
                    trailing = if (!ready && pending == null) stringResource(R.string.dl_start) else null,
                    onClick = {
                        when {
                            ready -> viewModel.useTranslation(id)
                            pending == null -> viewModel.extras.firstOrNull { it.id == id }?.let(viewModel::downloadExtra)
                        }
                    },
                    modifier = Modifier.restorableFocus(focus, id),
                )
            }
        }
    }
    RestoreFocusEffect(focus, "en.pickthall")
}

