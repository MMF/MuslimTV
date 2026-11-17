package com.mfouad.muslimtv.core.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.mfouad.muslimtv.core.data.audio.AudioSource
import com.mfouad.muslimtv.core.data.audio.FileTimingSource
import com.mfouad.muslimtv.core.data.audio.LocalAudioStore
import com.mfouad.muslimtv.core.data.audio.Reciters
import com.mfouad.muslimtv.core.data.quran.QuranMetadata
import com.mfouad.muslimtv.core.database.AyahNoteEntity
import com.mfouad.muslimtv.core.database.QuranDao
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.QuranText
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Works through the download queue stored in Room, one item at a time. WorkManager restarts it
 * after reboots or process death, and it picks up where it stopped (finished files are skipped).
 * Pause and cancel are just state changes in the table; the worker notices before each file.
 */
@HiltWorker
class DownloadQueueWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: DownloadRepository,
    private val local: LocalAudioStore,
    private val audio: AudioSource,
    private val timings: FileTimingSource,
    private val metadata: QuranMetadata,
    private val quranDao: QuranDao,
    private val http: OkHttpClient,
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo = foreground(applicationContext.getString(R.string.dl_notification))

    override suspend fun doWork(): Result {
        runCatching { setForeground(foreground(applicationContext.getString(R.string.dl_notification))) }
        while (true) {
            if (isStopped) return Result.retry()
            val item = repo.next() ?: return Result.success()
            val running = DownloadStateMachine.reduce(item, DownloadEvent.Start) ?: continue
            repo.save(running)
            val result = runCatching {
                when (running.kind) {
                    DownloadKind.AUDIO -> downloadSurah(running)
                    DownloadKind.TRANSLATION -> downloadTranslation(running)
                    DownloadKind.TIMING -> downloadFile(running, File(timings.dir, "${running.id.substringAfterLast(':')}.json"))
                    DownloadKind.SCENE -> downloadFile(running, File(applicationContext.filesDir, "scenes/${running.id.substringAfterLast(':')}.webp"))
                }
            }
            val latest = repo.current(running.id) ?: continue // cancelled meanwhile
            if (latest.status != DownloadStatus.DOWNLOADING) continue // paused meanwhile
            val failure = result.exceptionOrNull()
            val event = when {
                failure == null -> if (result.getOrDefault(false)) DownloadEvent.Complete else continue
                reasonOf(failure) == FailReason.NETWORK -> DownloadEvent.Interrupted
                else -> DownloadEvent.Fail(reasonOf(failure))
            }
            DownloadStateMachine.reduce(latest, event)?.let { repo.save(it) }
            // Lost connection: WorkManager runs us again once the network is back.
            if (event == DownloadEvent.Interrupted) return Result.retry()
        }
    }

    /** Returns true when every file is present, false if it stopped early (paused or cancelled). */
    private suspend fun downloadSurah(item: DownloadItem): Boolean {
        val reciter = Reciters.byId(item.reciterId ?: return true)
        val surah = item.surah ?: return true
        val count = metadata.surah(surah).ayahCount
        // The basmala before most surahs plays Al-Fatiha 1:1, so it is needed offline too.
        val refs = (1..count).map { AyahRef(surah, it) } +
            if (QuranText.hasBasmalaHeader(surah)) listOf(AyahRef(1, 1)) else emptyList()
        var bytes = 0L
        refs.forEachIndexed { index, ref ->
            if (isStopped) return false
            val stillWanted = repo.current(item.id)?.status == DownloadStatus.DOWNLOADING
            if (!stillWanted) return false
            val target = local.file(reciter.id, ref)
            if (!(target.isFile && target.length() > 0)) fetch(audio.remoteUrl(reciter, ref), target)
            bytes += target.length()
            if (index < count) {
                repo.current(item.id)?.let { cur ->
                    DownloadStateMachine.reduce(cur, DownloadEvent.Progress(index + 1, bytes))?.let { repo.save(it) }
                }
            }
        }
        return true
    }

    private suspend fun downloadTranslation(item: DownloadItem): Boolean {
        val source = item.id.substringAfterLast(':')
        val body = withContext(Dispatchers.IO) {
            http.newCall(Request.Builder().url(item.url!!).build()).execute().use { r ->
                if (!r.isSuccessful) throw HttpError(r.code)
                r.body!!.string()
            }
        }
        val rows = body.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { line ->
            val p = line.split('|', limit = 3)
            if (p.size < 3) null else AyahNoteEntity(source, p[0].toInt(), p[1].toInt(), p[2])
        }.toList()
        if (rows.size < AYAH_COUNT) throw HttpError(422)
        rows.chunked(500).forEach { quranDao.insertNotes(it) }
        return true
    }

    private suspend fun downloadFile(item: DownloadItem, target: File): Boolean {
        fetch(item.url!!, target)
        return true
    }

    private suspend fun fetch(url: String, target: File) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".part")
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) throw HttpError(r.code)
            r.body!!.byteStream().use { input -> temp.outputStream().use { input.copyTo(it) } }
        }
        if (!temp.renameTo(target)) throw IOException("rename failed")
    }

    private fun reasonOf(t: Throwable?): FailReason = when {
        t is HttpError && t.code == 404 -> FailReason.NOT_FOUND
        t is HttpError -> FailReason.NETWORK
        t?.message?.contains("ENOSPC") == true || t?.message?.contains("No space") == true -> FailReason.STORAGE_FULL
        t is IOException -> FailReason.NETWORK
        else -> FailReason.UNKNOWN
    }

    private fun foreground(text: String): ForegroundInfo {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, applicationContext.getString(R.string.dl_channel), NotificationManager.IMPORTANCE_LOW))
        }
        val n = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(text)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, n)
        }
    }

    private class HttpError(val code: Int) : IOException("HTTP $code")

    companion object {
        const val NAME = "download-queue"
        private const val CHANNEL = "downloads"
        private const val NOTIFICATION_ID = 7001
        private const val AYAH_COUNT = 6236
    }
}
