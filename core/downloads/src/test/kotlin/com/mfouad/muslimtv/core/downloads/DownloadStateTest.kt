package com.mfouad.muslimtv.core.downloads

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DownloadStateTest {
    private val queued = DownloadItem("audio:alafasy:2", DownloadKind.AUDIO, "Al-Baqara", DownloadStatus.QUEUED, filesTotal = 287)
    private fun DownloadItem.on(vararg events: DownloadEvent): DownloadItem? =
        events.fold(this as DownloadItem?) { acc, e -> acc?.let { DownloadStateMachine.reduce(it, e) } }

    @Test
    fun `happy path queued to completed`() {
        val done = queued.on(DownloadEvent.Start, DownloadEvent.Progress(100, 5_000_000), DownloadEvent.Complete)!!
        assertThat(done.status).isEqualTo(DownloadStatus.COMPLETED)
        assertThat(done.progress).isEqualTo(1f)
    }

    @Test
    fun `pause keeps progress and resume re-queues`() {
        val paused = queued.on(DownloadEvent.Start, DownloadEvent.Progress(40, 1), DownloadEvent.Pause)!!
        assertThat(paused.status).isEqualTo(DownloadStatus.PAUSED)
        assertThat(paused.filesDone).isEqualTo(40)
        // Progress reported after pausing (a late worker update) is ignored.
        assertThat(paused.on(DownloadEvent.Progress(41, 2))!!.filesDone).isEqualTo(40)
        assertThat(paused.on(DownloadEvent.Resume)!!.status).isEqualTo(DownloadStatus.QUEUED)
    }

    @Test
    fun `failure records the reason and retry clears it`() {
        val failed = queued.on(DownloadEvent.Start, DownloadEvent.Fail(FailReason.NETWORK))!!
        assertThat(failed.status).isEqualTo(DownloadStatus.FAILED)
        assertThat(failed.failReason).isEqualTo(FailReason.NETWORK)
        val retried = failed.on(DownloadEvent.Retry)!!
        assertThat(retried.status).isEqualTo(DownloadStatus.QUEUED)
        assertThat(retried.failReason).isNull()
    }

    @Test
    fun `a dropped connection re-queues without losing progress`() {
        val back = queued.on(DownloadEvent.Start, DownloadEvent.Progress(10, 1), DownloadEvent.Interrupted)!!
        assertThat(back.status).isEqualTo(DownloadStatus.QUEUED)
        assertThat(back.filesDone).isEqualTo(10)
        assertThat(back.on(DownloadEvent.Start)!!.status).isEqualTo(DownloadStatus.DOWNLOADING)
    }

    @Test
    fun `cancel removes the item from any state`() {
        assertThat(queued.on(DownloadEvent.Cancel)).isNull()
        assertThat(queued.on(DownloadEvent.Start, DownloadEvent.Pause, DownloadEvent.Cancel)).isNull()
    }

    @Test
    fun `invalid transitions are ignored`() {
        val done = queued.on(DownloadEvent.Start, DownloadEvent.Complete)!!
        assertThat(done.on(DownloadEvent.Pause)).isEqualTo(done)
        assertThat(done.on(DownloadEvent.Resume)).isEqualTo(done)
        assertThat(queued.on(DownloadEvent.Complete)).isEqualTo(queued)
        assertThat(queued.on(DownloadEvent.Retry)).isEqualTo(queued)
    }

    @Test
    fun `progress is clamped to the file count`() {
        assertThat(queued.on(DownloadEvent.Start, DownloadEvent.Progress(999, 1))!!.filesDone).isEqualTo(287)
    }

    @Test
    fun `size estimate scales with letters`() {
        assertThat(SizeEstimator.estimate(letters = 1000, sampleBytes = 200_000, sampleLetters = 100)).isEqualTo(2_000_000)
        assertThat(SizeEstimator.estimate(1000, 0, 100)).isEqualTo(0)
        assertThat(SizeEstimator.fromBitrate(letters = 1000, kbps = 128)).isEqualTo(1_760_000)
    }

    @Test
    fun `storage guard warns before the device nearly fills`() {
        val gb = 1024L * 1024 * 1024
        assertThat(StorageGuard.check(1 * gb, availableBytes = 6 * gb, totalBytes = 16 * gb)).isEqualTo(StorageVerdict.OK)
        assertThat(StorageGuard.check(5 * gb, availableBytes = 6 * gb, totalBytes = 16 * gb)).isEqualTo(StorageVerdict.LOW_AFTER)
        assertThat(StorageGuard.check(7 * gb, availableBytes = 6 * gb, totalBytes = 16 * gb)).isEqualTo(StorageVerdict.NOT_ENOUGH)
    }
}
