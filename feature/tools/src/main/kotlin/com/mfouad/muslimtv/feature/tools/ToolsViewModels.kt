package com.mfouad.muslimtv.feature.tools

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import com.mfouad.muslimtv.core.common.NetworkMonitor
import com.mfouad.muslimtv.core.data.azkar.AzkarRepository
import com.mfouad.muslimtv.core.data.prayer.PrayerScheduleRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.data.tools.FastingNote
import com.mfouad.muslimtv.core.data.tools.Hadith
import com.mfouad.muslimtv.core.data.tools.HadithRepository
import com.mfouad.muslimtv.core.data.tools.IslamicCalendar
import com.mfouad.muslimtv.core.data.tools.Occasion
import com.mfouad.muslimtv.core.data.tools.QiblaCalculator
import com.mfouad.muslimtv.core.data.tools.QiblaInfo
import com.mfouad.muslimtv.core.data.tools.RemoteConfig
import com.mfouad.muslimtv.core.data.tools.RemoteConfigRepository
import com.mfouad.muslimtv.core.data.tools.Stream
import com.mfouad.muslimtv.core.data.tools.WorldMap
import com.mfouad.muslimtv.core.media.QuranPlayer
import com.mfouad.muslimtv.core.model.AzkarChapter
import com.mfouad.muslimtv.core.model.City
import com.mfouad.muslimtv.core.model.HijriDate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarDay(val gregorian: LocalDate, val hijri: HijriDate, val occasions: List<Occasion>, val fasting: FastingNote?, val isToday: Boolean)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val schedules: PrayerScheduleRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val offset: StateFlow<Int> = savedState.getStateFlow(KEY, 0)
    private val _days = MutableStateFlow<List<CalendarDay>>(emptyList())
    val days: StateFlow<List<CalendarDay>> = _days.asStateFlow()

    init { load() }

    fun shift(delta: Int) { savedState[KEY] = (offset.value + delta).coerceIn(-12, 12); load() }

    /** Builds one Hijri month by walking Gregorian days from today (Umm al-Qura plus the user's offset). */
    private fun load() = viewModelScope.launch {
        val s = settings.settings.first()
        val zone = s.city?.timeZone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun hijri(d: LocalDate) = schedules.hijriFor(d, s.hijriOffset)
        // Find the first day of the target Hijri month.
        var cursor = today.minusDays(hijri(today).day - 1L)
        repeat(kotlin.math.abs(offset.value)) {
            cursor = if (offset.value > 0) cursor.plusDays(31).let { it.minusDays(hijri(it).day - 1L) }
            else cursor.minusDays(1).let { it.minusDays(hijri(it).day - 1L) }
        }
        val month = hijri(cursor).month
        _days.value = generateSequence(cursor) { it.plusDays(1) }.takeWhile { hijri(it).month == month }.map { d ->
            val h = hijri(d)
            CalendarDay(d, h, IslamicCalendar.occasions(h), IslamicCalendar.fasting(h, d), d == today)
        }.toList()
    }

    private companion object { const val KEY = "month" }
}

@HiltViewModel
class QiblaViewModel @Inject constructor(settings: SettingsRepository, private val map: WorldMap) : ViewModel() {
    val city: StateFlow<City?> = settings.settings.let { flow ->
        val state = MutableStateFlow<City?>(null)
        viewModelScope.launch { flow.collect { state.value = it.city } }
        state
    }
    val qibla: QiblaInfo? get() = city.value?.let(QiblaCalculator::forCity)
    private val _rings = MutableStateFlow<List<FloatArray>>(emptyList())
    val rings: StateFlow<List<FloatArray>> = _rings.asStateFlow()
    init { viewModelScope.launch { _rings.value = map.rings() } }
}

@HiltViewModel
class HadithViewModel @Inject constructor(private val repo: HadithRepository, private val savedState: SavedStateHandle) : ViewModel() {
    private val _today = MutableStateFlow<Hadith?>(null)
    val today: StateFlow<Hadith?> = _today.asStateFlow()
    private val _nawawi = MutableStateFlow<List<Hadith>>(emptyList())
    val nawawi: StateFlow<List<Hadith>> = _nawawi.asStateFlow()
    val index: StateFlow<Int> = savedState.getStateFlow(KEY, 0)

    init {
        viewModelScope.launch {
            _today.value = repo.ofTheDay(LocalDate.now())
            _nawawi.value = repo.nawawi()
        }
    }

    fun go(delta: Int) { savedState[KEY] = (index.value + delta).coerceIn(0, (_nawawi.value.size - 1).coerceAtLeast(0)) }

    private companion object { const val KEY = "index" }
}

@HiltViewModel
class LiveListViewModel @Inject constructor(private val repo: RemoteConfigRepository, network: NetworkMonitor) : ViewModel() {
    private val _config = MutableStateFlow(RemoteConfig(emptyList(), emptyList()))
    val config: StateFlow<RemoteConfig> = _config.asStateFlow()
    val online: StateFlow<Boolean> = network.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    init {
        viewModelScope.launch {
            _config.value = repo.cached()
            _config.value = repo.refresh()
        }
    }
}

/** Plays one live stream or radio station; its own player, separate from Quran listening. */
@UnstableApi
@HiltViewModel
class LivePlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedState: SavedStateHandle,
    repo: RemoteConfigRepository,
    quranPlayer: QuranPlayer,
) : ViewModel() {
    private val id = savedState.toRoute<LivePlayerRoute>().id
    private val _stream = MutableStateFlow<Stream?>(null)
    val stream: StateFlow<Stream?> = _stream.asStateFlow()
    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { _error.value = true }
        })
    }

    init {
        quranPlayer.pause()
        viewModelScope.launch {
            val c = repo.cached()
            _stream.value = (c.live + c.radio).firstOrNull { it.id == id }
            play()
        }
    }

    fun play() {
        val s = _stream.value ?: return
        _error.value = false
        player.setMediaItem(MediaItem.fromUri(s.url))
        player.prepare()
        player.play()
    }

    override fun onCleared() = player.release()
}

@HiltViewModel
class DuaListViewModel @Inject constructor(private val azkar: AzkarRepository) : ViewModel() {
    private val _chapters = MutableStateFlow<List<AzkarChapter>>(emptyList())
    val chapters: StateFlow<List<AzkarChapter>> = _chapters.asStateFlow()

    /** Loads Hisn al-Muslim chapters by title (letters only), keeping the given order. */
    fun load(titles: List<String>) = viewModelScope.launch {
        val all = azkar.chapters()
        _chapters.value = titles.mapNotNull { t -> all.firstOrNull { AzkarRepository.letters(it.title) == t } }
    }
}
