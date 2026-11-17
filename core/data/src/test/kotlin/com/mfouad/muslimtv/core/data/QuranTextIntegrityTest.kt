package com.mfouad.muslimtv.core.data

import com.google.common.truth.Truth.assertThat
import com.mfouad.muslimtv.core.data.quran.AyahOfTheDaySelector
import com.mfouad.muslimtv.core.model.QuranText
import java.io.File
import java.security.MessageDigest
import org.junit.Test

/**
 * The Quran text must stay exactly as published by Tanzil (Uthmani v1.1). If this test fails,
 * the bundled file was changed: restore it from tanzil.net, never edit it by hand.
 */
class QuranTextIntegrityTest {
    private val file = File("src/main/assets/quran/quran-uthmani.txt")

    @Test
    fun `bundled Quran text matches the published Tanzil checksum`() {
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        val hex = digest.joinToString("") { "%02x".format(it) }
        assertThat(hex).isEqualTo(TANZIL_UTHMANI_SHA256)
    }

    @Test
    fun `text has 6236 ayahs across 114 surahs in order`() {
        val rows = file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split('|', limit = 3) }
        assertThat(rows).hasSize(6236)
        assertThat(rows.map { it[0].toInt() }.distinct()).isEqualTo((1..114).toList())
        assertThat(rows.first().take(2)).containsExactly("1", "1").inOrder()
        assertThat(rows.last().take(2)).containsExactly("114", "6").inOrder()
    }

    @Test
    fun `every ayah of the day reference exists in the text`() {
        val keys = file.readLines().filter { !it.startsWith("#") && it.isNotBlank() }
            .map { it.substringBeforeLast('|') }.toSet()
        AyahOfTheDaySelector.refs.forEach { ref -> assertThat(keys).contains("${ref.surah}|${ref.ayah}") }
    }

    @Test
    fun `ayahs of the day are short enough for the home card`() {
        val marks = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
        val text = file.readLines().filter { !it.startsWith("#") && it.isNotBlank() }
            .associate { it.substringBeforeLast('|') to it.substringAfterLast('|') }
        AyahOfTheDaySelector.refs.forEach { ref ->
            val letters = text.getValue("${ref.surah}|${ref.ayah}").replace(marks, "").length
            assertThat(letters).isAtMost(80)
        }
    }

    @Test
    fun `basmala constant matches al-fatiha 1 and prefixes other surahs`() {
        val text = file.readLines().filter { !it.startsWith("#") && it.isNotBlank() }
            .associate { it.substringBeforeLast('|') to it.substringAfterLast('|') }
        assertThat(text.getValue("1|1")).isEqualTo(QuranText.BASMALA)
        (2..114).filter { it != 9 }.forEach { s ->
            val shown = QuranText.displayText(s, 1, text.getValue("$s|1"))
            assertThat(shown.split(' ').size).isEqualTo(text.getValue("$s|1").split(' ').size - 4)
            assertThat(text.getValue("$s|1")).endsWith(shown)
        }
        assertThat(QuranText.displayText(1, 1, text.getValue("1|1"))).isEqualTo(QuranText.BASMALA)
    }

    private companion object {
        const val TANZIL_UTHMANI_SHA256 = "7f30c647331a61100ebf24a80507dc0fcdd9f2df97f1312b5b2dfcb982a7f326"
    }
}
