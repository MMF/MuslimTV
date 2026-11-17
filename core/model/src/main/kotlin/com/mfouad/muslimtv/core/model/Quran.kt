package com.mfouad.muslimtv.core.model

enum class Revelation { MAKKI, MADANI }

data class Surah(
    val number: Int,
    val arabicName: String,
    val transliteration: String,
    val englishName: String,
    val ayahCount: Int,
    val revelation: Revelation,
)

data class AyahRef(val surah: Int, val ayah: Int) : Comparable<AyahRef> {
    override fun compareTo(other: AyahRef): Int = compareValuesBy(this, other, AyahRef::surah, AyahRef::ayah)
    override fun toString(): String = "$surah:$ayah"
}

data class Ayah(val surah: Int, val ayah: Int, val text: String) {
    val ref: AyahRef get() = AyahRef(surah, ayah)
}

data class AyahOfTheDay(val surah: Surah, val ayah: Ayah)

data class Juz(val number: Int, val start: AyahRef)

/** An ayah as shown in the reader: Quran text plus optional translation and tafsir. */
data class ReaderAyah(
    val surah: Int,
    val ayah: Int,
    val juz: Int,
    val text: String,
    val translation: String? = null,
) {
    val ref: AyahRef get() = AyahRef(surah, ayah)
}

data class Bookmark(val ref: AyahRef, val createdAt: Long)

enum class RecitationStyle { MURATTAL, MUJAWWAD, MUALLIM }

data class Reciter(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val style: RecitationStyle,
    /** Folder on the audio host; the AudioSource turns it into URLs. */
    val folder: String,
    val bitrateKbps: Int,
    /** True when word-level timing data exists for this recitation. */
    val hasWordTimings: Boolean = false,
    val forKids: Boolean = false,
) {
    fun displayName(arabic: Boolean) = if (arabic) nameAr else nameEn
}

/** One word's time range inside a single ayah's audio file. Word positions start at 1. */
data class WordSegment(val word: Int, val startMs: Long, val endMs: Long)

enum class RepeatMode { OFF, AYAH, SURAH }
