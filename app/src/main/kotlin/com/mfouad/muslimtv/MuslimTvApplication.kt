package com.mfouad.muslimtv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mfouad.muslimtv.core.common.ApplicationScope
import com.mfouad.muslimtv.core.data.quran.QuranImporter
import com.mfouad.muslimtv.core.downloads.DownloadRepository
import com.mfouad.muslimtv.feature.prayer.alarm.AdhanScheduler
import com.mfouad.muslimtv.feature.prayer.alarm.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MuslimTvApplication : Application(), Configuration.Provider {
    @Inject lateinit var quranImporter: QuranImporter
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var downloads: DownloadRepository
    @Inject lateinit var adhanScheduler: AdhanScheduler
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // First launch copies the bundled Quran text into Room in the background (about a second);
        // it is a no-op afterwards.
        quranImporter.start()
        appScope.launch { downloads.resumePending() }
        adhanScheduler.start()
        reminderScheduler.start()
    }
}
