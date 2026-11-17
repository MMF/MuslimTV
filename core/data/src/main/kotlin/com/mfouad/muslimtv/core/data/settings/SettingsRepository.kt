package com.mfouad.muslimtv.core.data.settings

import com.mfouad.muslimtv.core.datastore.SettingsDataSource
import com.mfouad.muslimtv.core.model.AdhanSettings
import com.mfouad.muslimtv.core.model.AlertMode
import com.mfouad.muslimtv.core.model.AmbientSound
import com.mfouad.muslimtv.core.model.AmbientVolume
import com.mfouad.muslimtv.core.model.AsrMadhab
import com.mfouad.muslimtv.core.model.CalculationMethod
import com.mfouad.muslimtv.core.model.City
import com.mfouad.muslimtv.core.model.HighLatitudeOption
import com.mfouad.muslimtv.core.model.Prayer
import com.mfouad.muslimtv.core.model.RamadanMode
import com.mfouad.muslimtv.core.model.TimeFormat
import com.mfouad.muslimtv.core.model.UserSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    val adhan: Flow<AdhanSettings>
    suspend fun setAlertMode(prayer: Prayer, mode: AlertMode)
    suspend fun setIqamah(prayer: Prayer, minutes: Int)
    suspend fun setMuezzin(id: String)
    suspend fun setStartOnBoot(on: Boolean)
    suspend fun setCity(city: City)
    suspend fun setMethod(method: CalculationMethod)
    suspend fun setMadhab(madhab: AsrMadhab)
    suspend fun setHighLatitude(option: HighLatitudeOption)
    suspend fun setAdjustment(prayer: Prayer, minutes: Int)
    suspend fun setHijriOffset(days: Int)
    suspend fun setQuranFontSize(sp: Int)
    suspend fun setTimeFormat(format: TimeFormat)
    suspend fun setReciter(id: String)
    suspend fun setShowTranslation(show: Boolean)
    suspend fun setTranslation(id: String)
    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun setNatureScene(index: Int)
    suspend fun setAmbientSound(sound: AmbientSound)
    suspend fun setAmbientVolume(v: AmbientVolume)
    suspend fun setAmbientTimeout(minutes: Int)
    suspend fun setRamadanMode(m: RamadanMode)
    suspend fun setKahfReminder(on: Boolean)
    suspend fun setMulkReminder(on: Boolean)
    suspend fun completeOnboarding()
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val source: SettingsDataSource,
) : SettingsRepository {
    override val settings: Flow<UserSettings> = source.settings
    override val adhan: Flow<AdhanSettings> = source.adhanSettings
    override suspend fun setAlertMode(prayer: Prayer, mode: AlertMode) { source.setAlertMode(prayer, mode) }
    override suspend fun setIqamah(prayer: Prayer, minutes: Int) { source.setIqamah(prayer, minutes) }
    override suspend fun setMuezzin(id: String) { source.setMuezzin(id) }
    override suspend fun setStartOnBoot(on: Boolean) { source.setStartOnBoot(on) }
    override suspend fun setCity(city: City) { source.setCity(city) }
    override suspend fun setMethod(method: CalculationMethod) { source.setMethod(method) }
    override suspend fun setMadhab(madhab: AsrMadhab) { source.setMadhab(madhab) }
    override suspend fun setHighLatitude(option: HighLatitudeOption) { source.setHighLatitude(option) }
    override suspend fun setAdjustment(prayer: Prayer, minutes: Int) { source.setAdjustment(prayer, minutes) }
    override suspend fun setHijriOffset(days: Int) { source.setHijriOffset(days) }
    override suspend fun setQuranFontSize(sp: Int) { source.setQuranFontSize(sp) }
    override suspend fun setTimeFormat(format: TimeFormat) { source.setTimeFormat(format) }
    override suspend fun setReciter(id: String) { source.setReciter(id) }
    override suspend fun setShowTranslation(show: Boolean) { source.setShowTranslation(show) }
    override suspend fun setTranslation(id: String) { source.setTranslation(id) }
    override suspend fun setPlaybackSpeed(speed: Float) { source.setPlaybackSpeed(speed) }
    override suspend fun setNatureScene(index: Int) { source.setNatureScene(index) }
    override suspend fun setAmbientSound(sound: AmbientSound) { source.setAmbientSound(sound) }
    override suspend fun setAmbientVolume(v: AmbientVolume) { source.setAmbientVolume(v) }
    override suspend fun setAmbientTimeout(minutes: Int) { source.setAmbientTimeout(minutes) }
    override suspend fun setRamadanMode(m: RamadanMode) { source.setRamadanMode(m) }
    override suspend fun setKahfReminder(on: Boolean) { source.setKahfReminder(on) }
    override suspend fun setMulkReminder(on: Boolean) { source.setMulkReminder(on) }
    override suspend fun completeOnboarding() { source.setOnboardingDone(true) }
}
