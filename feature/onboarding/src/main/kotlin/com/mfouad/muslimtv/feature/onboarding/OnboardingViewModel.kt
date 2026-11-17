package com.mfouad.muslimtv.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfouad.muslimtv.core.data.city.CityRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import com.mfouad.muslimtv.core.model.CalculationMethod
import com.mfouad.muslimtv.core.model.City
import com.mfouad.muslimtv.core.model.PrayerDefaults
import com.mfouad.muslimtv.core.model.UserSettings
import com.mfouad.muslimtv.core.ui.AppLanguage
import com.mfouad.muslimtv.core.ui.CityPickerState
import dagger.hilt.android.lifecycle.HiltViewModel
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
class OnboardingViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val cities: CityRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val step: StateFlow<Int> = savedState.getStateFlow(KEY_STEP, 0)

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val _cityState = MutableStateFlow(CityPickerState(query = savedState[KEY_QUERY] ?: ""))
    val cityState: StateFlow<CityPickerState> = _cityState.asStateFlow()

    private val query = MutableStateFlow(_cityState.value.query)

    init {
        viewModelScope.launch {
            val suggestions = runCatching { cities.suggestions(TimeZone.getDefault().id) }.getOrDefault(emptyList())
            _cityState.update { it.copy(suggestions = suggestions, loading = false) }
        }
        observeQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() = viewModelScope.launch {
        query.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged().collect { q ->
            val results = if (q.isBlank()) emptyList() else runCatching { cities.search(q) }.getOrDefault(emptyList())
            _cityState.update { it.copy(results = results) }
        }
    }

    fun onQueryChange(text: String) {
        savedState[KEY_QUERY] = text
        _cityState.update { it.copy(query = text) }
        query.value = text
    }

    fun chooseLanguage(language: AppLanguage) {
        // Save the step first: switching language recreates the activity.
        savedState[KEY_STEP] = 1
        AppLanguage.set(language)
    }

    fun chooseCity(city: City) = viewModelScope.launch {
        settingsRepository.setCity(city)
        settingsRepository.setMethod(PrayerDefaults.methodFor(city.countryCode))
        settingsRepository.setMadhab(PrayerDefaults.madhabFor(city.countryCode))
        savedState[KEY_STEP] = 2
    }

    fun chooseMethod(method: CalculationMethod, onDone: () -> Unit) = viewModelScope.launch {
        settingsRepository.setMethod(method)
        settingsRepository.completeOnboarding()
        onDone()
    }

    /** Returns false when already on the first step, so the caller shows the exit dialog. */
    fun back(): Boolean {
        val current = step.value
        if (current == 0) return false
        savedState[KEY_STEP] = current - 1
        return true
    }

    private companion object {
        const val KEY_STEP = "step"
        const val KEY_QUERY = "query"
        const val SEARCH_DEBOUNCE_MS = 250L
        const val STEPS = 3
    }
}
