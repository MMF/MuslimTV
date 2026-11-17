package com.mfouad.muslimtv.core.data.audio

import android.content.Context
import com.mfouad.muslimtv.core.common.IoDispatcher
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.WordSegment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Per-word timing inside each ayah's audio file. Null means "highlight the whole ayah". */
interface AyahTimingSource {
    suspend fun segments(reciterId: String, ref: AyahRef): List<WordSegment>?
}

/**
 * Reads timing files downloaded through the download manager:
 * files/timings/<reciter>.json = {"2:255": [[1, 0, 640], [2, 640, 1180], ...], ...}
 */
@Singleton
class FileTimingSource @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AyahTimingSource {
    val dir = File(context.filesDir, "timings")
    private val mutex = Mutex()
    private var loadedReciter: String? = null
    private var loaded: Map<String, List<WordSegment>> = emptyMap()

    fun file(reciterId: String) = File(dir, "$reciterId.json")

    override suspend fun segments(reciterId: String, ref: AyahRef): List<WordSegment>? = mutex.withLock {
        if (loadedReciter != reciterId) {
            loaded = withContext(io) { file(reciterId).takeIf { it.isFile }?.let { parse(it.readText()) } ?: emptyMap() }
            loadedReciter = reciterId
        }
        loaded["${ref.surah}:${ref.ayah}"]
    }

    companion object {
        fun parse(json: String): Map<String, List<WordSegment>> {
            val root = JSONObject(json)
            val out = HashMap<String, List<WordSegment>>(root.length())
            root.keys().forEach { key ->
                val arr = root.getJSONArray(key)
                out[key] = (0 until arr.length()).map { i ->
                    val s = arr.getJSONArray(i)
                    WordSegment(s.getInt(0), s.getLong(1), s.getLong(2))
                }
            }
            return out
        }
    }
}

/** Maps a playback position to the word being recited. Pure, so it is unit tested. */
object WordSync {
    /**
     * The word whose range contains [positionMs]. Between two words (a breath) it keeps the
     * previous word lit, so the highlight never flickers off mid-ayah. Before the first word: null.
     */
    fun wordAt(positionMs: Long, segments: List<WordSegment>): Int? {
        if (segments.isEmpty() || positionMs < segments.first().startMs) return null
        var lo = 0
        var hi = segments.lastIndex
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (segments[mid].startMs <= positionMs) lo = mid else hi = mid - 1
        }
        return segments[lo].word
    }
}
