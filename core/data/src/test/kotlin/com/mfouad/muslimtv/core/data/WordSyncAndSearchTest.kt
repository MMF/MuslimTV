package com.mfouad.muslimtv.core.data

import com.google.common.truth.Truth.assertThat
import com.mfouad.muslimtv.core.data.audio.FileTimingSource
import com.mfouad.muslimtv.core.data.audio.WordSync
import com.mfouad.muslimtv.core.data.quran.QuranSearch
import com.mfouad.muslimtv.core.data.quran.QuranSearchResult
import com.mfouad.muslimtv.core.model.QuranText
import com.mfouad.muslimtv.core.model.Revelation
import com.mfouad.muslimtv.core.model.Surah
import com.mfouad.muslimtv.core.model.WordSegment
import org.junit.Test

class WordSyncAndSearchTest {
    private val segs = listOf(WordSegment(1, 0, 500), WordSegment(2, 500, 900), WordSegment(3, 1000, 1600))

    @Test
    fun `word follows the real playback position`() {
        assertThat(WordSync.wordAt(0, segs)).isEqualTo(1)
        assertThat(WordSync.wordAt(499, segs)).isEqualTo(1)
        assertThat(WordSync.wordAt(500, segs)).isEqualTo(2)
        assertThat(WordSync.wordAt(1599, segs)).isEqualTo(3)
    }

    @Test
    fun `a gap between words keeps the previous word lit, and seeking back works`() {
        assertThat(WordSync.wordAt(950, segs)).isEqualTo(2)
        assertThat(WordSync.wordAt(5000, segs)).isEqualTo(3)
        assertThat(WordSync.wordAt(100, segs)).isEqualTo(1) // after a backwards seek
    }

    @Test
    fun `no timing before the first word or without data`() {
        assertThat(WordSync.wordAt(10, listOf(WordSegment(1, 200, 400)))).isNull()
        assertThat(WordSync.wordAt(10, emptyList())).isNull()
    }

    @Test
    fun `timing json parses`() {
        val map = FileTimingSource.parse("""{"1:1":[[1,0,640],[2,640,1180]]}""")
        assertThat(map.getValue("1:1")).containsExactly(WordSegment(1, 0, 640), WordSegment(2, 640, 1180)).inOrder()
    }

    @Test
    fun `pause marks are not counted as words`() {
        val tokens = QuranText.tokens("ٱلْقَيُّومُ ۚ لَا تَأْخُذُهُۥ")
        assertThat(tokens.map { it.wordIndex }).containsExactly(1, 0, 2, 3).inOrder()
    }

    private val surahs = listOf(
        Surah(1, "الفاتحة", "Al-Faatiha", "The Opening", 7, Revelation.MAKKI),
        Surah(2, "البقرة", "Al-Baqara", "The Cow", 286, Revelation.MADANI),
        Surah(36, "يس", "Yaseen", "Yaseen", 83, Revelation.MAKKI),
    )

    @Test
    fun `search by ayah reference in several notations`() {
        listOf("2:255", "2 255", "٢:٢٥٥", "2.255").forEach { q ->
            val r = QuranSearch.search(surahs, q).single() as QuranSearchResult.AyahMatch
            assertThat(r.surah.number to r.ayah).isEqualTo(2 to 255)
        }
    }

    @Test
    fun `search by name ignores the article and diacritics`() {
        assertThat(QuranSearch.search(surahs, "baqara").map { (it as QuranSearchResult.SurahMatch).surah.number }).containsExactly(2)
        assertThat(QuranSearch.search(surahs, "البقره").map { (it as QuranSearchResult.SurahMatch).surah.number }).containsExactly(2)
        assertThat(QuranSearch.search(surahs, "Al-Faatiha").map { (it as QuranSearchResult.SurahMatch).surah.number }).containsExactly(1)
        assertThat(QuranSearch.search(surahs, "36").map { (it as QuranSearchResult.SurahMatch).surah.number }).containsExactly(36)
    }
}
