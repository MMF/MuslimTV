package com.mfouad.muslimtv.core.downloads

enum class DownloadKind { AUDIO, TRANSLATION, TIMING, SCENE }

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }

enum class FailReason { NETWORK, STORAGE_FULL, NOT_FOUND, UNKNOWN }

data class DownloadItem(
    val id: String,
    val kind: DownloadKind,
    val title: String,
    val status: DownloadStatus,
    val reciterId: String? = null,
    val surah: Int? = null,
    val url: String? = null,
    val filesDone: Int = 0,
    val filesTotal: Int = 1,
    val bytesDone: Long = 0,
    val bytesEstimate: Long = 0,
    val failReason: FailReason? = null,
    val createdAt: Long = 0,
) {
    val progress: Float get() = if (filesTotal <= 0) 0f else (filesDone.toFloat() / filesTotal).coerceIn(0f, 1f)
    val isActive: Boolean get() = status == DownloadStatus.QUEUED || status == DownloadStatus.DOWNLOADING

    companion object {
        fun audioId(reciterId: String, surah: Int) = "audio:$reciterId:$surah"
        fun extraId(kind: DownloadKind, id: String) = "extra:${kind.name.lowercase()}:$id"
    }
}

sealed interface DownloadEvent {
    data object Start : DownloadEvent
    data class Progress(val filesDone: Int, val bytesDone: Long) : DownloadEvent
    data object Complete : DownloadEvent
    data class Fail(val reason: FailReason) : DownloadEvent
    data object Pause : DownloadEvent
    data object Resume : DownloadEvent
    data object Retry : DownloadEvent
    data object Cancel : DownloadEvent

    /** Connection lost mid-download: back to the queue, resumed automatically when online. */
    data object Interrupted : DownloadEvent
}

/**
 * All download state changes go through here. Pure, so it is unit tested.
 * Returns null when the item should be removed (cancel). Invalid events leave the item unchanged.
 */
object DownloadStateMachine {
    fun reduce(item: DownloadItem, event: DownloadEvent): DownloadItem? = when (event) {
        DownloadEvent.Start -> if (item.status == DownloadStatus.QUEUED || item.status == DownloadStatus.DOWNLOADING) {
            item.copy(status = DownloadStatus.DOWNLOADING, failReason = null)
        } else item
        is DownloadEvent.Progress -> if (item.status == DownloadStatus.DOWNLOADING) {
            item.copy(filesDone = event.filesDone.coerceIn(0, item.filesTotal), bytesDone = event.bytesDone)
        } else item
        DownloadEvent.Complete -> if (item.status == DownloadStatus.DOWNLOADING) {
            item.copy(status = DownloadStatus.COMPLETED, filesDone = item.filesTotal, failReason = null)
        } else item
        is DownloadEvent.Fail -> if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.QUEUED) {
            item.copy(status = DownloadStatus.FAILED, failReason = event.reason)
        } else item
        DownloadEvent.Pause -> if (item.isActive) item.copy(status = DownloadStatus.PAUSED) else item
        DownloadEvent.Resume -> if (item.status == DownloadStatus.PAUSED) item.copy(status = DownloadStatus.QUEUED) else item
        DownloadEvent.Retry -> if (item.status == DownloadStatus.FAILED) item.copy(status = DownloadStatus.QUEUED, failReason = null) else item
        DownloadEvent.Cancel -> null
        DownloadEvent.Interrupted -> if (item.status == DownloadStatus.DOWNLOADING) item.copy(status = DownloadStatus.QUEUED) else item
    }
}

/** Size estimates before downloading, without fetching every file. Pure, so it is unit tested. */
object SizeEstimator {
    /**
     * Recitation length tracks text length closely, so bytes ≈ letters × bytes-per-letter, where
     * bytes-per-letter is measured from a few real files of the same reciter.
     */
    fun estimate(letters: Long, sampleBytes: Long, sampleLetters: Long): Long {
        if (sampleLetters <= 0 || sampleBytes <= 0) return 0
        return (letters.toDouble() * sampleBytes / sampleLetters).toLong()
    }

    /** Fallback when the sample cannot be measured: kbps × an average of 0.11 s of audio per letter. */
    fun fromBitrate(letters: Long, kbps: Int): Long = (letters * SECONDS_PER_LETTER * kbps * 1000 / 8).toLong()

    private const val SECONDS_PER_LETTER = 0.11
}

enum class StorageVerdict { OK, LOW_AFTER, NOT_ENOUGH }

/** TV storage is small: warn before a download leaves less than 10% (min 500 MB) free. */
object StorageGuard {
    const val MIN_FREE_BYTES = 500L * 1024 * 1024

    fun check(downloadBytes: Long, availableBytes: Long, totalBytes: Long): StorageVerdict {
        val reserve = maxOf(MIN_FREE_BYTES, totalBytes / 10)
        return when {
            downloadBytes >= availableBytes -> StorageVerdict.NOT_ENOUGH
            availableBytes - downloadBytes < reserve -> StorageVerdict.LOW_AFTER
            else -> StorageVerdict.OK
        }
    }
}
