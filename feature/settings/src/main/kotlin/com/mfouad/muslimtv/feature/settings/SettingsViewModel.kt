package com.mfouad.muslimtv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfouad.muslimtv.core.data.city.CityRepository
import com.mfouad.muslimtv.core.data.prayer.PrayerScheduleRepository
import com.mfouad.muslimtv.core.data.quran.AyahOfTheDaySelector
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.model.AsrMadhab
import com.mfouad.muslimtv.core.model.Ayah
import com.mfouad.muslimtv.core.model.CalculationMethod
import com.mfouad.muslimtv.core.model.City
import com.mfouad.muslimtv.core.model.HighLatitudeOption
import com.mfouad.muslimtv.core.model.HijriDate
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.model.TimeFormat
import com.mfouad.muslimtv.core.model.UserSettings
import com.mfouad.muslimtv.core.ui.CityPickerState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val cities: CityRepository,
    private val schedule: PrayerScheduleRepository,
    private val quran: QuranRepository,
    private val family: com.mfouad.muslimtv.core.data.family.FamilyRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _cityState = MutableStateFlow(CityPickerState())
    val cityState: StateFlow<CityPickerState> = _cityState.asStateFlow()
    private val query = MutableStateFlow("")

    private val _sampleAyah = MutableStateFlow<Ayah?>(null)
    val sampleAyah: StateFlow<Ayah?> = _sampleAyah.asStateFlow()

    fun loadCities() {
        if (_cityState.value.suggestions.isNotEmpty()) return
        viewModelScope.launch {
            val s = runCatching { cities.suggestions(TimeZone.getDefault().id) }.getOrDefault(emptyList())
            _cityState.update { it.copy(suggestions = s, loading = false) }
        }
        observeQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() = viewModelScope.launch {
        query.debounce(250).distinctUntilChanged().collect { q ->
            val r = if (q.isBlank()) emptyList() else runCatching { cities.search(q) }.getOrDefault(emptyList())
            _cityState.update { it.copy(results = r) }
        }
    }

    fun onQueryChange(text: String) {
        _cityState.update { it.copy(query = text) }
        query.value = text
    }

    fun loadSampleAyah() = viewModelScope.launch {
        if (_sampleAyah.value == null) _sampleAyah.value = quran.ayah(AyahOfTheDaySelector.refs.first())
    }

    /** The Hijri date today would show with a given offset, as a live preview. */
    fun hijriPreview(offset: Int): HijriDate {
        val zone = settings.value?.city?.timeZone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        return schedule.hijriFor(LocalDate.now(zone), offset)
    }

    fun setCity(city: City) = launch { repository.setCity(city) }
    fun setMethod(m: CalculationMethod) = launch { repository.setMethod(m) }
    fun setMadhab(m: AsrMadhab) = launch { repository.setMadhab(m) }
    fun setHighLatitude(o: HighLatitudeOption) = launch { repository.setHighLatitude(o) }
    fun setAdjustment(p: Prayer, minutes: Int) = launch { repository.setAdjustment(p, minutes) }
    fun setHijriOffset(days: Int) = launch { repository.setHijriOffset(days) }
    fun setQuranFontSize(sp: Int) = launch { repository.setQuranFontSize(sp) }
    fun setTimeFormat(f: TimeFormat) = launch { repository.setTimeFormat(f) }
    fun setAmbientTimeout(m: Int) = launch { repository.setAmbientTimeout(m) }
    fun setRamadanMode(m: com.mfouad.muslimtv.core.model.RamadanMode) = launch { repository.setRamadanMode(m) }
    fun setKahf(on: Boolean) = launch { repository.setKahfReminder(on) }
    fun setMulk(on: Boolean) = launch { repository.setMulkReminder(on) }

    val profileName: StateFlow<String?> = kotlinx.coroutines.flow.combine(family.all, family.activeId) { all, id -> all.firstOrNull { it.id == id }?.takeIf { it.id != 1L }?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}
