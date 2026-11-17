package com.mfouad.muslimtv.core.data.quran

import android.content.Context
import android.util.Xml
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.Juz
import com.mfouad.muslimtv.core.model.Revelation
import com.mfouad.muslimtv.core.model.Surah
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.xmlpull.v1.XmlPullParser

/** Surah and juz metadata from the bundled Tanzil quran-data.xml, parsed once. */
@Singleton
class QuranMetadata @Inject constructor(@ApplicationContext private val context: Context) {
    private val parsed: Triple<List<Surah>, List<Juz>, List<AyahRef>> by lazy { parse() }

    val surahs: List<Surah> get() = parsed.first
    val juzs: List<Juz> get() = parsed.second

    /** First ayah of each of the 604 pages (Madani mushaf), index 0 = page 1. */
    val pageStarts: List<AyahRef> get() = parsed.third

    fun surah(number: Int): Surah = surahs[number - 1]

    /** Juz number containing [ref]. */
    fun juzOf(ref: AyahRef): Int = juzs.lastOrNull { it.start <= ref }?.number ?: 1

    private fun parse(): Triple<List<Surah>, List<Juz>, List<AyahRef>> {
        val surahs = ArrayList<Surah>(114)
        val juzs = ArrayList<Juz>(30)
        val pages = ArrayList<AyahRef>(604)
        context.assets.open(META_ASSET).use { input ->
            val p = Xml.newPullParser()
            p.setInput(input, "UTF-8")
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType != XmlPullParser.START_TAG) continue
                when (p.name) {
                    "sura" -> surahs += Surah(
                        number = p.attr("index").toInt(),
                        arabicName = p.attr("name"),
                        transliteration = p.attr("tname"),
                        englishName = p.attr("ename"),
                        ayahCount = p.attr("ayas").toInt(),
                        revelation = if (p.attr("type") == "Medinan") Revelation.MADANI else Revelation.MAKKI,
                    )
                    "juz" -> juzs += Juz(p.attr("index").toInt(), AyahRef(p.attr("sura").toInt(), p.attr("aya").toInt()))
                    "page" -> pages += AyahRef(p.attr("sura").toInt(), p.attr("aya").toInt())
                }
            }
        }
        return Triple(surahs, juzs, pages)
    }

    private fun XmlPullParser.attr(name: String): String = getAttributeValue(null, name)

    companion object {
        const val TEXT_ASSET = "quran/quran-uthmani.txt"
        const val META_ASSET = "quran/quran-data.xml"
    }
}
