package com.mfouad.muslimtv.core.model

/** Silence after each ayah for the learner to recite it back. */
sealed interface PauseMode {
    data object None : PauseMode
    data class Seconds(val seconds: Int) : PauseMode
    /** As long as the ayah itself, so a learner has the same time to repeat it. */
    data object AyahLength : PauseMode

    fun durationMs(ayahMs: Long): Long = when (this) {
        None -> 0
        is Seconds -> seconds * 1_000L
        AyahLength -> ayahMs
    }
}

data class TahfeezConfig(
    val surah: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val reciterId: String,
    /** Times each ayah is played before moving on; 0 means endless (until the user moves on). */
    val repeatsPerAyah: Int = 3,
    /** Times the whole range is played. */
    val rangeRepeats: Int = 1,
    val pause: PauseMode = PauseMode.AyahLength,
    val hideText: Boolean = false,
) {
    val ayahCount: Int get() = toAyah - fromAyah + 1
    val endless: Boolean get() = repeatsPerAyah == 0

    companion object {
        const val MAX_REPEATS = 20
        const val MAX_ROUNDS = 10

        /** Kids' preset: a Juz Amma surah with a teaching (muallim) reciter and repeat-after-me. */
        fun kids(surah: Int, ayahCount: Int, reciterId: String) = TahfeezConfig(
            surah = surah, fromAyah = 1, toAyah = ayahCount, reciterId = reciterId,
            repeatsPerAyah = 3, rangeRepeats = 1, pause = PauseMode.AyahLength, hideText = false,
        )
    }
}

/** Where the session is. [revealed] matters only with hidden text. */
data class TahfeezState(
    val ayah: Int,
    val repeat: Int = 1,
    val round: Int = 1,
    val phase: Phase = Phase.LISTEN,
    val pauseMs: Long = 0,
    val revealed: Boolean = false,
) {
    enum class Phase { LISTEN, RECITE, DONE }
}

/**
 * The memorization loop: listen to an ayah, then a silent pause to recite it back; repeat the ayah
 * N times, move through the range, and repeat the whole range M times. Pure: the screen feeds it
 * "audio ended" and "pause ended" events. Unit tested.
 */
class TahfeezEngine(val config: TahfeezConfig, start: TahfeezState? = null) {
    var state: TahfeezState = start ?: TahfeezState(ayah = config.fromAyah)
        private set

    val isHidden: Boolean
        get() = config.hideText && !state.revealed && state.phase != TahfeezState.Phase.DONE

    /** The ayah audio finished. Goes to the recite pause, or straight on when there is none. */
    fun onAudioEnded(ayahDurationMs: Long): TahfeezState {
        if (state.phase != TahfeezState.Phase.LISTEN) return state
        val pause = config.pause.durationMs(ayahDurationMs)
        state = if (pause > 0) state.copy(phase = TahfeezState.Phase.RECITE, pauseMs = pause) else advance()
        return state
    }

    fun onPauseEnded(): TahfeezState {
        if (state.phase != TahfeezState.Phase.RECITE) return state
        state = advance()
        return state
    }

    /** OK while the text is hidden shows it early. */
    fun reveal(): TahfeezState { state = state.copy(revealed = true); return state }

    /** Skip to the next ayah regardless of repeats (needed for endless mode). */
    fun nextAyah(): TahfeezState { state = moveOn(); return state }

    private fun advance(): TahfeezState =
        if (config.endless || state.repeat < config.repeatsPerAyah) {
            state.copy(repeat = state.repeat + 1, phase = TahfeezState.Phase.LISTEN, pauseMs = 0, revealed = false)
        } else {
            moveOn()
        }

    private fun moveOn(): TahfeezState = when {
        state.ayah < config.toAyah -> TahfeezState(ayah = state.ayah + 1, round = state.round)
        state.round < config.rangeRepeats -> TahfeezState(ayah = config.fromAyah, round = state.round + 1)
        else -> state.copy(phase = TahfeezState.Phase.DONE, pauseMs = 0)
    }
}
