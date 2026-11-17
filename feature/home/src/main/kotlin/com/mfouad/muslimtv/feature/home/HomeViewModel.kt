package com.mfouad.muslimtv.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfouad.muslimtv.core.common.NetworkMonitor
import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.azkar.AzkarSuggester
import com.mfouad.muslimtv.core.data.prayer.PrayerSchedule
import com.mfouad.muslimtv.core.data.prayer.PrayerScheduleRepository
import com.mfouad.muslimtv.core.data.prayer.PrayerTimesCalculator
import com.mfouad.muslimtv.core.data.quran.PositionKind
import com.mfouad.muslimtv.core.data.quran.QuranPosition
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.model.AyahOfTheDay
import com.mfouad.muslimtv.core.model.AzkarCategory
import com.mfouad.muslimtv.core.model.NextPrayer
import com.mfouad.muslimtv.core.model.Surah
import com.mfouad.muslimtv.core.model.KhatmahPlan
import com.mfouad.muslimtv.core.model.KhatmahPortion
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.model.PrayerDay
import com.mfouad.muslimtv.core.model.RamadanMode
import com.mfouad.muslimtv.core.data.family.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class HomeUiState(
    val schedule: PrayerSchedule? = null,
    val next: NextPrayer? = null,
    val azkar: AzkarCategory = AzkarCategory.MORNING,
    val ayahOfTheDay: AyahOfTheDay? = null,
    val continueSurah: Surah? = null,
    val continueAyah: Int = 1,
    val hasReadingHistory: Boolean = false,
    val continueIsListening: Boolean = false,
    val isOnline: Boolean = true,
    val loaded: Boolean = false,
    val ramadan: Boolean = false,
    val khatmah: KhatmahPlan? = null,
    val khatmahPortion: KhatmahPortion? = null,
    val reminder: SurahReminder? = null,
)

/** Sunnah readings suggested on Home: Al-Kahf around Friday, Al-Mulk at night. */
enum class SurahReminder(val surah: Int) { KAHF(18), MULK(67) }

/** Pure: which reminder (if any) applies at [now]. Unit tested. */
object SurahReminders {
    fun at(now: Long, today: PrayerDay, dayOfWeek: java.time.DayOfWeek, kahf: Boolean, mulk: Boolean): SurahReminder? {
        val afterMaghrib = now >= today[Prayer.MAGHRIB]
        val kahfTime = (dayOfWeek == java.time.DayOfWeek.FRIDAY && !afterMaghrib) || (dayOfWeek == java.time.DayOfWeek.THURSDAY && afterMaghrib)
        return when {
            kahf && kahfTime -> SurahReminder.KAHF
            mulk && (now >= today[Prayer.ISHA] || now < today[Prayer.FAJR]) -> SurahReminder.MULK
            else -> null
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    scheduleRepository: PrayerScheduleRepository,
    private val quranRepo: QuranRepository,
    private val family: FamilyRepository,
    network: NetworkMonitor,
    time: TimeProvider,
) : ViewModel() {

    /** Ticks every second; read it only in the small composables that show seconds. */
    val now: StateFlow<Long> = time.ticker()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), time.nowMillis())

    private val minute = time.ticker(MINUTE_MS)

    private val ayah = scheduleRepository.schedule
        .map { it.date }
        .distinctUntilChanged()
        .mapLatest<_, AyahOfTheDay?> { quranRepo.ayahOfTheDay(it) }
        // Never hold back the whole screen for the ayah: prayer cards draw first.
        .onStart { emit(null) }

    /** The most recent of reading and listening, so "continue" resumes whatever the family did last. */
    private val lastPosition = combine(quranRepo.position(PositionKind.READ), quranRepo.position(PositionKind.LISTEN)) { r, l ->
        listOfNotNull(r?.let { it to false }, l?.let { it to true }).maxByOrNull { it.first.updatedAt }
    }.onStart { emit(null) }

    private val base = combine(
        scheduleRepository.schedule,
        minute,
        ayah,
        lastPosition,
        network.isOnline,
    ) { schedule, nowMillis, ayahOfDay, last, online ->
        val ref = last?.first?.ref
        HomeUiState(
            schedule = schedule,
            // Recomputed each minute; the countdown itself is derived from [now] in the UI.
            next = PrayerTimesCalculator.nextPrayer(schedule.today, schedule.tomorrow, nowMillis),
            azkar = AzkarSuggester.suggest(nowMillis, schedule.today),
            ayahOfTheDay = ayahOfDay,
            continueSurah = quranRepo.surah(ref?.surah ?: 1),
            continueAyah = ref?.ayah ?: 1,
            hasReadingHistory = last != null,
            continueIsListening = last?.second == true,
            isOnline = online,
            loaded = true,
        ) to nowMillis
    }

    val state: StateFlow<HomeUiState> = combine(base, family.khatmah) { (ui, nowMillis), plan ->
        val s = ui.schedule ?: return@combine ui
        val ramadan = when (s.settings.ramadanMode) {
            RamadanMode.ON -> true
            RamadanMode.OFF -> false
            RamadanMode.AUTO -> s.hijri.isRamadan
        }
        val maghrib = s.today[Prayer.MAGHRIB]
        val iftarTime = ramadan && nowMillis in (maghrib - IFTAR_WINDOW_MS)..(maghrib + IFTAR_WINDOW_MS)
        ui.copy(
            ramadan = ramadan,
            azkar = if (iftarTime) AzkarCategory.IFTAR else ui.azkar,
            khatmah = plan?.takeIf { !it.finished },
            khatmahPortion = plan?.takeIf { !it.finished }?.let { family.portion(it, it.completed + 1) },
            reminder = SurahReminders.at(nowMillis, s.today, s.date.dayOfWeek, s.settings.kahfReminder, s.settings.mulkReminder),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun surah(n: Int) = quranRepo.surah(n)

    private companion object {
        const val MINUTE_MS = 60_000L
        const val IFTAR_WINDOW_MS = 30 * 60_000L
    }
}
