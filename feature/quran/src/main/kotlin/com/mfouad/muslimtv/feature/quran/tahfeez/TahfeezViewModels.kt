package com.mfouad.muslimtv.feature.quran.tahfeez

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.navigation.toRoute
import com.mfouad.muslimtv.core.common.NetworkMonitor
import com.mfouad.muslimtv.core.data.audio.AyahAudioResolver
import com.mfouad.muslimtv.core.data.audio.LocalAudioStore
import com.mfouad.muslimtv.core.data.audio.Reciters
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.data.tahfeez.TahfeezRepository
import com.mfouad.muslimtv.core.data.tahfeez.TahfeezSession
import com.mfouad.muslimtv.core.media.QuranPlayer
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.PauseMode
import com.mfouad.muslimtv.core.model.QuranText
import com.mfouad.muslimtv.core.model.Surah
import com.mfouad.muslimtv.core.model.TahfeezConfig
import com.mfouad.muslimtv.core.model.TahfeezEngine
import com.mfouad.muslimtv.core.model.TahfeezState
import com.mfouad.muslimtv.feature.quran.TahfeezSessionRoute
import com.mfouad.muslimtv.feature.quran.TahfeezSetupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class TahfeezListViewModel @Inject constructor(
    private val repo: TahfeezRepository,
    private val quran: QuranRepository,
) : ViewModel() {
    val sessions: StateFlow<List<TahfeezSession>> = repo.sessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun surah(n: Int): Surah = quran.surah(n)
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }

    /** Juz Amma: surahs 78–114. */
    val juzAmma: List<Surah> get() = quran.surahs.filter { it.number >= JUZ_AMMA_START }

    /** Creates a kids' session and returns its id. */
    suspend fun startKids(surah: Int): Long {
        val s = quran.surah(surah)
        val config = TahfeezConfig.kids(surah, s.ayahCount, Reciters.kids.id)
        return repo.save(null, config, TahfeezState(ayah = 1))
    }

    private companion object { const val JUZ_AMMA_START = 78 }
}

@HiltViewModel
class TahfeezSetupViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val quran: QuranRepository,
    private val settings: SettingsRepository,
    private val repo: TahfeezRepository,
) : ViewModel() {
    private val route = savedState.toRoute<TahfeezSetupRoute>()
    val surah: Surah = quran.surah(route.surah)
    private val _config = MutableStateFlow(
        TahfeezConfig(surah.number, route.ayah, (route.ayah + 4).coerceAtMost(surah.ayahCount), Reciters.all.first().id),
    )
    val config: StateFlow<TahfeezConfig> = _config.asStateFlow()

    init {
        viewModelScope.launch { _config.update { it.copy(reciterId = settings.settings.first().reciterId) } }
    }

    fun from(delta: Int) = _config.update { c -> val f = (c.fromAyah + delta).coerceIn(1, surah.ayahCount); c.copy(fromAyah = f, toAyah = c.toAyah.coerceAtLeast(f)) }
    fun to(delta: Int) = _config.update { c -> c.copy(toAyah = (c.toAyah + delta).coerceIn(c.fromAyah, surah.ayahCount)) }
    fun repeats(delta: Int) = _config.update { it.copy(repeatsPerAyah = (it.repeatsPerAyah + delta).coerceIn(0, TahfeezConfig.MAX_REPEATS)) }
    fun rounds(delta: Int) = _config.update { it.copy(rangeRepeats = (it.rangeRepeats + delta).coerceIn(1, TahfeezConfig.MAX_ROUNDS)) }
    fun cyclePause() = _config.update { c -> c.copy(pause = PAUSES[(PAUSES.indexOf(c.pause).coerceAtLeast(0) + 1) % PAUSES.size]) }
    fun toggleHide() = _config.update { it.copy(hideText = !it.hideText) }
    fun cycleReciter() = _config.update { c ->
        val i = Reciters.all.indexOfFirst { it.id == c.reciterId }
        c.copy(reciterId = Reciters.all[(i + 1) % Reciters.all.size].id)
    }

    suspend fun start(): Long = repo.save(null, _config.value, TahfeezState(ayah = _config.value.fromAyah))

    companion object {
        val PAUSES: List<PauseMode> = listOf(PauseMode.None, PauseMode.Seconds(3), PauseMode.Seconds(5), PauseMode.Seconds(10), PauseMode.AyahLength)
    }
}

data class TahfeezUi(
    val surah: Surah? = null,
    val config: TahfeezConfig? = null,
    val state: TahfeezState? = null,
    val ayahText: String = "",
    val hidden: Boolean = false,
    val paused: Boolean = false,
    /** 0..1 progress through the recite pause. */
    val reciteProgress: Float = 0f,
    val offlineMissing: Boolean = false,
)

/**
 * Runs a session: plays the ayah with its own player (not the media session, so the TV's media
 * keys keep controlling regular listening), times the recite pause, and saves progress.
 */
@UnstableApi
@HiltViewModel
class TahfeezSessionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedState: SavedStateHandle,
    private val repo: TahfeezRepository,
    private val quran: QuranRepository,
    private val resolver: AyahAudioResolver,
    private val local: LocalAudioStore,
    private val network: NetworkMonitor,
    dataSources: DataSource.Factory,
    quranPlayer: QuranPlayer,
    @com.mfouad.muslimtv.core.common.ApplicationScope private val appScope: kotlinx.coroutines.CoroutineScope,
) : ViewModel() {
    private val sessionId = savedState.toRoute<TahfeezSessionRoute>().sessionId
    private var engine: TahfeezEngine? = null
    private var pauseJob: Job? = null
    private var pauseRemainingMs = 0L

    private val _ui = MutableStateFlow(TahfeezUi())
    val ui: StateFlow<TahfeezUi> = _ui.asStateFlow()

    private val player = ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(dataSources)).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) onAudioEnded(duration.coerceAtLeast(0))
            }
        })
    }

    init {
        quranPlayer.pause()
        viewModelScope.launch {
            val session = repo.get(sessionId) ?: return@launch
            val start = if (session.finished) TahfeezState(ayah = session.config.fromAyah) else session.state
            engine = TahfeezEngine(session.config, start)
            _ui.value = TahfeezUi(surah = quran.surah(session.config.surah), config = session.config)
            playCurrent()
        }
    }

    private suspend fun playCurrent() {
        val e = engine ?: return
        publish()
        if (e.state.phase != TahfeezState.Phase.LISTEN) { save(); return }
        val reciter = Reciters.byId(e.config.reciterId)
        val ref = AyahRef(e.config.surah, e.state.ayah)
        if (!network.isOnline.first() && !local.isDownloaded(reciter.id, ref)) {
            _ui.update { it.copy(offlineMissing = true) }
            return
        }
        player.setMediaItem(MediaItem.fromUri(resolver.uri(reciter, ref)))
        player.prepare()
        player.play()
        save()
    }

    private fun onAudioEnded(durationMs: Long) {
        val e = engine ?: return
        val s = e.onAudioEnded(durationMs)
        if (s.phase == TahfeezState.Phase.RECITE) startPause(s.pauseMs) else viewModelScope.launch { playCurrent() }
    }

    private fun startPause(totalMs: Long, remainingMs: Long = totalMs) {
        publish()
        pauseRemainingMs = remainingMs
        pauseJob?.cancel()
        pauseJob = viewModelScope.launch {
            while (isActive && pauseRemainingMs > 0) {
                _ui.update { it.copy(reciteProgress = 1f - pauseRemainingMs.toFloat() / totalMs) }
                delay(TICK_MS)
                pauseRemainingMs -= TICK_MS
            }
            engine?.onPauseEnded()
            playCurrent()
        }
    }

    /** OK: reveals hidden text early. */
    fun reveal() { engine?.reveal(); publish() }

    fun togglePause() {
        val e = engine ?: return
        if (_ui.value.paused) {
            _ui.update { it.copy(paused = false) }
            if (e.state.phase == TahfeezState.Phase.RECITE) startPause(e.state.pauseMs, pauseRemainingMs) else player.play()
        } else {
            _ui.update { it.copy(paused = true) }
            pauseJob?.cancel()
            player.pause()
        }
    }

    fun nextAyah() = viewModelScope.launch {
        pauseJob?.cancel()
        player.stop()
        engine?.nextAyah()
        _ui.update { it.copy(paused = false) }
        playCurrent()
    }

    fun restart() = viewModelScope.launch {
        val c = engine?.config ?: return@launch
        engine = TahfeezEngine(c)
        _ui.update { it.copy(paused = false) }
        playCurrent()
    }

    private fun publish() {
        val e = engine ?: return
        viewModelScope.launch {
            val text = quran.ayah(AyahRef(e.config.surah, e.state.ayah))?.let { QuranText.displayText(it.surah, it.ayah, it.text) }.orEmpty()
            _ui.update { it.copy(state = e.state, ayahText = text, hidden = e.isHidden, reciteProgress = if (e.state.phase == TahfeezState.Phase.RECITE) it.reciteProgress else 0f) }
        }
    }

    private suspend fun save() {
        val e = engine ?: return
        repo.save(sessionId, e.config, e.state)
    }

    override fun onCleared() {
        pauseJob?.cancel()
        player.release()
        val e = engine
        // Outlives the screen so the last position is saved even as it closes.
        if (e != null) appScope.launch { repo.save(sessionId, e.config, e.state) }
    }

    private companion object { const val TICK_MS = 100L }
}
