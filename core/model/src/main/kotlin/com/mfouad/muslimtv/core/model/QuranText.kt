package com.mfouad.muslimtv.core.model

/** Display helpers for the Tanzil text. They never change the stored text, only how it is split. */
object QuranText {
    /** The basmala exactly as it appears at the start of ayah 1 in the Tanzil Uthmani text (escaped so editors cannot reorder its marks). */
    const val BASMALA = "\u0628\u0650\u0633\u0652\u0645\u0650\u0020\u0671\u0644\u0644\u0651\u064E\u0647\u0650\u0020\u0671\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0640\u0670\u0646\u0650\u0020\u0671\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650"

    /** Surahs whose first ayah does not start with a separate basmala in display. */
    fun hasBasmalaHeader(surah: Int): Boolean = surah != 1 && surah != 9

    /**
     * Tanzil prefixes ayah 1 of most surahs with the basmala. Readers show it as a header instead,
     * so this returns the ayah text after the basmala. Surah 1 (where the basmala is ayah 1) and
     * surah 9 (which has none) are returned unchanged.
     */
    fun displayText(surah: Int, ayah: Int, text: String): String {
        if (ayah != 1 || !hasBasmalaHeader(surah)) return text
        val words = text.split(' ')
        // Match by letters only: a few surahs (e.g. 95, 97) write the basmala with an extra shadda.
        val isBasmala = words.size > BASMALA_WORDS &&
            words.take(BASMALA_WORDS).joinToString(" ") { skeleton(it) } == BASMALA.split(' ').joinToString(" ") { skeleton(it) }
        return if (isBasmala) words.drop(BASMALA_WORDS).joinToString(" ") else text
    }

    private const val BASMALA_WORDS = 4

    /** Letters only, without vowel and recitation marks. */
    private fun skeleton(word: String): String = word.filter { c -> c !in '\u064B'..'\u065F' && c != '\u0670' && c !in markRange && c != '\u0640' }

    private val markRange = 'ۖ'..'ۭ'

    /**
     * Splits display text into tokens and marks which are real words. Pause signs (ۚ ۖ ۗ …) are
     * separate tokens in the Tanzil text but are not words for timing purposes.
     */
    fun tokens(text: String): List<Token> {
        var word = 0
        return text.split(' ').filter { it.isNotEmpty() }.map { t ->
            val isWord = t.any { it !in markRange }
            if (isWord) word++
            Token(t, if (isWord) word else 0)
        }
    }

    /** [wordIndex] is 1-based for words, 0 for pause marks. */
    data class Token(val text: String, val wordIndex: Int)

    private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun arabicNumber(n: Int): String = n.toString().map { arabicDigits[it - '0'] }.joinToString("")

    /** End-of-ayah ornament followed by the ayah number, which Quran fonts draw inside it. */
    /**
     * Ayah number between ornate brackets: ﴿١٢﴾. The single End-of-Ayah sign (U+06DD) only wraps
     * the digits on text engines that support it; on some TVs the number showed beside the circle.
     */
    fun ayahMarker(n: Int): String = "\uFD3F" + arabicNumber(n) + "\uFD3E"
}
