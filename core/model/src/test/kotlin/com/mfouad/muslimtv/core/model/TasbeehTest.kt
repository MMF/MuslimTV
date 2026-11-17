package com.mfouad.muslimtv.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TasbeehTest {
    @Test
    fun `completes a round at the target and keeps counting`() {
        var t = Tasbeeh(target = 33)
        repeat(33) { t = t.tap() }
        assertThat(t.justCompleted).isTrue()
        assertThat(t.rounds).isEqualTo(1)
        t = t.tap()
        assertThat(t.justCompleted).isFalse()
        assertThat(t.inRound).isEqualTo(1)
    }

    @Test
    fun `changing the target starts over`() {
        val t = Tasbeeh(target = 33).tap().tap().withTarget(100)
        assertThat(t.count).isEqualTo(0)
        assertThat(t.target).isEqualTo(100)
    }
}
