package com.mfouad.muslimtv.feature.prayer

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfouad.muslimtv.core.data.prayer.PrayerSchedule
import com.mfouad.muslimtv.core.data.prayer.PrayerScheduleRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.model.AdhanSettings
import com.mfouad.muslimtv.core.model.AlertMode
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.feature.prayer.alarm.AdhanScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PrayerExtrasViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedState: SavedStateHandle,
    private val settings: SettingsRepository,
    private val schedules: PrayerScheduleRepository,
    private val scheduler: AdhanScheduler,
) : ViewModel() {
    val adhan: StateFlow<AdhanSettings> = settings.adhan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdhanSettings())

    private val _month = MutableStateFlow<List<PrayerSchedule>>(emptyList())
    val month: StateFlow<List<PrayerSchedule>> = _month.asStateFlow()

    val monthOffset: StateFlow<Int> = savedState.getStateFlow(KEY_MONTH, 0)

    private val _previewing = MutableStateFlow(false)
    val previewing: StateFlow<Boolean> = _previewing.asStateFlow()
    private var preview: MediaPlayer? = null

    val exactAlarmsAllowed: Boolean get() = scheduler.canScheduleExact()

    init { loadMonth() }

    fun shiftMonth(delta: Int) {
        savedState[KEY_MONTH] = (monthOffset.value + delta).coerceIn(-12, 12)
        loadMonth()
    }

    private fun loadMonth() = viewModelScope.launch {
        val s = settings.settings.first()
        val city = s.city ?: return@launch
        val zone = runCatching { ZoneId.of(city.timeZone) }.getOrDefault(ZoneId.systemDefault())
        val ym = YearMonth.from(LocalDate.now(zone)).plusMonths(monthOffset.value.toLong())
        _month.value = (1..ym.lengthOfMonth()).map { schedules.build(s, city, ym.atDay(it)) }
    }

    fun cycleMode(p: Prayer) = viewModelScope.launch {
        val next = AlertMode.entries[(adhan.value.mode(p).ordinal + 1) % AlertMode.entries.size]
        settings.setAlertMode(p, next)
    }

    fun changeIqamah(p: Prayer, delta: Int) = viewModelScope.launch { settings.setIqamah(p, adhan.value.iqamah(p) + delta) }
    fun setMuezzin(id: String) = viewModelScope.launch { settings.setMuezzin(id) }
    private val _testScheduled = MutableStateFlow(false)
    val testScheduled: StateFlow<Boolean> = _testScheduled.asStateFlow()

    fun testAdhan() {
        scheduler.scheduleTest(Prayer.DHUHR)
        _testScheduled.value = true
    }

    private val _overlayAllowed = MutableStateFlow(overlayAllowed())
    val overlay: StateFlow<Boolean> = _overlayAllowed.asStateFlow()

    private fun overlayAllowed() = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context)

    fun refreshOverlay() { _overlayAllowed.value = overlayAllowed() }

    /** Opens the system screen to allow "display over other apps"; false if this TV has none. */
    fun requestOverlay(): Boolean = runCatching {
        context.startActivity(
            android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + context.packageName))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.isSuccess

    fun toggleStartOnBoot() = viewModelScope.launch { settings.setStartOnBoot(!adhan.value.startOnBoot) }

    fun togglePreview(id: String) {
        if (preview != null) { stopPreview(); return }
        val res = if (id == "short") R.raw.adhan_short else R.raw.adhan_full
        preview = MediaPlayer.create(context, res)?.apply {
            setOnCompletionListener { stopPreview() }
            start()
        }
        _previewing.value = preview != null
    }

    private fun stopPreview() {
        preview?.run { runCatching { stop() }; release() }
        preview = null
        _previewing.value = false
    }

    override fun onCleared() = stopPreview()

    private companion object {
        const val KEY_MONTH = "month"
    }
}
