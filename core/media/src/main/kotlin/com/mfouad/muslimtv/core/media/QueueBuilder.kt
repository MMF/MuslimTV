package com.mfouad.muslimtv.core.media

import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.QuranText

/** One audio item in the queue. The basmala item plays Al-Fatiha 1:1 before a surah's first ayah. */
data class QueueEntry(val ref: AyahRef, val isBasmala: Boolean = false) {
    val mediaId: String get() = encode(this)

    /** The audio file to play: the basmala uses 1:1. */
    val audioRef: AyahRef get() = if (isBasmala) AyahRef(1, 1) else ref

    companion object {
        fun encode(e: QueueEntry) = (if (e.isBasmala) "b:" else "a:") + "${e.ref.surah}:${e.ref.ayah}"

        fun decode(id: String?): QueueEntry? {
            val p = id?.split(':') ?: return null
            if (p.size != 3) return null
            val ref = AyahRef(p[1].toIntOrNull() ?: return null, p[2].toIntOrNull() ?: return null)
            return QueueEntry(ref, isBasmala = p[0] == "b")
        }
    }
}

/** Builds the per-ayah queue for a range of one surah. Pure, so it is unit tested. */
object QueueBuilder {
    fun build(surah: Int, ayahCount: Int, from: Int = 1, to: Int = ayahCount, includeBasmala: Boolean = true): List<QueueEntry> {
        val start = from.coerceIn(1, ayahCount)
        val end = to.coerceIn(start, ayahCount)
        val ayahs = (start..end).map { QueueEntry(AyahRef(surah, it)) }
        return if (includeBasmala && start == 1 && QuranText.hasBasmalaHeader(surah)) listOf(QueueEntry(AyahRef(surah, 1), isBasmala = true)) + ayahs else ayahs
    }
}
