package com.mfouad.muslimtv.core.data.city

import android.content.Context
import com.mfouad.muslimtv.core.common.IoDispatcher
import com.mfouad.muslimtv.core.model.City
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.SoftReference
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface CityRepository {
    suspend fun search(query: String, limit: Int = 30): List<City>

    /** Cities to show before the user types anything: local ones first, then well-known ones. */
    suspend fun suggestions(deviceTimeZone: String, limit: Int = 12): List<City>
}

/**
 * Reads the bundled GeoNames list (CC BY 4.0). The list is held through a SoftReference so a
 * low-memory TV box can reclaim it once the city picker is closed.
 */
@Singleton
class AssetCityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CityRepository {

    private val mutex = Mutex()
    private var cache = SoftReference<List<IndexedCity>>(null)

    override suspend fun search(query: String, limit: Int): List<City> = withContext(io) {
        val q = CitySearch.normalize(query)
        if (q.isEmpty()) return@withContext emptyList()
        CitySearch.rank(cities(), q, limit)
    }

    override suspend fun suggestions(deviceTimeZone: String, limit: Int): List<City> = withContext(io) {
        val all = cities()
        val local = all.asSequence().filter { it.city.timeZone == deviceTimeZone }.take(LOCAL_SUGGESTIONS).map { it.city }
        val featured = FEATURED_IDS.mapNotNull { id -> all.firstOrNull { it.city.id == id }?.city }
        (local + featured).distinctBy { it.id }.take(limit).toList()
    }

    private suspend fun cities(): List<IndexedCity> = mutex.withLock {
        cache.get() ?: load().also { cache = SoftReference(it) }
    }

    private fun load(): List<IndexedCity> = context.assets.open(ASSET).bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull(::parse).toList()
    }

    private fun parse(line: String): IndexedCity? {
        val p = line.split('|')
        if (p.size < FIELD_COUNT) return null
        val city = City(
            id = p[0].toLongOrNull() ?: return null,
            name = p[1],
            arabicName = p[3],
            countryCode = p[4],
            latitude = p[5].toDoubleOrNull() ?: return null,
            longitude = p[6].toDoubleOrNull() ?: return null,
            timeZone = p[7],
            population = p[8].toLongOrNull() ?: 0,
        )
        // Search keys are normalized at build time (field 10), so loading stays fast on TV boxes.
        val keys = p.getOrNull(9)?.split(';')?.filter { it.isNotEmpty() }
            ?: listOf(p[1], p[2], p[3]).map(CitySearch::normalize).filter { it.isNotEmpty() }
        return IndexedCity(city, keys)
    }

    private companion object {
        const val ASSET = "cities/cities.txt"
        const val FIELD_COUNT = 9
        const val LOCAL_SUGGESTIONS = 6
        // Makkah, Madinah, Cairo, Istanbul, Riyadh, Dubai, Jakarta, Karachi, London.
        val FEATURED_IDS = listOf(104515L, 109223L, 360630L, 745044L, 108410L, 292223L, 1642911L, 1174872L, 2643743L)
    }
}

internal data class IndexedCity(val city: City, val keys: List<String>)

internal object CitySearch {
    /** Common English spellings that differ from the GeoNames name. */
    private val aliases = mapOf(
        "mecca" to "makkah",
        "makka" to "makkah",
        "medina" to "madinah",
        "madina" to "madinah",
        "jidda" to "jeddah",
        "jedda" to "jeddah",
    )

    private val diacritics = Regex("[\\u064B-\\u065F\\u0670\\u0640\\p{Mn}]")

    fun normalize(text: String): String {
        val n = Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(diacritics, "")
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
            .replace('ة', 'ه').replace('ى', 'ي')
            .replace('\'', ' ').replace('’', ' ').replace('-', ' ')
            .replace(Regex("\\s+"), " ")
        return aliases[n] ?: n
    }

    /** Prefix matches rank above substring matches; within each, larger cities come first. */
    fun rank(cities: List<IndexedCity>, query: String, limit: Int): List<City> {
        val prefix = ArrayList<City>()
        val contains = ArrayList<City>()
        for (c in cities) {
            when {
                c.keys.any { it.startsWith(query) } -> prefix += c.city
                c.keys.any { it.contains(query) } -> contains += c.city
            }
            if (prefix.size >= limit) break
        }
        return (prefix + contains).take(limit)
    }
}
