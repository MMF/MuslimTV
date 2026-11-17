package com.mfouad.muslimtv.core.media

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.mfouad.muslimtv.core.model.AmbientSound
import com.mfouad.muslimtv.core.model.AmbientVolume
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Optional rain / stream / birds under the recitation, always far quieter (at most 15% volume)
 * and without audio focus so it never interrupts the recitation. The recordings are not seamless
 * loops, so two players crossfade a few seconds before each end.
 */
@UnstableApi
@Singleton
class AmbientSoundPlayer @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var players: List<ExoPlayer> = emptyList()
    private var job: Job? = null
    private var current: AmbientSound = AmbientSound.OFF

    fun play(sound: AmbientSound, volume: AmbientVolume) {
        if (sound == current && players.isNotEmpty()) { setVolume(volume); return }
        stop()
        current = sound
        val res = when (sound) {
            AmbientSound.OFF -> return
            AmbientSound.RAIN -> R.raw.ambient_rain
            AmbientSound.STREAM -> R.raw.ambient_stream
            AmbientSound.BIRDS -> R.raw.ambient_birds
        }
        val uri = "android.resource://${context.packageName}/$res"
        players = List(2) {
            ExoPlayer.Builder(context).build().apply {
                setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
            }
        }
        val target = volume.level
        job = scope.launch {
            var active = 0
            players[0].volume = 0f
            players[0].play()
            fade(players[0], 0f, target)
            while (isActive) {
                val p = players[active]
                val remaining = p.duration - p.currentPosition
                if (p.duration > 0 && remaining in 1..CROSSFADE_MS) {
                    val next = players[1 - active]
                    next.seekTo(0)
                    next.volume = 0f
                    next.play()
                    launch { fade(p, p.volume, 0f) }
                    fade(next, 0f, target)
                    p.pause()
                    active = 1 - active
                }
                delay(POLL_MS)
            }
        }
    }

    private suspend fun fade(p: ExoPlayer, from: Float, to: Float) {
        val steps = 20
        for (i in 1..steps) {
            p.volume = from + (to - from) * i / steps
            delay(CROSSFADE_MS / steps)
        }
    }

    private fun setVolume(v: AmbientVolume) = players.forEach { if (it.isPlaying) it.volume = v.level }

    fun stop() {
        job?.cancel()
        players.forEach { it.release() }
        players = emptyList()
        current = AmbientSound.OFF
    }

    private companion object {
        const val CROSSFADE_MS = 4_000L
        const val POLL_MS = 250L
    }
}
