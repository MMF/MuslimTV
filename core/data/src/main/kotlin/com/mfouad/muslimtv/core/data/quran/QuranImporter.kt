package com.mfouad.muslimtv.core.data.quran

import android.content.Context
import androidx.room.withTransaction
import com.mfouad.muslimtv.core.common.ApplicationScope
import com.mfouad.muslimtv.core.common.IoDispatcher
import com.mfouad.muslimtv.core.database.AyahEntity
import com.mfouad.muslimtv.core.database.AyahNoteEntity
import com.mfouad.muslimtv.core.database.MuslimTvDatabase
import com.mfouad.muslimtv.core.model.AyahRef
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/** A bundled translation or tafsir file in Tanzil "sura|aya|text" format. */
data class NoteSource(val id: String, val asset: String, val isTafsir: Boolean)

object BundledNotes {
    /** Pickthall (1930), public domain: the translator died in 1936. */
    val TRANSLATION = NoteSource("en.pickthall", "quran/en.pickthall.txt", isTafsir = false)

    /** Tafsir al-Jalalayn (15th century), Arabic. */
    val TAFSIR = NoteSource("ar.jalalayn", "quran/ar.jalalayn.txt", isTafsir = true)

    val all = listOf(TRANSLATION, TAFSIR)
}

/**
 * Copies the bundled, verbatim Tanzil text (and bundled translation/tafsir) into Room once, so
 * every Quran screen reads from the local database. Runs at app start in the background; screens
 * await [ready], which is already complete on every launch after the first.
 */
@Singleton
class QuranImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: MuslimTvDatabase,
    private val metadata: QuranMetadata,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationScope scope: CoroutineScope,
) {
    private val job = scope.async(io, start = CoroutineStart.LAZY) { importIfNeeded() }

    fun start() { job.start() }

    suspend fun ready() { job.await() }

    private suspend fun importIfNeeded() {
        val dao = db.quranDao()
        if (dao.ayahCount() != AYAH_COUNT) {
            val rows = readTanzil(QuranMetadata.TEXT_ASSET).map { (ref, text) ->
                AyahEntity(ref.surah, ref.ayah, metadata.juzOf(ref), text)
            }
            check(rows.size == AYAH_COUNT) { "Bundled Quran text has ${rows.size} ayahs" }
            db.withTransaction { rows.chunked(CHUNK).forEach { dao.insertAyahs(it) } }
        }
        for (source in BundledNotes.all) {
            if (dao.noteCount(source.id) > 0) continue
            val rows = runCatching { readTanzil(source.asset) }.getOrNull() ?: continue // not bundled
            db.withTransaction {
                rows.map { (ref, text) -> AyahNoteEntity(source.id, ref.surah, ref.ayah, text) }
                    .chunked(CHUNK).forEach { dao.insertNotes(it) }
            }
        }
    }

    private suspend fun readTanzil(asset: String): List<Pair<AyahRef, String>> = withContext(io) {
        context.assets.open(asset).bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { line ->
                val p = line.split('|', limit = 3)
                if (p.size < 3) null else AyahRef(p[0].toInt(), p[1].toInt()) to p[2]
            }.toList()
        }
    }

    private companion object {
        const val AYAH_COUNT = 6236
        const val CHUNK = 500
    }
}
