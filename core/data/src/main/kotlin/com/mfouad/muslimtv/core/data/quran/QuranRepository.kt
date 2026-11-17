package com.mfouad.muslimtv.core.data.quran

import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.profile.ActiveProfile
import com.mfouad.muslimtv.core.database.BookmarkEntity
import com.mfouad.muslimtv.core.database.PositionEntity
import com.mfouad.muslimtv.core.database.QuranDao
import com.mfouad.muslimtv.core.database.UserQuranDao
import com.mfouad.muslimtv.core.model.Ayah
import com.mfouad.muslimtv.core.model.AyahOfTheDay
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.Bookmark
import com.mfouad.muslimtv.core.model.Juz
import com.mfouad.muslimtv.core.model.ReaderAyah
import com.mfouad.muslimtv.core.model.Surah
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

enum class PositionKind(val key: String) { READ("read"), LISTEN("listen") }

data class QuranPosition(val ref: AyahRef, val reciterId: String?, val updatedAt: Long)

sealed interface QuranSearchResult {
    data class SurahMatch(val surah: Surah) : QuranSearchResult
    data class AyahMatch(val surah: Surah, val ayah: Int) : QuranSearchResult
}

interface QuranRepository {
    val surahs: List<Surah>
    val juzs: List<Juz>
    fun surah(number: Int): Surah
    /** [translation] is a note source id, or null for Quran text only. */
    suspend fun readerAyahs(surah: Int, translation: String?): List<ReaderAyah>
    suspend fun availableTranslations(): List<String>
    suspend fun ayah(ref: AyahRef): Ayah?
    suspend fun tafsir(ref: AyahRef): String?
    suspend fun ayahOfTheDay(date: LocalDate): AyahOfTheDay?
    fun search(query: String): List<QuranSearchResult>
    val bookmarks: Flow<List<Bookmark>>
    suspend fun setBookmark(ref: AyahRef, bookmarked: Boolean)
    fun position(kind: PositionKind): Flow<QuranPosition?>
    suspend fun savePosition(kind: PositionKind, ref: AyahRef, reciterId: String? = null)
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RoomQuranRepository @Inject constructor(
    private val importer: QuranImporter,
    private val metadata: QuranMetadata,
    private val quranDao: QuranDao,
    private val userDao: UserQuranDao,
    private val profile: ActiveProfile,
    private val time: TimeProvider,
) : QuranRepository {

    override val surahs: List<Surah> get() = metadata.surahs
    override val juzs: List<Juz> get() = metadata.juzs
    override fun surah(number: Int): Surah = metadata.surah(number)

    override suspend fun readerAyahs(surah: Int, translation: String?): List<ReaderAyah> {
        importer.ready()
        val translations = if (translation != null) {
            val rows = quranDao.notes(translation, surah).ifEmpty { quranDao.notes(BundledNotes.TRANSLATION.id, surah) }
            rows.associate { it.ayah to it.text }
        } else {
            emptyMap()
        }
        return quranDao.surah(surah).map { ReaderAyah(it.surah, it.ayah, it.juz, it.text, translations[it.ayah]) }
    }

    override suspend fun availableTranslations(): List<String> {
        importer.ready()
        return quranDao.noteSources().filter { it != BundledNotes.TAFSIR.id }
    }

    override suspend fun ayah(ref: AyahRef): Ayah? {
        importer.ready()
        return quranDao.ayah(ref.surah, ref.ayah)?.let { Ayah(it.surah, it.ayah, it.text) }
    }

    override suspend fun tafsir(ref: AyahRef): String? {
        importer.ready()
        return quranDao.note(BundledNotes.TAFSIR.id, ref.surah, ref.ayah)?.text
    }

    override suspend fun ayahOfTheDay(date: LocalDate): AyahOfTheDay? {
        val ref = AyahOfTheDaySelector.forDate(date)
        val ayah = ayah(ref) ?: return null
        return AyahOfTheDay(metadata.surah(ref.surah), ayah)
    }

    override fun search(query: String): List<QuranSearchResult> = QuranSearch.search(metadata.surahs, query)

    override val bookmarks: Flow<List<Bookmark>> = profile.id.flatMapLatest { id ->
        userDao.bookmarks(id).map { list -> list.map { Bookmark(AyahRef(it.surah, it.ayah), it.createdAt) } }
    }

    override suspend fun setBookmark(ref: AyahRef, bookmarked: Boolean) {
        val id = profile.id.first()
        if (bookmarked) userDao.addBookmark(BookmarkEntity(id, ref.surah, ref.ayah, time.nowMillis()))
        else userDao.removeBookmark(id, ref.surah, ref.ayah)
    }

    override fun position(kind: PositionKind): Flow<QuranPosition?> = profile.id.flatMapLatest { id ->
        userDao.position(id, kind.key).map { p -> p?.let { QuranPosition(AyahRef(it.surah, it.ayah), it.reciterId, it.updatedAt) } }
    }

    override suspend fun savePosition(kind: PositionKind, ref: AyahRef, reciterId: String?) {
        userDao.savePosition(PositionEntity(profile.id.first(), kind.key, ref.surah, ref.ayah, reciterId, time.nowMillis()))
    }
}

/**
 * Short, self-contained ayahs (at most ~80 letters), rotated by date so every TV in the home
 * shows the same ayah on the same day. Only references are stored; the text comes from Tanzil.
 */
object AyahOfTheDaySelector {
    val refs: List<AyahRef> = listOf(
        2 to 152, 2 to 153, 2 to 201, 3 to 8, 3 to 139, 3 to 200, 9 to 51, 13 to 28,
        14 to 7, 17 to 24, 18 to 10, 20 to 25, 23 to 118, 25 to 74, 29 to 69, 33 to 41,
        40 to 60, 55 to 13, 2 to 156, 94 to 5, 94 to 6, 2 to 45, 3 to 190, 7 to 23,
    ).map { (s, a) -> AyahRef(s, a) }

    fun forDate(date: LocalDate): AyahRef = refs[Math.floorMod(date.toEpochDay(), refs.size.toLong()).toInt()]
}
