package com.mfouad.muslimtv.feature.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfouad.muslimtv.core.common.IoDispatcher
import com.mfouad.muslimtv.core.data.audio.LocalAudioStore
import com.mfouad.muslimtv.core.data.audio.Reciters
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.data.quran.QuranSearchResult
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.model.Bookmark
import com.mfouad.muslimtv.core.model.Reciter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReciterRow(val reciter: Reciter, val downloadedSurahs: Int)

@HiltViewModel
class QuranHomeViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    val quran: QuranRepository,
    private val settings: SettingsRepository,
    private val local: LocalAudioStore,
    @IoDispatcher private val io: CoroutineDispatcher,
    network: com.mfouad.muslimtv.core.common.NetworkMonitor,
) : ViewModel() {
    val tab: StateFlow<Int> = savedState.getStateFlow(KEY_TAB, 0)

    val reciterId: StateFlow<String> = settings.settings.map { it.reciterId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val bookmarks: StateFlow<List<Bookmark>> = quran.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _reciters = MutableStateFlow(Reciters.all.map { ReciterRow(it, 0) })
    val reciters: StateFlow<List<ReciterRow>> = _reciters.asStateFlow()

    private val _query = MutableStateFlow(savedState[KEY_QUERY] ?: "")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<QuranSearchResult>> = _query.map { quran.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshDownloads()
    }

    fun refreshDownloads() = viewModelScope.launch {
        val rows = withContext(io) {
            Reciters.all.map { r -> ReciterRow(r, quran.surahs.count { local.isSurahDownloaded(r.id, it.number, it.ayahCount) }) }
        }
        _reciters.value = rows
    }

    /** Surahs downloaded for the current reciter, for the badge on surah cards. */
    private val _downloadedSurahs = MutableStateFlow<Set<Int>>(emptySet())
    val downloadedSurahs: StateFlow<Set<Int>> = _downloadedSurahs.asStateFlow()

    val online: StateFlow<Boolean> = network.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        viewModelScope.launch {
            reciterId.collect { id ->
                if (id.isEmpty()) return@collect
                _downloadedSurahs.value = withContext(io) { quran.surahs.filter { local.isSurahDownloaded(id, it.number, it.ayahCount) }.map { it.number }.toSet() }
            }
        }
    }

    fun selectTab(i: Int) { savedState[KEY_TAB] = i }

    fun setQuery(q: String) {
        savedState[KEY_QUERY] = q
        _query.value = q
    }

    fun chooseReciter(r: Reciter) = viewModelScope.launch { settings.setReciter(r.id) }

    private companion object {
        const val KEY_TAB = "tab"
        const val KEY_QUERY = "query"
    }
}
