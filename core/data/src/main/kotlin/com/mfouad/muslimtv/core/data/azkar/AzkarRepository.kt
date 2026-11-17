package com.mfouad.muslimtv.core.data.azkar

import android.content.Context
import com.mfouad.muslimtv.core.common.IoDispatcher
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.AzkarCategory
import com.mfouad.muslimtv.core.model.AzkarChapter
import com.mfouad.muslimtv.core.model.DivineName
import com.mfouad.muslimtv.core.model.Zikr
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import android.util.JsonReader
import org.json.JSONObject

/**
 * Hisn al-Muslim (Arabic, with repeat counts and hadith references; MIT dataset by A. Sellam) and
 * the 99 Names (MIT, teamsazen/99-Names), both bundled and read once.
 */
@Singleton
class AzkarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private var chapters: List<AzkarChapter>? = null
    private var names: List<DivineName>? = null

    suspend fun chapters(): List<AzkarChapter> = mutex.withLock {
        chapters ?: withContext(io) { parseChapters() }.also { chapters = it }
    }

    suspend fun chapter(id: Int): AzkarChapter? = chapters().getOrNull(id)

    suspend fun forCategory(category: AzkarCategory): AzkarChapter? {
        val key = CATEGORY_TITLES[category] ?: return null
        return chapters().firstOrNull { letters(it.title) == key }
    }

    /** Everything that is not a daily routine: travel, rain, illness, food, and so on. */
    suspend fun occasions(): List<AzkarChapter> = chapters().filter { letters(it.title) !in CATEGORY_TITLES.values }

    suspend fun names(): List<DivineName> = mutex.withLock {
        names ?: withContext(io) { parseNames() }.also { names = it }
    }

    /** Streams the file so chapters keep the book's order (ids are positions in the file). */
    private fun parseChapters(): List<AzkarChapter> {
        val result = ArrayList<AzkarChapter>()
        JsonReader(context.assets.open(HISN).bufferedReader()).use { r ->
            r.beginObject()
            while (r.hasNext()) {
                val title = r.nextName()
                val items = ArrayList<Zikr>()
                r.beginObject()
                while (r.hasNext()) {
                    if (r.nextName() != "Adhkar") { r.skipValue(); continue }
                    r.beginArray()
                    while (r.hasNext()) {
                        var text = ""
                        var count = 1
                        var reference = ""
                        r.beginObject()
                        while (r.hasNext()) {
                            when (r.nextName()) {
                                "Text" -> text = r.nextString()
                                "Count" -> count = runCatching { r.nextInt() }.getOrDefault(1)
                                "Reference" -> reference = r.nextString()
                                else -> r.skipValue()
                            }
                        }
                        r.endObject()
                        items += Zikr(text, count.coerceAtLeast(1), reference)
                    }
                    r.endArray()
                }
                r.endObject()
                result += AzkarChapter(result.size, title, items)
            }
            r.endObject()
        }
        return result
    }

    private fun parseNames(): List<DivineName> {
        val arr = JSONObject(context.assets.open(ASMA).bufferedReader().readText()).getJSONArray("names")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DivineName(o.getInt("number"), o.getString("arabic"), o.getString("transliteration"), o.getString("english"), o.getString("explanation"))
        }
    }

    companion object {
        private const val HISN = "azkar/hisn.json"
        private const val ASMA = "azkar/asma.json"

        /** Hisn al-Muslim chapter titles (letters only) for the daily categories. */
        val CATEGORY_TITLES = mapOf(
            AzkarCategory.WAKING to "أذكارالاستيقاظمنالنوم",
            AzkarCategory.AFTER_PRAYER to "الأذكاربعدالسلاممنالصلاة",
            AzkarCategory.MORNING to "أذكارالصباح",
            AzkarCategory.EVENING to "أذكارالمساء",
            AzkarCategory.SLEEP to "أذكارالنوم",
            AzkarCategory.IFTAR to "الدعاءعندإفطارالصائم",
        )

        /** Arabic letters only: drops vowel marks, tatweel and spaces so titles compare reliably. */
        fun letters(s: String): String = s.filter { it in '\u0621'..'\u063A' || it in '\u0641'..'\u064A' }

        /** Ayahs commonly recited for ruqyah, played from the Quran recitation. */
        val RUQYAH: List<Pair<AyahRef, AyahRef>> = listOf(
            AyahRef(1, 1) to AyahRef(1, 7),
            AyahRef(2, 1) to AyahRef(2, 5),
            AyahRef(2, 255) to AyahRef(2, 255),
            AyahRef(2, 285) to AyahRef(2, 286),
            AyahRef(3, 18) to AyahRef(3, 19),
            AyahRef(7, 117) to AyahRef(7, 122),
            AyahRef(10, 81) to AyahRef(10, 82),
            AyahRef(20, 69) to AyahRef(20, 69),
            AyahRef(112, 1) to AyahRef(112, 4),
            AyahRef(113, 1) to AyahRef(113, 5),
            AyahRef(114, 1) to AyahRef(114, 6),
        )
    }
}
