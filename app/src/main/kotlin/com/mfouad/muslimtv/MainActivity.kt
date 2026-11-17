package com.mfouad.muslimtv

import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mfouad.muslimtv.core.designsystem.theme.MuslimTvTheme
import com.mfouad.muslimtv.ui.MuslimTvApp
import dagger.hilt.android.AndroidEntryPoint

/** The single activity. AppCompat gives per-app language switching back to API 23. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    /** Deep link from the adhan screen ("open the after-prayer azkar"), consumed by the app shell. */
    private val pendingOpen = MutableStateFlow<String?>(null)

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        viewModel.onUserActivity()
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_OPEN)?.let { pendingOpen.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingOpen.value = intent?.getStringExtra(EXTRA_OPEN)
        val arabic = ConfigurationCompat.getLocales(resources.configuration)[0]?.language == "ar"
        setContent {
            MuslimTvTheme(arabic = arabic) {
                val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
                val online by viewModel.online.collectAsStateWithLifecycle()
                val idle by viewModel.idle.collectAsStateWithLifecycle()
                val playback by viewModel.playback.collectAsStateWithLifecycle()
                onboardingDone?.let { done ->
                    MuslimTvApp(
                        onboardingDone = done,
                        online = online,
                        pendingOpen = pendingOpen,
                        idle = idle,
                        recitationActive = playback.isActive,
                        recitationPlaying = playback.isPlaying,
                        onTogglePlayback = viewModel::togglePlayback,
                        versionName = BuildConfig.VERSION_NAME,
                        onExit = ::finish,
                    )
                }
            }
        }
    }

    companion object {
        /** Must match AdhanActivity.EXTRA_OPEN. */
        const val EXTRA_OPEN = "open"
    }
}
