package com.mfouad.muslimtv.core.data.tools

import android.content.Context
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Qibla
import com.mfouad.muslimtv.core.common.IoDispatcher
import com.mfouad.muslimtv.core.model.City
import com.mfouad.muslimtv.core.model.HijriDate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class Hadith(val collection: String, val collectionEn: String, val number: Int, val text: String)

data class Stream(val id: String, val nameEn: String, val nameAr: String, val url: String, val video: Boolean) {
    fun name(arabic: Boolean) = if (arabic) nameAr else nameEn
}

data class RemoteConfig(val live: List<Stream>, val radio: List<Stream>)

/** Hadith of the day (short Bukhari/Muslim narrations) and the Forty Hadith of an-Nawawi. Arabic, Unlicense. */
@Singleton
class HadithRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private var daily: List<Hadith>? = null
    private var nawawi: List<Hadith>? = null

    suspend fun ofTheDay(date: LocalDate): Hadith? = dailyList().let { list ->
        if (list.isEmpty()) null else list[Math.floorMod(date.toEpochDay(), list.size.toLong()).toInt()]
    }

    suspend fun nawawi(): List<Hadith> = mutex.withLock {
        nawawi ?: withContext(io) {
            val arr = JSONObject(context.assets.open("hadith/nawawi.json").bufferedReader().readText()).getJSONArray("hadiths")
            (0 until arr.length()).map { i -> arr.getJSONObject(i).let { Hadith("الأربعون النووية", "Forty Hadith of an-Nawawi", it.getInt("n"), it.getString("t")) } }
        }.also { nawawi = it }
    }

    private suspend fun dailyList(): List<Hadith> = mutex.withLock {
        daily ?: withContext(io) {
            val arr = JSONObject(context.assets.open("hadith/daily.json").bufferedReader().readText()).getJSONArray("hadiths")
            (0 until arr.length()).map { i -> arr.getJSONObject(i).let { Hadith(it.getString("c"), it.getString("ce"), it.getInt("n"), it.getString("t")) } }
        }.also { daily = it }
    }
}

/** Live streams and radio, from a remote JSON so links can change without an app update. */
@Singleton
class RemoteConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: OkHttpClient,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val cache = File(context.filesDir, "remote-config.json")

    /** Bundled or last downloaded copy, immediately. */
    suspend fun cached(): RemoteConfig = withContext(io) {
        parse(cache.takeIf { it.isFile }?.readText() ?: context.assets.open(BUNDLED).bufferedReader().readText())
    }

    /** Refreshes from [REMOTE_CONFIG_URL] when set; keeps the cached copy on any failure. */
    suspend fun refresh(): RemoteConfig = withContext(io) {
        if (REMOTE_CONFIG_URL.isNotBlank()) {
            runCatching {
                http.newCall(Request.Builder().url(REMOTE_CONFIG_URL).build()).execute().use { r ->
                    val body = r.body?.string()
                    if (r.isSuccessful && body != null) { parse(body); cache.writeText(body) }
                }
            }
        }
        cached()
    }

    companion object {
        /** Where the editable config lives once hosted (e.g. a GitHub raw URL). Empty = bundled only. */
        const val REMOTE_CONFIG_URL = ""
        private const val BUNDLED = "config/remote.json"

        fun parse(json: String): RemoteConfig {
            val o = JSONObject(json)
            fun list(key: String) = o.optJSONArray(key)?.let { arr ->
                (0 until arr.length()).map { i ->
                    arr.getJSONObject(i).let { Stream(it.getString("id"), it.getString("nameEn"), it.getString("nameAr"), it.getString("url"), it.optBoolean("video")) }
                }
            }.orEmpty()
            return RemoteConfig(list("live"), list("radio"))
        }
    }
}

data class QiblaInfo(val bearingDegrees: Double, val distanceKm: Double, val path: List<Pair<Double, Double>>)

/** Qibla bearing from true north (Adhan library), distance, and the great-circle path for the map. */
object QiblaCalculator {
    const val KAABA_LAT = 21.4225241
    const val KAABA_LNG = 39.8261818

    fun forCity(city: City): QiblaInfo {
        val bearing = Qibla(Coordinates(city.latitude, city.longitude)).direction
        return QiblaInfo(bearing, distanceKm(city.latitude, city.longitude, KAABA_LAT, KAABA_LNG), path(city.latitude, city.longitude, KAABA_LAT, KAABA_LNG))
    }

    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * asin(sqrt(a))
    }

    /** Points along the great circle (lat, lng), for drawing the shortest route on the map. */
    fun path(lat1: Double, lng1: Double, lat2: Double, lng2: Double, steps: Int = 64): List<Pair<Double, Double>> {
        val p1 = Math.toRadians(lat1); val l1 = Math.toRadians(lng1)
        val p2 = Math.toRadians(lat2); val l2 = Math.toRadians(lng2)
        val d = 2 * asin(sqrt(sin((p2 - p1) / 2).let { it * it } + cos(p1) * cos(p2) * sin((l2 - l1) / 2).let { it * it }))
        if (d == 0.0) return listOf(lat1 to lng1)
        return (0..steps).map { i ->
            val f = i.toDouble() / steps
            val a = sin((1 - f) * d) / sin(d)
            val b = sin(f * d) / sin(d)
            val x = a * cos(p1) * cos(l1) + b * cos(p2) * cos(l2)
            val y = a * cos(p1) * sin(l1) + b * cos(p2) * sin(l2)
            val z = a * sin(p1) + b * sin(p2)
            Math.toDegrees(atan2(z, sqrt(x * x + y * y))) to Math.toDegrees(atan2(y, x))
        }
    }

    /** "ENE" style compass point for a bearing. */
    fun compassPoint(bearing: Double): String = POINTS[(((bearing % 360) + 360) % 360 / 22.5 + 0.5).toInt() % 16]

    private val POINTS = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
}

/** Land outlines for the Qibla map (Natural Earth 1:110m, public domain), as lng/lat rings. */
@Singleton
class WorldMap @Inject constructor(@ApplicationContext private val context: Context, @IoDispatcher private val io: CoroutineDispatcher) {
    private var rings: List<FloatArray>? = null
    suspend fun rings(): List<FloatArray> = rings ?: withContext(io) {
        val arr = JSONObject(context.assets.open("map/land.json").bufferedReader().readText()).getJSONArray("rings")
        (0 until arr.length()).map { i -> arr.getJSONArray(i).let { r -> FloatArray(r.length()) { r.getDouble(it).toFloat() } } }
    }.also { rings = it }
}

enum class Occasion { HIJRI_NEW_YEAR, ASHURA, RAMADAN_START, LAST_TEN_NIGHTS, EID_AL_FITR, DAY_OF_ARAFAH, EID_AL_ADHA, TASHREEQ, DHUL_HIJJAH_TEN }

enum class FastingNote { RECOMMENDED, FORBIDDEN, RAMADAN }

/**
 * Islamic occasions and fasting guidance by Hijri date. Only widely agreed occasions are listed.
 * Pure, so it is unit tested.
 */
object IslamicCalendar {
    fun occasions(h: HijriDate): List<Occasion> = buildList {
        if (h.month == 1 && h.day == 1) add(Occasion.HIJRI_NEW_YEAR)
        if (h.month == 1 && h.day == 10) add(Occasion.ASHURA)
        if (h.month == 9 && h.day == 1) add(Occasion.RAMADAN_START)
        if (h.month == 9 && h.day >= 21) add(Occasion.LAST_TEN_NIGHTS)
        if (h.month == 10 && h.day == 1) add(Occasion.EID_AL_FITR)
        if (h.month == 12 && h.day in 1..8) add(Occasion.DHUL_HIJJAH_TEN)
        if (h.month == 12 && h.day == 9) add(Occasion.DAY_OF_ARAFAH)
        if (h.month == 12 && h.day == 10) add(Occasion.EID_AL_ADHA)
        if (h.month == 12 && h.day in 11..13) add(Occasion.TASHREEQ)
    }

    fun fasting(h: HijriDate, gregorian: LocalDate): FastingNote? = when {
        h.month == 9 -> FastingNote.RAMADAN
        // No fasting on the two Eids and the days of Tashreeq.
        (h.month == 10 && h.day == 1) || (h.month == 12 && h.day in 10..13) -> FastingNote.FORBIDDEN
        h.month == 12 && h.day in 1..9 -> FastingNote.RECOMMENDED
        h.month == 1 && h.day in 9..10 -> FastingNote.RECOMMENDED
        h.month == 10 && h.day in 2..7 -> FastingNote.RECOMMENDED // six days of Shawwal (any six days of the month)
        h.day in 13..15 -> FastingNote.RECOMMENDED // the white days
        gregorian.dayOfWeek == java.time.DayOfWeek.MONDAY || gregorian.dayOfWeek == java.time.DayOfWeek.THURSDAY -> FastingNote.RECOMMENDED
        else -> null
    }
}
