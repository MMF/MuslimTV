package com.mfouad.muslimtv.feature.azkar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.toRoute
import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.audio.Reciters
import com.mfouad.muslimtv.core.data.azkar.AzkarRepository
import com.mfouad.muslimtv.core.data.azkar.AzkarSuggester
import com.mfouad.muslimtv.core.data.prayer.PrayerScheduleRepository
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.media.QuranPlayer
import com.mfouad.muslimtv.core.model.AzkarCategory
import com.mfouad.muslimtv.core.model.AzkarChapter
import com.mfouad.muslimtv.core.model.DivineName
import com.mfouad.muslimtv.core.model.Surah
import com.mfouad.muslimtv.core.model.Tasbeeh
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AzkarHomeViewModel @Inject constructor(
    private val repo: AzkarRepository,
    schedules: PrayerScheduleRepository,
    time: TimeProvider,
) : ViewModel() {
    val suggested: StateFlow<AzkarCategory?> = schedules.schedule
        .map { AzkarSuggester.suggest(time.nowMillis(), it.today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _counts = MutableStateFlow<Map<AzkarCategory, Int>>(emptyMap())
    val counts: StateFlow<Map<AzkarCategory, Int>> = _counts.asStateFlow()

    private val _occasions = MutableStateFlow<List<AzkarChapter>>(emptyList())
    val occasions: StateFlow<List<AzkarChapter>> = _occasions.asStateFlow()

    private val _names = MutableStateFlow<List<DivineName>>(emptyList())
    val names: StateFlow<List<DivineName>> = _names.asStateFlow()

    init {
        viewModelScope.launch {
            _counts.value = AzkarCategory.entries.mapNotNull { c -> repo.forCategory(c)?.let { c to it.items.size } }.toMap()
            _occasions.value = repo.occasions()
            _names.value = repo.names()
        }
    }
}

/** Walks through one chapter, one card at a time. Counting state survives Back and rotation. */
@HiltViewModel
class ZikrViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val repo: AzkarRepository,
) : ViewModel() {
    private val route = savedState.toRoute<ZikrRoute>()

    private val _chapter = MutableStateFlow<AzkarChapter?>(null)
    val chapter: StateFlow<AzkarChapter?> = _chapter.asStateFlow()

    val index: StateFlow<Int> = savedState.getStateFlow(KEY_INDEX, 0)
    val counted: StateFlow<Int> = savedState.getStateFlow(KEY_COUNT, 0)
    val finished: StateFlow<Boolean> = savedState.getStateFlow(KEY_DONE, false)

    init {
        viewModelScope.launch {
            _chapter.value = route.category?.let { c -> repo.forCategory(AzkarCategory.valueOf(c)) } ?: repo.chapter(route.chapterId)
        }
    }

    /** One repetition. Returns true when this zikr is now complete (the screen then advances). */
    fun count(): Boolean {
        val zikr = _chapter.value?.items?.getOrNull(index.value) ?: return false
        val next = (counted.value + 1).coerceAtMost(zikr.count)
        savedState[KEY_COUNT] = next
        return next >= zikr.count
    }

    fun go(delta: Int) {
        val size = _chapter.value?.items?.size ?: return
        val target = index.value + delta
        when {
            target >= size -> savedState[KEY_DONE] = true
            target >= 0 -> { savedState[KEY_INDEX] = target; savedState[KEY_COUNT] = 0 }
        }
    }

    fun restart() {
        savedState[KEY_INDEX] = 0
        savedState[KEY_COUNT] = 0
        savedState[KEY_DONE] = false
    }

    private companion object {
        const val KEY_INDEX = "index"
        const val KEY_COUNT = "count"
        const val KEY_DONE = "done"
    }
}

@HiltViewModel
class TasbeehViewModel @Inject constructor(private val savedState: SavedStateHandle) : ViewModel() {
    private val _t = MutableStateFlow(Tasbeeh(count = savedState[KEY_COUNT] ?: 0, target = savedState[KEY_TARGET] ?: 33).let { it.copy(rounds = if (it.target > 0) it.count / it.target else 0) })
    val tasbeeh: StateFlow<Tasbeeh> = _t.asStateFlow()
    val phrase: StateFlow<Int> = savedState.getStateFlow(KEY_PHRASE, 0)

    fun tap() = update { it.tap() }
    fun setTarget(t: Int) = update { it.withTarget(t) }
    fun reset() = update { it.reset() }
    fun setPhrase(i: Int) { savedState[KEY_PHRASE] = i; reset() }

    private fun update(f: (Tasbeeh) -> Tasbeeh) {
        _t.update(f)
        savedState[KEY_COUNT] = _t.value.count
        savedState[KEY_TARGET] = _t.value.target
    }

    private companion object {
        const val KEY_COUNT = "count"
        const val KEY_TARGET = "target"
        const val KEY_PHRASE = "phrase"
    }
}

@OptIn(UnstableApi::class)
@HiltViewModel
class RuqyahViewModel @Inject constructor(
    private val player: QuranPlayer,
    private val settings: SettingsRepository,
    private val quran: QuranRepository,
) : ViewModel() {
    val passages = AzkarRepository.RUQYAH
    fun surah(n: Int): Surah = quran.surah(n)

    private val _offline = MutableStateFlow(false)
    /** True when offline and the ruqyah ayahs are not downloaded for the chosen reciter. */
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    fun playAll(onStarted: () -> Unit) = viewModelScope.launch {
        val reciter = Reciters.byId(settings.settings.first().reciterId)
        val problem = player.playRanges(reciter, passages)
        player.dismissProblem()
        _offline.value = problem != null
        if (problem == null) onStarted()
    }
}
