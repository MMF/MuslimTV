package com.mfouad.muslimtv.core.media

import com.google.common.truth.Truth.assertThat
import com.mfouad.muslimtv.core.model.AyahRef
import org.junit.Test

class QueueBuilderTest {
    @Test
    fun `a surah from the start begins with the basmala`() {
        val q = QueueBuilder.build(surah = 112, ayahCount = 4)
        assertThat(q.first()).isEqualTo(QueueEntry(AyahRef(112, 1), isBasmala = true))
        assertThat(q.first().audioRef).isEqualTo(AyahRef(1, 1))
        assertThat(q.drop(1).map { it.ref.ayah }).containsExactly(1, 2, 3, 4).inOrder()
    }

    @Test
    fun `al-fatiha and at-tawbah have no extra basmala`() {
        assertThat(QueueBuilder.build(1, 7).none { it.isBasmala }).isTrue()
        assertThat(QueueBuilder.build(9, 129).none { it.isBasmala }).isTrue()
    }

    @Test
    fun `starting mid-surah skips the basmala and clamps the range`() {
        val q = QueueBuilder.build(2, 286, from = 255, to = 999)
        assertThat(q.first()).isEqualTo(QueueEntry(AyahRef(2, 255)))
        assertThat(q.last().ref).isEqualTo(AyahRef(2, 286))
    }

    @Test
    fun `media ids round trip`() {
        listOf(QueueEntry(AyahRef(2, 255)), QueueEntry(AyahRef(36, 1), isBasmala = true)).forEach {
            assertThat(QueueEntry.decode(it.mediaId)).isEqualTo(it)
        }
        assertThat(QueueEntry.decode("junk")).isNull()
    }
}
