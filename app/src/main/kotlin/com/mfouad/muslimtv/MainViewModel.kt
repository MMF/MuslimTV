package com.mfouad.muslimtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfouad.muslimtv.core.common.NetworkMonitor
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    network: NetworkMonitor,
    private val player: com.mfouad.muslimtv.core.media.QuranPlayer,
) : ViewModel() {
    /** Drives the rail's pause/resume entry while a recitation is loaded anywhere in the app. */
    val playback: StateFlow<com.mfouad.muslimtv.core.media.PlayerState> = player.state

    fun togglePlayback() = player.togglePlayPause()

    val online: StateFlow<Boolean> = network.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val lastActivity = MutableStateFlow(System.currentTimeMillis())

    /** Called for every key press. */
    fun onUserActivity() { lastActivity.value = System.currentTimeMillis() }

    /**
     * Emits when the remote has been idle for the configured time (0 = never), so the shell can
     * open the ambient screen.
     */
    val idle: StateFlow<Boolean> = combine(settings.settings.map { it.ambientTimeoutMin }, lastActivity) { minutes, last -> minutes to last }
        .flatMapLatest { (minutes, last) ->
            flow {
                emit(false)
                if (minutes > 0) {
                    delay((last + minutes * 60_000L - System.currentTimeMillis()).coerceAtLeast(0))
                    emit(true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Null until settings are read (a few ms), then whether first-launch setup is done. */
    val onboardingDone: StateFlow<Boolean?> = flow { emit(settings.settings.first().onboardingDone) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
