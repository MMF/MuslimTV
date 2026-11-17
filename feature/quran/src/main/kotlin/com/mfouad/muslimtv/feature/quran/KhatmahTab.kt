package com.mfouad.muslimtv.feature.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.data.family.FamilyRepository
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.designsystem.component.ButtonStyle
import com.mfouad.muslimtv.core.designsystem.component.IconButton
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.focus.FocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.KhatmahPlan
import com.mfouad.muslimtv.core.model.KhatmahPortion
import com.mfouad.muslimtv.core.model.Surah
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class KhatmahViewModel @Inject constructor(
    private val family: FamilyRepository,
    private val quran: QuranRepository,
) : ViewModel() {
    val plan: StateFlow<KhatmahPlan?> = family.khatmah.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun today(plan: KhatmahPlan): KhatmahPortion = family.portion(plan, plan.completed + 1)
    fun surah(n: Int): Surah = quran.surah(n)
    fun start(days: Int) = viewModelScope.launch { family.startKhatmah(days, LocalDate.now()) }
    fun done() = viewModelScope.launch { family.markPortionDone() }
    fun stop() = viewModelScope.launch { family.stopKhatmah() }
}

/** "Range" text such as "Al-Baqara 142 → Al-Baqara 202". */
@Composable
internal fun portionText(p: KhatmahPortion, surah: (Int) -> Surah): String {
    val f = rememberTimeFormatter()
    val start = "${surah(p.start.surah).displayName()} ${f.number(p.start.ayah)}"
    val end = p.endExclusive?.let { "${surah(it.surah).displayName()} ${f.number(it.ayah)}" } ?: surah(114).displayName()
    return "$start → $end"
}

@Composable
fun KhatmahTab(focus: FocusRestoreState, onRead: (AyahRef) -> Unit, viewModel: KhatmahViewModel = hiltViewModel()) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val f = rememberTimeFormatter()
    val c = MtvTheme.colors
    val p = plan
    Column(Modifier.widthIn(max = 1100.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        if (p == null || p.finished) {
            var days by rememberSaveable { mutableIntStateOf(30) }
            if (p?.finished == true) Text(stringResource(R.string.kh_finished), style = MtvTheme.type.title, color = c.gold)
            Text(stringResource(R.string.kh_intro), style = MtvTheme.type.body, color = c.onBackgroundMuted)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                Text(stringResource(R.string.kh_days), style = MtvTheme.type.bodyStrong)
                IconButton(Icons.Rounded.Remove, stringResource(R.string.kh_days), { days = (days - if (days > 30) 10 else 1).coerceAtLeast(3) }, Modifier.restorableFocus(focus, "kh-"))
                Text(stringResource(R.string.kh_days_value, f.number(days)), style = MtvTheme.type.title, color = c.goldSoft, modifier = Modifier.widthIn(min = 160.dp))
                IconButton(Icons.Rounded.Add, stringResource(R.string.kh_days), { days = (days + if (days >= 30) 10 else 1).coerceAtMost(365) }, Modifier.restorableFocus(focus, "kh+"))
                MtvButton(stringResource(R.string.kh_start), { viewModel.start(days) }, Modifier.restorableFocus(focus, "kh_start"))
            }
        } else {
            var confirmStop by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
            val portion = viewModel.today(p)
            Text(stringResource(R.string.kh_day, f.number(p.completed + 1), f.number(p.days)), style = MtvTheme.type.headline)
            Text(portionText(portion) { viewModel.surah(it) }, style = MtvTheme.type.title, color = c.goldSoft)
            Text(stringResource(R.string.kh_pages, f.number(portion.pages.first), f.number(portion.pages.last)), style = MtvTheme.type.body, color = c.onBackgroundMuted)
            Box(Modifier.fillMaxWidth().height(12.dp).background(c.surfaceFocused, MtvShapes.pill)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(p.progress).background(c.gold, MtvShapes.pill))
            }
            val behind = p.behind(LocalDate.now().toEpochDay())
            if (behind > 1) Text(stringResource(R.string.kh_behind, f.number(behind)), style = MtvTheme.type.body, color = c.goldSoft)
            Row(horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
                MtvButton(stringResource(R.string.kh_read), { onRead(portion.start) }, Modifier.restorableFocus(focus, "kh_read"), icon = Icons.AutoMirrored.Rounded.MenuBook)
                MtvButton(stringResource(R.string.kh_done), viewModel::done, Modifier.restorableFocus(focus, "kh_done"), style = ButtonStyle.Secondary, icon = Icons.Rounded.Check)
                MtvButton(stringResource(R.string.kh_stop), { confirmStop = true }, Modifier.restorableFocus(focus, "kh_stop"), style = ButtonStyle.Secondary)
            }
            if (confirmStop) {
                com.mfouad.muslimtv.core.designsystem.component.ConfirmDialog(
                    title = stringResource(R.string.kh_stop),
                    message = stringResource(R.string.kh_stop_confirm),
                    confirmLabel = stringResource(R.string.kh_stop),
                    dismissLabel = stringResource(R.string.kh_keep),
                    onConfirm = { confirmStop = false; viewModel.stop() },
                    onDismiss = { confirmStop = false },
                )
            }
        }
    }
}
