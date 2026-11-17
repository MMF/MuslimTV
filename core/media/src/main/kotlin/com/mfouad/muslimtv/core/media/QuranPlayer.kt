package com.mfouad.muslimtv.core.media

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.mfouad.muslimtv.core.common.ApplicationScope
import com.mfouad.muslimtv.core.common.NetworkMonitor
import com.mfouad.muslimtv.core.data.audio.AyahAudioResolver
import com.mfouad.muslimtv.core.data.audio.LocalAudioStore
import com.mfouad.muslimtv.core.data.audio.Reciters
import com.mfouad.muslimtv.core.data.quran.PositionKind
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.RepeatMode
import com.mfouad.muslimtv.core.model.Reciter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class SleepTimer(val minutes: Int) { OFF(0), MIN_15(15), MIN_30(30), MIN_60(60), END_OF_SURAH(-1) }

sealed interface PlaybackProblem {
    /** Offline and this surah is not downloaded for the reciter; [alternatives] have it downloaded. */
    data class NotDownloaded(val surah: Int, val reciter: Reciter, val alternatives: List<Reciter>) : PlaybackProblem
    data class Network(val surah: Int) : PlaybackProblem
}

data class PlayerState(
    val reciter: Reciter = Reciters.all.first(),
    val surah: Int = 0,
    /** The ayah being recited; for the basmala item this is the surah's first ayah with [isBasmala]. */
    val current: AyahRef? = null,
    val isBasmala: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val speed: Float = 1f,
    val repeat: RepeatMode = RepeatMode.OFF,
    val sleep: SleepTimer = SleepTimer.OFF,
    val sleepEndsAt: Long = 0,
    val problem: PlaybackProblem? = null,
) {
    val isActive: Boolean get() = current != null
}

/**
 * The app's single entry point to playback. Builds per-ayah queues (gapless in ExoPlayer),
 * talks to [PlaybackService] through a MediaController, and exposes state for follow-along.
 * The ayah and word highlight are driven by the player's real item index and position, so they
 * stay right after pause, seek and skip.
 */
@UnstableApi
@Singleton
class QuranPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resolver: AyahAudioResolver,
    private val local: LocalAudioStore,
    private val quran: QuranRepository,
    private val network: NetworkMonitor,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    /** Position inside the current ayah file, refreshed ~10 times a second while playing. */
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var sleepJob: Job? = null
    /** Continue into the next surah when one ends (unless repeating). */
    var continuous: Boolean = true

    private suspend fun controller(): MediaController {
        controller?.let { return it }
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        val c = suspendCancellableCoroutine { cont ->
            future.addListener({
                runCatching { future.get() }.onSuccess { cont.resume(it) }.onFailure { cont.resumeWithException(it) }
            }, ContextCompat.getMainExecutor(context))
            cont.invokeOnCancellation { future.cancel(true) }
        }
        c.addListener(listener)
        controller = c
        return c
    }

    /** Starts [surah] from [fromAyah] with [reciter]. Returns a problem instead of failing silently. */
    // MediaController may only be used on the main thread; auto-continue and next/previous surah
    // call these from the background application scope, which crashed the app at a surah's end.
    suspend fun play(reciter: Reciter, surah: Int, fromAyah: Int = 1, toAyah: Int? = null): PlaybackProblem? =
        withContext(Dispatchers.Main.immediate) { playOnMain(reciter, surah, fromAyah, toAyah) }

    private suspend fun playOnMain(reciter: Reciter, surah: Int, fromAyah: Int, toAyah: Int?): PlaybackProblem? {
        val info = quran.surah(surah)
        val online = network.isOnline.first()
        if (!online && !local.isSurahDownloaded(reciter.id, surah, info.ayahCount)) {
            val alternatives = Reciters.all.filter { it.id != reciter.id && local.isSurahDownloaded(it.id, surah, info.ayahCount) }
            val problem = PlaybackProblem.NotDownloaded(surah, reciter, alternatives)
            _state.update { it.copy(problem = problem) }
            return problem
        }
        val basmalaPlayable = online || local.isDownloaded(reciter.id, AyahRef(1, 1))
        val entries = QueueBuilder.build(surah, info.ayahCount, fromAyah, toAyah ?: info.ayahCount, includeBasmala = basmalaPlayable)
        val items = entries.map { e ->
            MediaItem.Builder()
                .setMediaId(e.mediaId)
                .setUri(resolver.uri(reciter, e.audioRef))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("${info.transliteration} · ${if (e.isBasmala) "Basmala" else e.ref.ayah}")
                        .setArtist(reciter.nameEn)
                        .setAlbumTitle(info.arabicName)
                        .build(),
                )
                .build()
        }
        continuous = true
        val c = controller()
        c.setMediaItems(items, 0, 0L)
        c.setPlaybackSpeed(_state.value.speed)
        c.repeatMode = _state.value.repeat.toPlayer()
        c.prepare()
        c.play()
        _state.update { it.copy(reciter = reciter, surah = surah, problem = null) }
        syncFromController(c)
        return null
    }

    /** Plays several ayah ranges in a row (used for ruqyah); no basmala items, no auto-continue. */
    suspend fun playRanges(reciter: Reciter, ranges: List<Pair<AyahRef, AyahRef>>): PlaybackProblem? =
        withContext(Dispatchers.Main.immediate) { playRangesOnMain(reciter, ranges) }

    private suspend fun playRangesOnMain(reciter: Reciter, ranges: List<Pair<AyahRef, AyahRef>>): PlaybackProblem? {
        val online = network.isOnline.first()
        val entries = ranges.flatMap { (from, to) -> (from.ayah..to.ayah).map { QueueEntry(AyahRef(from.surah, it)) } }
        if (!online && entries.any { !local.isDownloaded(reciter.id, it.ref) }) {
            val alternatives = Reciters.all.filter { r -> r.id != reciter.id && entries.all { local.isDownloaded(r.id, it.ref) } }
            return PlaybackProblem.NotDownloaded(entries.first().ref.surah, reciter, alternatives).also { p -> _state.update { it.copy(problem = p) } }
        }
        val items = entries.map { e ->
            val info = quran.surah(e.ref.surah)
            MediaItem.Builder()
                .setMediaId(e.mediaId)
                .setUri(resolver.uri(reciter, e.ref))
                .setMediaMetadata(MediaMetadata.Builder().setTitle("${info.transliteration} · ${e.ref.ayah}").setArtist(reciter.nameEn).setAlbumTitle(info.arabicName).build())
                .build()
        }
        continuous = false
        val c = controller()
        c.setMediaItems(items, 0, 0L)
        c.repeatMode = Player.REPEAT_MODE_OFF
        c.prepare()
        c.play()
        _state.update { it.copy(reciter = reciter, surah = entries.first().ref.surah, problem = null, repeat = RepeatMode.OFF) }
        syncFromController(c)
        return null
    }

    fun togglePlayPause() = withController { if (it.isPlaying) it.pause() else { if (it.playbackState == Player.STATE_IDLE) it.prepare(); it.play() } }
    fun pause() = withController { it.pause() }
    fun nextAyah() = withController { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    fun previousAyah() = withController { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() else it.seekTo(0) }

    fun nextSurah() = scope.launch { val s = _state.value; if (s.surah in 1 until 114) play(s.reciter, s.surah + 1) }
    fun previousSurah() = scope.launch { val s = _state.value; if (s.surah > 1) play(s.reciter, s.surah - 1) }

    fun stop() = withController {
        it.stop()
        it.clearMediaItems()
        _state.update { s -> s.copy(current = null, isPlaying = false, surah = 0) }
    }

    fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        withController { it.setPlaybackSpeed(speed) }
    }

    fun setRepeat(mode: RepeatMode) {
        _state.update { it.copy(repeat = mode) }
        withController { it.repeatMode = mode.toPlayer() }
    }

    fun setSleepTimer(timer: SleepTimer) {
        sleepJob?.cancel()
        val endsAt = if (timer.minutes > 0) System.currentTimeMillis() + timer.minutes * 60_000L else 0L
        _state.update { it.copy(sleep = timer, sleepEndsAt = endsAt) }
        if (timer.minutes > 0) {
            sleepJob = scope.launch {
                delay(timer.minutes * 60_000L)
                pause()
                _state.update { it.copy(sleep = SleepTimer.OFF, sleepEndsAt = 0) }
            }
        }
    }

    fun dismissProblem() = _state.update { it.copy(problem = null) }

    private fun withController(block: (MediaController) -> Unit) {
        scope.launch(kotlinx.coroutines.Dispatchers.Main) { runCatching { block(controller()) } }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            controller?.let { syncFromController(it) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val entry = QueueEntry.decode(mediaItem?.mediaId) ?: return
            if (!entry.isBasmala) scope.launch { quran.savePosition(PositionKind.LISTEN, entry.ref, _state.value.reciter.id) }
            val s = _state.value
            if (s.sleep == SleepTimer.END_OF_SURAH && reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                controller?.pause()
                _state.update { it.copy(sleep = SleepTimer.OFF) }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            val s = _state.value
            when {
                s.sleep == SleepTimer.END_OF_SURAH -> _state.update { it.copy(sleep = SleepTimer.OFF) }
                continuous && s.repeat == RepeatMode.OFF && s.surah in 1 until 114 -> scope.launch { play(s.reciter, s.surah + 1) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(problem = PlaybackProblem.Network(it.surah), isPlaying = false) }
        }
    }

    private fun syncFromController(c: MediaController) {
        val entry = QueueEntry.decode(c.currentMediaItem?.mediaId)
        _state.update {
            it.copy(
                surah = entry?.ref?.surah ?: it.surah,
                current = entry?.ref,
                isBasmala = entry?.isBasmala == true,
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
            )
        }
        _positionMs.value = c.currentPosition
        if (c.isPlaying) startTicker(c) else ticker?.cancel()
    }

    private fun startTicker(c: MediaController) {
        if (ticker?.isActive == true) return
        ticker = scope.launch(kotlinx.coroutines.Dispatchers.Main) {
            while (isActive) {
                _positionMs.value = c.currentPosition
                delay(POSITION_TICK_MS)
            }
        }
    }

    private fun RepeatMode.toPlayer(): Int = when (this) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.AYAH -> Player.REPEAT_MODE_ONE
        RepeatMode.SURAH -> Player.REPEAT_MODE_ALL
    }

    private companion object {
        const val POSITION_TICK_MS = 100L
    }
}
