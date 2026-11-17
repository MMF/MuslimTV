package com.mfouad.muslimtv.core.data.quran

import com.mfouad.muslimtv.core.model.Surah
import java.text.Normalizer

/**
 * Search by surah name (Arabic, transliteration or English) or by number:
 * "2" opens a surah, "2:255" or "2 255" an ayah. Pure, so it is unit tested.
 */
object QuranSearch {
    private val refPattern = Regex("^\\s*(\\d{1,3})\\s*[:.\\s/]\\s*(\\d{1,3})\\s*$")
    private val numberPattern = Regex("^\\s*(\\d{1,3})\\s*$")
    private val marks = Regex("[\\u064B-\\u065F\\u0670\\u0640\\u06D6-\\u06ED\\p{Mn}]")

    fun normalize(s: String): String = Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
        .replace(marks, "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
        .replace('ة', 'ه').replace('ى', 'ي')
        .replace(Regex("[-'’`]"), "")
        .replace(Regex("^(al|an|ar|as|at|ad|adh|az|ash)\\s*"), "")
        .replace(Regex("^ال"), "")
        .replace(Regex("\\s+"), "")

    fun search(surahs: List<Surah>, query: String): List<QuranSearchResult> {
        refPattern.matchEntire(convertDigits(query))?.let { m ->
            val s = m.groupValues[1].toInt()
            val a = m.groupValues[2].toInt()
            val surah = surahs.firstOrNull { it.number == s } ?: return emptyList()
            return if (a in 1..surah.ayahCount) listOf(QuranSearchResult.AyahMatch(surah, a)) else listOf(QuranSearchResult.SurahMatch(surah))
        }
        numberPattern.matchEntire(convertDigits(query))?.let { m ->
            val n = m.groupValues[1].toInt()
            return surahs.firstOrNull { it.number == n }?.let { listOf(QuranSearchResult.SurahMatch(it)) } ?: emptyList()
        }
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        fun keys(s: Surah) = listOf(s.arabicName, s.transliteration, s.englishName).map(::normalize)
        val prefix = surahs.filter { s -> keys(s).any { it.startsWith(q) } }
        val contains = surahs.filter { s -> s !in prefix && keys(s).any { it.contains(q) } }
        return (prefix + contains).map { QuranSearchResult.SurahMatch(it) }
    }

    /** Accepts Arabic-Indic and Persian digits typed or spoken. */
    private fun convertDigits(s: String): String = buildString {
        s.forEach { c ->
            append(
                when (c) {
                    in '٠'..'٩' -> '0' + (c - '٠')
                    in '۰'..'۹' -> '0' + (c - '۰')
                    else -> c
                },
            )
        }
    }
}
