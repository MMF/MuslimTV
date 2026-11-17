package com.mfouad.muslimtv.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfouad.muslimtv.core.common.NetworkMonitor
import com.mfouad.muslimtv.core.data.audio.LocalAudioStore
import com.mfouad.muslimtv.core.data.audio.Reciters
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.downloads.DownloadEvent
import com.mfouad.muslimtv.core.downloads.DownloadItem
import com.mfouad.muslimtv.core.downloads.DownloadRepository
import com.mfouad.muslimtv.core.downloads.DownloadStatus
import com.mfouad.muslimtv.core.downloads.ExtraContent
import com.mfouad.muslimtv.core.downloads.ExtrasCatalog
import com.mfouad.muslimtv.core.downloads.ReciterUsage
import com.mfouad.muslimtv.core.downloads.StorageGuard
import com.mfouad.muslimtv.core.downloads.StorageInfo
import com.mfouad.muslimtv.core.downloads.StorageVerdict
import com.mfouad.muslimtv.core.model.Reciter
import com.mfouad.muslimtv.core.model.Surah
import com.mfouad.muslimtv.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A pending download the user is confirming: what, how big, and whether it fits. */
data class DownloadOffer(
    val title: String,
    val reciter: Reciter,
    val surahs: List<Int>,
    val bytes: Long? = null,
    val storage: StorageInfo? = null,
    val verdict: StorageVerdict? = null,
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repo: DownloadRepository,
    private val quran: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val local: LocalAudioStore,
    network: NetworkMonitor,
) : ViewModel() {
    val items: StateFlow<List<DownloadItem>> = repo.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val online: StateFlow<Boolean> = network.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val settings: StateFlow<UserSettings> = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val _usage = MutableStateFlow<List<ReciterUsage>>(emptyList())
    val usage: StateFlow<List<ReciterUsage>> = _usage.asStateFlow()

    private val _storage = MutableStateFlow(repo.storage())
    val storage: StateFlow<StorageInfo> = _storage.asStateFlow()

    private val _offer = MutableStateFlow<DownloadOffer?>(null)
    val offer: StateFlow<DownloadOffer?> = _offer.asStateFlow()

    private val _translations = MutableStateFlow<List<String>>(emptyList())
    val installedTranslations: StateFlow<List<String>> = _translations.asStateFlow()

    private var offerJob: Job? = null

    val surahs: List<Surah> get() = quran.surahs
    val juzs get() = quran.juzs

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _usage.value = repo.usage()
        _storage.value = repo.storage()
        _translations.value = quran.availableTranslations()
    }

    fun isDownloaded(reciter: Reciter, surah: Surah) = local.isSurahDownloaded(reciter.id, surah.number, surah.ayahCount)

    suspend fun currentReciter(): Reciter = Reciters.byId(settingsRepository.settings.first().reciterId)

    /** Shows the confirm dialog at once, then fills in the measured size and storage verdict. */
    fun offer(title: String, reciter: Reciter, surahs: List<Int>) {
        offerJob?.cancel()
        _offer.value = DownloadOffer(title, reciter, surahs)
        offerJob = viewModelScope.launch {
            val bytes = repo.estimate(reciter, surahs)
            val storage = repo.storage()
            _offer.value = _offer.value?.copy(bytes = bytes, storage = storage, verdict = StorageGuard.check(bytes, storage.availableBytes, storage.totalBytes))
        }
    }

    fun dismissOffer() { offerJob?.cancel(); _offer.value = null }

    fun confirmOffer() = viewModelScope.launch {
        val o = _offer.value ?: return@launch
        _offer.value = null
        repo.enqueueSurahs(o.reciter, o.surahs, o.bytes ?: 0)
    }

    fun pause(item: DownloadItem) = event(item, DownloadEvent.Pause)
    fun resume(item: DownloadItem) = event(item, DownloadEvent.Resume)
    fun retry(item: DownloadItem) = event(item, DownloadEvent.Retry)
    fun cancel(item: DownloadItem) = event(item, DownloadEvent.Cancel)

    private fun event(item: DownloadItem, e: DownloadEvent) = viewModelScope.launch {
        repo.apply(item.id, e)
        if (item.status == DownloadStatus.COMPLETED || e == DownloadEvent.Cancel) refresh()
    }

    fun deleteSurah(reciterId: String, surah: Int) = viewModelScope.launch { repo.deleteSurah(reciterId, surah); refresh() }
    fun deleteReciter(reciterId: String) = viewModelScope.launch { repo.deleteReciter(reciterId); refresh() }

    fun downloadExtra(extra: ExtraContent) = viewModelScope.launch { repo.enqueueExtra(extra) }
    fun useTranslation(id: String) = viewModelScope.launch {
        settingsRepository.setTranslation(id)
        settingsRepository.setShowTranslation(true)
    }

    val extras = ExtrasCatalog.translations
}
