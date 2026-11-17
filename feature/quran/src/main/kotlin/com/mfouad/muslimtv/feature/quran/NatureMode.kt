package com.mfouad.muslimtv.feature.quran

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Water
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.designsystem.component.ButtonStyle
import com.mfouad.muslimtv.core.designsystem.component.MtvButton
import com.mfouad.muslimtv.core.designsystem.component.NatureBackground
import com.mfouad.muslimtv.core.designsystem.component.Scenes
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.media.AmbientSoundPlayer
import com.mfouad.muslimtv.core.model.AmbientSound
import com.mfouad.muslimtv.core.model.AmbientVolume
import com.mfouad.muslimtv.core.model.UserSettings
import com.mfouad.muslimtv.core.ui.rememberTimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@HiltViewModel
class NatureViewModel @Inject constructor(
    private val settings: SettingsRepository,
    val ambient: AmbientSoundPlayer,
) : ViewModel() {
    val state: StateFlow<UserSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun cycleScene() = viewModelScope.launch {
        val s = state.value ?: return@launch
        val next = if (s.natureScene + 1 >= Scenes.all.size) Scenes.ROTATE else s.natureScene + 1
        settings.setNatureScene(next)
    }

    /** Off → rain → stream → birds → off, and for sounds also low ↔ medium volume. */
    fun cycleSound() = viewModelScope.launch {
        val s = state.value ?: return@launch
        settings.setAmbientSound(AmbientSound.entries[(s.ambientSound.ordinal + 1) % AmbientSound.entries.size])
    }

    fun cycleVolume() = viewModelScope.launch {
        val s = state.value ?: return@launch
        settings.setAmbientVolume(if (s.ambientVolume == AmbientVolume.LOW) AmbientVolume.MEDIUM else AmbientVolume.LOW)
    }
}

/** Full-screen listening over calm nature scenes, with an optional quiet ambient sound. */
@OptIn(UnstableApi::class)
@Composable
fun NatureEntry(onChooseSurah: () -> Unit, viewModel: NatureViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val settings = s ?: return
    val f = rememberTimeFormatter()
    LaunchedEffect(settings.ambientSound, settings.ambientVolume) { viewModel.ambient.play(settings.ambientSound, settings.ambientVolume) }
    DisposableEffect(Unit) { onDispose { viewModel.ambient.stop() } }
    ListeningEntry(
        onChooseSurah = onChooseSurah,
        background = { NatureBackground(settings.natureScene) },
        extraControls = { focus ->
            MtvButton(
                if (settings.natureScene == Scenes.ROTATE) stringResource(R.string.nature_rotate) else stringResource(R.string.nature_scene, f.number(settings.natureScene + 1)),
                viewModel::cycleScene, Modifier.restorableFocus(focus, "scene"), style = ButtonStyle.Secondary, icon = Icons.Rounded.Landscape,
            )
            MtvButton(
                stringResource(
                    when (settings.ambientSound) {
                        AmbientSound.OFF -> R.string.sound_off
                        AmbientSound.RAIN -> R.string.sound_rain
                        AmbientSound.STREAM -> R.string.sound_stream
                        AmbientSound.BIRDS -> R.string.sound_birds
                    },
                ),
                viewModel::cycleSound, Modifier.restorableFocus(focus, "sound"), style = ButtonStyle.Secondary, icon = Icons.Rounded.Water,
            )
            if (settings.ambientSound != AmbientSound.OFF) {
                MtvButton(
                    stringResource(if (settings.ambientVolume == AmbientVolume.LOW) R.string.sound_low else R.string.sound_medium),
                    viewModel::cycleVolume, Modifier.restorableFocus(focus, "vol"), style = ButtonStyle.Secondary,
                )
            }
        },
    )
}
