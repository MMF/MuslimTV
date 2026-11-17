package com.mfouad.muslimtv.feature.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.mfouad.muslimtv.core.data.audio.AyahTimingSource
import com.mfouad.muslimtv.core.data.audio.WordSync
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.media.PlayerState
import com.mfouad.muslimtv.core.media.QuranPlayer
import com.mfouad.muslimtv.core.media.SleepTimer
import com.mfouad.muslimtv.core.model.QuranText
import com.mfouad.muslimtv.core.model.ReaderAyah
import com.mfouad.muslimtv.core.model.RepeatMode
import com.mfouad.muslimtv.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NowReciting(val ayah: ReaderAyah? = null, val isBasmala: Boolean = false, val word: Int? = null)

@OptIn(UnstableApi::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ListeningViewModel @Inject constructor(
    val player: QuranPlayer,
    private val quran: QuranRepository,
    private val timings: AyahTimingSource,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<PlayerState> = player.state

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val ayah = combine(player.state.map { it.current to it.isBasmala }.distinctUntilChanged(), settingsRepository.settings.map { if (it.showTranslation) it.translationId else null }.distinctUntilChanged()) { a, t -> a to t }
        .mapLatest { (cur, withTranslation) ->
            val (ref, basmala) = cur
            if (ref == null) null to null else {
                val ayahs = quran.readerAyahs(ref.surah, withTranslation)
                val a = ayahs.firstOrNull { it.ayah == ref.ayah }?.let { it.copy(text = QuranText.displayText(it.surah, it.ayah, it.text)) }
                val segs = if (!basmala && player.state.value.reciter.hasWordTimings) timings.segments(player.state.value.reciter.id, ref) else null
                a to segs
            }
        }

    val now: StateFlow<NowReciting> = combine(ayah, player.positionMs, player.state) { (a, segs), pos, s ->
        NowReciting(a, s.isBasmala, segs?.let { WordSync.wordAt(pos, it) })
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowReciting())

    fun surahName(n: Int) = quran.surah(n)

    fun cycleSpeed() {
        val next = SPEEDS[(SPEEDS.indexOf(player.state.value.speed).coerceAtLeast(0) + 1) % SPEEDS.size]
        player.setSpeed(next)
        viewModelScope.launch { settingsRepository.setPlaybackSpeed(next) }
    }

    fun cycleRepeat() = player.setRepeat(RepeatMode.entries[(player.state.value.repeat.ordinal + 1) % RepeatMode.entries.size])

    fun cycleSleep() = player.setSleepTimer(SleepTimer.entries[(player.state.value.sleep.ordinal + 1) % SleepTimer.entries.size])

    companion object {
        val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f)
    }
}
