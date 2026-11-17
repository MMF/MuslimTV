package com.mfouad.muslimtv.feature.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.prayer.PrayerSchedule
import com.mfouad.muslimtv.core.data.prayer.PrayerScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PrayerTodayViewModel @Inject constructor(
    repository: PrayerScheduleRepository,
    time: TimeProvider,
) : ViewModel() {
    val schedule: StateFlow<PrayerSchedule?> = repository.schedule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val now: StateFlow<Long> = time.ticker()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), time.nowMillis())
}
