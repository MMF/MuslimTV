package com.mfouad.muslimtv.core.downloads

import android.content.Context
import android.os.StatFs
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.mfouad.muslimtv.core.common.IoDispatcher
import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.audio.AudioSource
import com.mfouad.muslimtv.core.data.audio.LocalAudioStore
import com.mfouad.muslimtv.core.data.audio.Reciters
import com.mfouad.muslimtv.core.data.quran.QuranImporter
import com.mfouad.muslimtv.core.data.quran.QuranMetadata
import com.mfouad.muslimtv.core.database.DownloadDao
import com.mfouad.muslimtv.core.database.DownloadEntity
import com.mfouad.muslimtv.core.database.QuranDao
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.Reciter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class StorageInfo(val availableBytes: Long, val totalBytes: Long)

data class ReciterUsage(val reciter: Reciter, val bytes: Long, val surahs: List<Int>)

/** Everything the download and storage screens need, behind one interface. */
@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DownloadDao,
    private val quranDao: QuranDao,
    private val importer: QuranImporter,
    private val metadata: QuranMetadata,
    private val local: LocalAudioStore,
    private val audio: AudioSource,
    private val http: OkHttpClient,
    private val time: TimeProvider,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val items: Flow<List<DownloadItem>> = dao.all().map { list -> list.map { it.toItem() } }

    private val lettersCache = ConcurrentHashMap<Int, Long>()
    private val bytesPerLetter = ConcurrentHashMap<String, Double>()

    // ---- Size and storage -------------------------------------------------------------------

    suspend fun lettersInSurah(surah: Int): Long = lettersCache[surah] ?: run {
        importer.ready()
        quranDao.surah(surah).sumOf { it.text.count(Char::isLetter).toLong() }.also { lettersCache[surah] = it }
    }

    /** Estimated bytes still to download for [surahs], measured from a few real files of this reciter. */
    suspend fun estimate(reciter: Reciter, surahs: List<Int>): Long = withContext(io) {
        val missing = surahs.filter { s -> !local.isSurahDownloaded(reciter.id, s, metadata.surah(s).ayahCount) }
        val letters = missing.sumOf { lettersInSurah(it) }
        val rate = bytesPerLetter[reciter.id] ?: calibrate(reciter)
        if (rate > 0) (letters * rate).toLong() else SizeEstimator.fromBitrate(letters, reciter.bitrateKbps)
    }

    private suspend fun calibrate(reciter: Reciter): Double = coroutineScope {
        val sample = (1..7).map { AyahRef(1, it) } + (1..4).map { AyahRef(112, it) }
        val sizes = sample.map { ref -> async(io) { contentLength(audio.remoteUrl(reciter, ref)) } }.awaitAll()
        if (sizes.any { it <= 0 }) return@coroutineScope 0.0
        val letters = sample.sumOf { ref -> quranDao.ayah(ref.surah, ref.ayah)?.text?.count(Char::isLetter)?.toLong() ?: 0L }
        (sizes.sum().toDouble() / letters).also { bytesPerLetter[reciter.id] = it }
    }

    private fun contentLength(url: String): Long = runCatching {
        http.newCall(Request.Builder().url(url).head().build()).execute().use { r -> if (r.isSuccessful) r.header("Content-Length")?.toLongOrNull() ?: -1 else -1 }
    }.getOrDefault(-1)

    fun storage(): StorageInfo {
        val stat = StatFs(context.filesDir.absolutePath)
        return StorageInfo(stat.availableBytes, stat.totalBytes)
    }

    suspend fun usage(): List<ReciterUsage> = withContext(io) {
        Reciters.all.mapNotNull { r ->
            val bytes = local.bytesFor(r.id)
            if (bytes <= 0) return@mapNotNull null
            val surahs = metadata.surahs.filter { local.isSurahDownloaded(r.id, it.number, it.ayahCount) }.map { it.number }
            ReciterUsage(r, bytes, surahs)
        }
    }

    // ---- Queue ----------------------------------------------------------------------------------

    suspend fun enqueueSurahs(reciter: Reciter, surahs: List<Int>, estimateBytes: Long) {
        val now = time.nowMillis()
        val perSurah = if (surahs.isEmpty()) 0 else estimateBytes / surahs.size
        val rows = surahs.filter { s -> !local.isSurahDownloaded(reciter.id, s, metadata.surah(s).ayahCount) }.mapIndexed { i, s ->
            val info = metadata.surah(s)
            DownloadEntity(
                id = DownloadItem.audioId(reciter.id, s),
                kind = DownloadKind.AUDIO.name,
                reciterId = reciter.id,
                surah = s,
                url = null,
                title = "${reciter.nameEn} · ${info.transliteration}",
                state = DownloadStatus.QUEUED.name,
                filesDone = 0,
                filesTotal = info.ayahCount,
                bytesDone = 0,
                bytesEstimate = perSurah,
                error = null,
                createdAt = now + i,
                updatedAt = now,
            )
        }
        dao.upsertAll(rows)
        startWorker()
    }

    suspend fun enqueueExtra(extra: ExtraContent) {
        val now = time.nowMillis()
        dao.upsert(
            DownloadEntity(
                id = DownloadItem.extraId(extra.kind, extra.id), kind = extra.kind.name, reciterId = null, surah = null,
                url = extra.url, title = extra.title, state = DownloadStatus.QUEUED.name, filesDone = 0, filesTotal = 1,
                bytesDone = 0, bytesEstimate = 0, error = null, createdAt = now, updatedAt = now,
            ),
        )
        startWorker()
    }

    suspend fun apply(id: String, event: DownloadEvent) {
        val current = dao.get(id)?.toItem() ?: return
        val next = DownloadStateMachine.reduce(current, event)
        if (next == null) {
            dao.delete(id)
            if (current.kind == DownloadKind.AUDIO && current.status != DownloadStatus.COMPLETED) {
                current.reciterId?.let { r -> current.surah?.let { deleteSurahFiles(r, it) } }
            }
        } else if (next != current) {
            dao.upsert(next.toEntity(time.nowMillis()))
        }
        if (event == DownloadEvent.Resume || event == DownloadEvent.Retry) startWorker()
    }

    suspend fun deleteSurah(reciterId: String, surah: Int) {
        dao.delete(DownloadItem.audioId(reciterId, surah))
        deleteSurahFiles(reciterId, surah)
    }

    suspend fun deleteReciter(reciterId: String) = withContext(io) {
        dao.deleteReciter(reciterId)
        local.reciterDir(reciterId).deleteRecursively()
    }

    private suspend fun deleteSurahFiles(reciterId: String, surah: Int) = withContext(io) {
        (1..metadata.surah(surah).ayahCount).forEach { local.file(reciterId, AyahRef(surah, it)).delete() }
    }

    /** Called at app start: picks up downloads interrupted by a reboot, update or force-stop. */
    suspend fun resumePending() {
        // Nothing runs in a freshly started process, so replacing the rescheduled job is safe and
        // starts it now instead of whenever JobScheduler gets to it.
        if (dao.next() != null) startWorker(ExistingWorkPolicy.REPLACE)
    }

    /** One worker processes the queue; KEEP means calling this again while it runs is harmless. */
    fun startWorker(policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
        val request = OneTimeWorkRequestBuilder<DownloadQueueWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(DownloadQueueWorker.NAME, policy, request)
    }

    internal suspend fun next(): DownloadItem? = dao.next()?.toItem()
    internal suspend fun save(item: DownloadItem) = dao.upsert(item.toEntity(time.nowMillis()))
    internal suspend fun current(id: String): DownloadItem? = dao.get(id)?.toItem()
}

internal fun DownloadEntity.toItem() = DownloadItem(
    id = id,
    kind = DownloadKind.valueOf(kind),
    title = title,
    status = DownloadStatus.valueOf(state),
    reciterId = reciterId,
    surah = surah,
    url = url,
    filesDone = filesDone,
    filesTotal = filesTotal,
    bytesDone = bytesDone,
    bytesEstimate = bytesEstimate,
    failReason = error?.let { runCatching { FailReason.valueOf(it) }.getOrNull() },
    createdAt = createdAt,
)

internal fun DownloadItem.toEntity(now: Long) = DownloadEntity(
    id = id, kind = kind.name, reciterId = reciterId, surah = surah, url = url, title = title,
    state = status.name, filesDone = filesDone, filesTotal = filesTotal, bytesDone = bytesDone,
    bytesEstimate = bytesEstimate, error = failReason?.name, createdAt = createdAt, updatedAt = now,
)
