package com.mfouad.muslimtv.core.model

import com.google.common.truth.Truth.assertThat
import com.mfouad.muslimtv.core.model.TahfeezState.Phase
import org.junit.Test

class TahfeezEngineTest {
    private fun config(repeats: Int = 2, rounds: Int = 1, pause: PauseMode = PauseMode.Seconds(5), hide: Boolean = false) =
        TahfeezConfig(surah = 114, fromAyah = 1, toAyah = 3, reciterId = "husary", repeatsPerAyah = repeats, rangeRepeats = rounds, pause = pause, hideText = hide)

    /** Plays one listen + recite cycle. */
    private fun TahfeezEngine.cycle(ayahMs: Long = 4_000) { onAudioEnded(ayahMs); onPauseEnded() }

    @Test
    fun `each ayah repeats N times with a pause after every recitation`() {
        val e = TahfeezEngine(config(repeats = 2))
        assertThat(e.onAudioEnded(4_000)).isEqualTo(TahfeezState(ayah = 1, repeat = 1, phase = Phase.RECITE, pauseMs = 5_000))
        assertThat(e.onPauseEnded()).isEqualTo(TahfeezState(ayah = 1, repeat = 2))
        e.cycle()
        assertThat(e.state).isEqualTo(TahfeezState(ayah = 2, repeat = 1))
    }

    @Test
    fun `the whole range repeats for the requested rounds and then finishes`() {
        val e = TahfeezEngine(config(repeats = 1, rounds = 2))
        repeat(3) { e.cycle() }
        assertThat(e.state).isEqualTo(TahfeezState(ayah = 1, repeat = 1, round = 2))
        repeat(3) { e.cycle() }
        assertThat(e.state.phase).isEqualTo(Phase.DONE)
        // Late events after the end are ignored.
        assertThat(e.onAudioEnded(1)).isEqualTo(e.state)
    }

    @Test
    fun `pause equal to ayah length uses the real audio duration`() {
        val e = TahfeezEngine(config(pause = PauseMode.AyahLength))
        assertThat(e.onAudioEnded(7_300).pauseMs).isEqualTo(7_300)
    }

    @Test
    fun `no pause goes straight to the next repetition`() {
        val e = TahfeezEngine(config(repeats = 2, pause = PauseMode.None))
        assertThat(e.onAudioEnded(4_000)).isEqualTo(TahfeezState(ayah = 1, repeat = 2))
    }

    @Test
    fun `endless repeats stay on the ayah until the user moves on`() {
        val e = TahfeezEngine(config(repeats = 0))
        repeat(25) { e.cycle() }
        assertThat(e.state.ayah).isEqualTo(1)
        assertThat(e.state.repeat).isEqualTo(26)
        assertThat(e.nextAyah()).isEqualTo(TahfeezState(ayah = 2))
    }

    @Test
    fun `hidden text stays hidden until revealed, and hides again on the next repetition`() {
        val e = TahfeezEngine(config(hide = true))
        assertThat(e.isHidden).isTrue()
        e.onAudioEnded(4_000)
        e.reveal()
        assertThat(e.isHidden).isFalse()
        e.onPauseEnded()
        assertThat(e.isHidden).isTrue()
    }

    @Test
    fun `events in the wrong phase are ignored`() {
        val e = TahfeezEngine(config())
        assertThat(e.onPauseEnded()).isEqualTo(TahfeezState(ayah = 1))
        e.onAudioEnded(1_000)
        assertThat(e.onAudioEnded(1_000).phase).isEqualTo(Phase.RECITE)
    }

    @Test
    fun `a saved session resumes where it stopped`() {
        val e = TahfeezEngine(config(), start = TahfeezState(ayah = 3, repeat = 2, round = 1))
        e.cycle()
        assertThat(e.state.phase).isEqualTo(Phase.DONE)
    }

    @Test
    fun `kids preset repeats each ayah three times with repeat-after-me pauses`() {
        val c = TahfeezConfig.kids(surah = 112, ayahCount = 4, reciterId = "husary_muallim")
        assertThat(c.repeatsPerAyah).isEqualTo(3)
        assertThat(c.pause).isEqualTo(PauseMode.AyahLength)
        assertThat(c.fromAyah to c.toAyah).isEqualTo(1 to 4)
    }
}
