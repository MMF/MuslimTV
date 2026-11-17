package com.mfouad.muslimtv.core.data.audio

import android.content.Context
import com.mfouad.muslimtv.core.model.AyahRef
import com.mfouad.muslimtv.core.model.Reciter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Where per-ayah audio comes from on the network. Swappable: EveryAyah today. */
interface AudioSource {
    fun remoteUrl(reciter: Reciter, ref: AyahRef): String
}

class EveryAyahAudioSource @Inject constructor() : AudioSource {
    override fun remoteUrl(reciter: Reciter, ref: AyahRef): String =
        "$BASE/${reciter.folder}/${fileName(ref)}"

    companion object {
        const val BASE = "https://everyayah.com/data"
        fun fileName(ref: AyahRef): String = "%03d%03d.mp3".format(ref.surah, ref.ayah)
    }
}

/** Downloaded audio on the device: files/audio/<reciter>/<SSSAAA>.mp3. Survives cache eviction. */
@Singleton
class LocalAudioStore @Inject constructor(@ApplicationContext context: Context) {
    val root: File = File(context.filesDir, "audio")

    fun reciterDir(reciterId: String) = File(root, reciterId)

    fun file(reciterId: String, ref: AyahRef) = File(reciterDir(reciterId), EveryAyahAudioSource.fileName(ref))

    fun isDownloaded(reciterId: String, ref: AyahRef): Boolean = file(reciterId, ref).let { it.isFile && it.length() > 0 }

    fun isSurahDownloaded(reciterId: String, surah: Int, ayahCount: Int): Boolean =
        (1..ayahCount).all { isDownloaded(reciterId, AyahRef(surah, it)) }

    fun bytesFor(reciterId: String): Long = reciterDir(reciterId).listFiles()?.sumOf { it.length() } ?: 0L
}

/** Picks the local file when downloaded, otherwise the network URL (which the player caches). */
class AyahAudioResolver @Inject constructor(
    private val source: AudioSource,
    private val local: LocalAudioStore,
) {
    fun uri(reciter: Reciter, ref: AyahRef): String {
        val file = local.file(reciter.id, ref)
        return if (file.isFile && file.length() > 0) file.toURI().toString() else source.remoteUrl(reciter, ref)
    }

    fun isLocal(reciter: Reciter, ref: AyahRef) = local.isDownloaded(reciter.id, ref)
}
