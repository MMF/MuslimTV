package com.mfouad.muslimtv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mfouad.muslimtv.core.designsystem.component.ChoiceRow
import com.mfouad.muslimtv.core.designsystem.component.ConfirmDialog
import com.mfouad.muslimtv.core.designsystem.component.DialogPanel
import com.mfouad.muslimtv.core.designsystem.component.IconButton
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.ScreenHeader
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.Profile
import com.mfouad.muslimtv.core.ui.SearchField
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProfilesViewModel @Inject constructor(private val family: FamilyRepository) : ViewModel() {
    val profiles: StateFlow<List<Profile>> = family.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val active: StateFlow<Long> = family.activeId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1L)
    fun switchTo(id: Long) = viewModelScope.launch { family.switchTo(id) }
    fun add(name: String) = viewModelScope.launch { if (name.isNotBlank()) family.switchTo(family.add(name)) }
    fun delete(id: Long) = viewModelScope.launch { family.delete(id) }
}

/** Each family member keeps their own bookmarks, khatmah and memorization progress. */
@Composable
fun ProfilesEntry(viewModel: ProfilesViewModel = hiltViewModel()) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val focus = rememberFocusRestoreState()
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.profiles_title), subtitle = stringResource(R.string.profiles_hint))
        MtvButton(stringResource(R.string.profiles_add), { adding = true }, Modifier.restorableFocus(focus, "add"), icon = Icons.Rounded.PersonAdd)
        LazyColumn(Modifier.widthIn(max = 900.dp), verticalArrangement = Arrangement.spacedBy(MtvSpacing.xs), contentPadding = PaddingValues(6.dp)) {
            items(profiles, key = { it.id }) { p ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
                    ChoiceRow(
                        title = if (p.id == 1L) stringResource(R.string.profile_family) else p.name,
                        selected = p.id == active,
                        onClick = { viewModel.switchTo(p.id) },
                        modifier = Modifier.weight(1f).restorableFocus(focus, "p${p.id}"),
                    )
                    if (p.id != 1L) IconButton(Icons.Rounded.Delete, stringResource(R.string.profiles_delete), { deleting = p.id }, Modifier.restorableFocus(focus, "d${p.id}"))
                }
            }
        }
    }
    RestoreFocusEffect(focus, "p$active")
    if (adding) {
        var name by rememberSaveable { mutableStateOf("") }
        DialogPanel({ adding = false }) {
            Text(stringResource(R.string.profiles_add), style = MtvTheme.type.title)
            SearchField(name, { name = it.take(20) }, stringResource(R.string.profiles_name_hint), onDone = {})
            MtvButton(stringResource(R.string.profiles_save), { viewModel.add(name); adding = false })
        }
    }
    deleting?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.profiles_delete),
            message = stringResource(R.string.profiles_delete_confirm, profiles.firstOrNull { it.id == id }?.name.orEmpty()),
            confirmLabel = stringResource(R.string.profiles_delete),
            dismissLabel = stringResource(R.string.profiles_keep),
            onConfirm = { viewModel.delete(id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}
