package com.mfouad.muslimtv.feature.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.toRoute
import com.mfouad.muslimtv.core.data.audio.AyahTimingSource
import com.mfouad.muslimtv.core.data.audio.Reciters
import com.mfouad.muslimtv.core.data.audio.WordSync
import com.mfouad.muslimtv.core.data.quran.PositionKind
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.media.PlaybackProblem
import com.mfouad.muslimtv.core.media.PlayerState
import com.mfouad.muslimtv.core.media.QuranPlayer
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.QuranText
import com.mfouad.muslimtv.core.model.ReaderAyah
import com.mfouad.muslimtv.core.model.Reciter
import com.mfouad.muslimtv.core.model.Surah
import com.mfouad.muslimtv.core.model.UserSettings
import com.mfouad.muslimtv.core.model.WordSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReaderUiState(
    val surah: Surah? = null,
    val ayahs: List<ReaderAyah> = emptyList(),
    val startAyah: Int = 1,
    val settings: UserSettings = UserSettings(),
    val bookmarked: Set<Int> = emptySet(),
    val loaded: Boolean = false,
)

/** What the reciter is saying right now in this surah: the ayah, and the word when timing exists. */
data class FollowAlong(val ayah: Int? = null, val isBasmala: Boolean = false, val word: Int? = null)

@OptIn(UnstableApi::class, ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val quran: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val timings: AyahTimingSource,
    val player: QuranPlayer,
) : ViewModel() {
    private val route = savedState.toRoute<ReaderRoute>()
    val surahNumber = route.surah

    private val ayahs = MutableStateFlow<List<ReaderAyah>>(emptyList())
    private val loaded = MutableStateFlow(false)

    val state: StateFlow<ReaderUiState> = combine(
        ayahs, loaded, settingsRepository.settings, quran.bookmarks,
    ) { list, isLoaded, settings, bookmarks ->
        ReaderUiState(
            surah = quran.surah(surahNumber),
            ayahs = list,
            startAyah = route.ayah,
            settings = settings,
            bookmarked = bookmarks.filter { it.ref.surah == surahNumber }.map { it.ref.ayah }.toSet(),
            loaded = isLoaded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState(surah = quran.surah(surahNumber), startAyah = route.ayah))

    val playerState: StateFlow<PlayerState> = player.state

    private val segments: StateFlow<List<WordSegment>?> = player.state
        .map { s -> if (s.surah == surahNumber && !s.isBasmala) s.current?.let { s.reciter to it } else null }
        .distinctUntilChanged()
        .mapLatest { pair -> pair?.let { (reciter, ref) -> if (reciter.hasWordTimings) timings.segments(reciter.id, ref) else null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Driven by the player's real item and position, so it stays right after pause, seek and skip. */
    val followAlong: StateFlow<FollowAlong> = combine(player.state, player.positionMs, segments) { s, pos, segs ->
        if (s.surah != surahNumber || s.current == null) FollowAlong()
        else FollowAlong(s.current?.ayah, s.isBasmala, segs?.let { WordSync.wordAt(pos, it) })
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FollowAlong())

    private val focusedAyah = MutableStateFlow<Int?>(null)

    private val _tafsir = MutableStateFlow<Pair<AyahRef, String?>?>(null)
    val tafsir: StateFlow<Pair<AyahRef, String?>?> = _tafsir.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.map { if (it.showTranslation) it.translationId else null }.distinctUntilChanged().collect { translation ->
                ayahs.value = quran.readerAyahs(surahNumber, translation).map {
                    it.copy(text = QuranText.displayText(it.surah, it.ayah, it.text))
                }
                loaded.value = true
            }
        }
        // Remember where the family stopped reading, without writing on every key press.
        viewModelScope.launch {
            focusedAyah.filterNotNull().debounce(POSITION_DEBOUNCE_MS).collect {
                quran.savePosition(PositionKind.READ, AyahRef(surahNumber, it))
            }
        }
    }

    fun onAyahFocused(ayah: Int) { focusedAyah.value = ayah }

    fun playFrom(ayah: Int, reciter: Reciter? = null) = viewModelScope.launch {
        val r = reciter ?: Reciters.byId(settingsRepository.settings.first().reciterId)
        player.play(r, surahNumber, ayah)
    }

    fun retry() = viewModelScope.launch {
        val s = player.state.value
        player.dismissProblem()
        playFrom(s.current?.ayah ?: 1, s.reciter)
    }

    fun setBookmark(ayah: Int, on: Boolean) = viewModelScope.launch { quran.setBookmark(AyahRef(surahNumber, ayah), on) }

    fun toggleTranslation() = viewModelScope.launch {
        settingsRepository.setShowTranslation(!settingsRepository.settings.first().showTranslation)
    }

    fun showTafsir(ayah: Int) = viewModelScope.launch {
        val ref = AyahRef(surahNumber, ayah)
        _tafsir.value = ref to quran.tafsir(ref)
    }

    fun closeTafsir() { _tafsir.value = null }

    fun dismissProblem() = player.dismissProblem()

    val problem: StateFlow<PlaybackProblem?> = player.state.map { it.problem }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private companion object {
        const val POSITION_DEBOUNCE_MS = 1_000L
    }
}
