package com.mfouad.muslimtv.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
import com.mfouad.muslimtv.core.model.PrayerAdjustments
import com.mfouad.muslimtv.core.model.PrayerSettings
import com.mfouad.muslimtv.core.model.TimeFormat
import com.mfouad.muslimtv.core.model.UserSettings
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Device-wide settings. Per-profile data (bookmarks, khatmah) lives in Room from Phase 2/7. */
@Singleton
class SettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<UserSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toUserSettings() }

    val adhanSettings: Flow<AdhanSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val defaults = AdhanSettings()
            AdhanSettings(
                modes = Prayer.entries.filter { it.isObligatory }.associateWith { pr ->
                    enumOrDefault(p[Keys.alertMode(pr)], defaults.mode(pr))
                },
                muezzinId = p[Keys.muezzin] ?: defaults.muezzinId,
                iqamahMinutes = Prayer.entries.filter { it.isObligatory }.associateWith { pr -> p[Keys.iqamah(pr)] ?: defaults.iqamah(pr) },
                startOnBoot = p[Keys.startOnBoot] ?: false,
            )
        }

    suspend fun setAlertMode(prayer: Prayer, mode: AlertMode): Unit = edit { it[Keys.alertMode(prayer)] = mode.name }
    suspend fun setIqamah(prayer: Prayer, minutes: Int): Unit = edit { it[Keys.iqamah(prayer)] = minutes.coerceIn(0, 60) }
    suspend fun setMuezzin(id: String): Unit = edit { it[Keys.muezzin] = id }
    suspend fun setStartOnBoot(on: Boolean): Unit = edit { it[Keys.startOnBoot] = on }

    val activeProfileId: Flow<Long> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.activeProfile] ?: 1L }

    suspend fun setActiveProfile(id: Long): Unit = edit { it[Keys.activeProfile] = id }

    suspend fun setCity(city: City): Unit = edit {
        it[Keys.cityId] = city.id
        it[Keys.cityName] = city.name
        it[Keys.cityArabic] = city.arabicName
        it[Keys.cityCountry] = city.countryCode
        it[Keys.cityLat] = city.latitude
        it[Keys.cityLng] = city.longitude
        it[Keys.cityTz] = city.timeZone
    }

    suspend fun setMethod(method: CalculationMethod): Unit = edit { it[Keys.method] = method.name }

    suspend fun setMadhab(madhab: AsrMadhab): Unit = edit { it[Keys.madhab] = madhab.name }

    suspend fun setHighLatitude(option: HighLatitudeOption): Unit = edit { it[Keys.highLat] = option.name }

    suspend fun setAdjustment(prayer: Prayer, minutes: Int): Unit = edit {
        it[Keys.adjustment(prayer)] = minutes.coerceIn(-PrayerAdjustments.MAX_ADJUSTMENT, PrayerAdjustments.MAX_ADJUSTMENT)
    }

    suspend fun setHijriOffset(days: Int): Unit = edit {
        it[Keys.hijriOffset] = days.coerceIn(-UserSettings.MAX_HIJRI_OFFSET, UserSettings.MAX_HIJRI_OFFSET)
    }

    suspend fun setQuranFontSize(sp: Int): Unit = edit {
        it[Keys.quranFont] = sp.coerceIn(UserSettings.MIN_QURAN_FONT_SP, UserSettings.MAX_QURAN_FONT_SP)
    }

    suspend fun setTimeFormat(format: TimeFormat): Unit = edit { it[Keys.timeFormat] = format.name }

    suspend fun setReciter(id: String): Unit = edit { it[Keys.reciter] = id }

    suspend fun setShowTranslation(show: Boolean): Unit = edit { it[Keys.showTranslation] = show }

    suspend fun setTranslation(id: String): Unit = edit { it[Keys.translation] = id }

    suspend fun setPlaybackSpeed(speed: Float): Unit = edit { it[Keys.speed] = speed }
    suspend fun setNatureScene(index: Int): Unit = edit { it[Keys.scene] = index }
    suspend fun setAmbientSound(sound: AmbientSound): Unit = edit { it[Keys.sound] = sound.name }
    suspend fun setAmbientVolume(v: AmbientVolume): Unit = edit { it[Keys.soundVolume] = v.name }
    suspend fun setAmbientTimeout(minutes: Int): Unit = edit { it[Keys.ambientTimeout] = minutes }
    suspend fun setRamadanMode(m: RamadanMode): Unit = edit { it[Keys.ramadan] = m.name }
    suspend fun setKahfReminder(on: Boolean): Unit = edit { it[Keys.kahf] = on }
    suspend fun setMulkReminder(on: Boolean): Unit = edit { it[Keys.mulk] = on }

    suspend fun setOnboardingDone(done: Boolean): Unit = edit { it[Keys.onboardingDone] = done }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val city = this[Keys.cityId]?.let { id ->
            City(
                id = id,
                name = this[Keys.cityName].orEmpty(),
                arabicName = this[Keys.cityArabic].orEmpty(),
                countryCode = this[Keys.cityCountry].orEmpty(),
                latitude = this[Keys.cityLat] ?: 0.0,
                longitude = this[Keys.cityLng] ?: 0.0,
                timeZone = this[Keys.cityTz] ?: "UTC",
            )
        }
        val adjustments = Prayer.entries.fold(PrayerAdjustments()) { acc, p ->
            acc.with(p, this[Keys.adjustment(p)] ?: 0)
        }
        return UserSettings(
            onboardingDone = this[Keys.onboardingDone] ?: false,
            city = city,
            prayer = PrayerSettings(
                method = enumOrDefault(this[Keys.method], CalculationMethod.MUSLIM_WORLD_LEAGUE),
                madhab = enumOrDefault(this[Keys.madhab], AsrMadhab.SHAFI),
                highLatitude = enumOrDefault(this[Keys.highLat], HighLatitudeOption.AUTO),
                adjustments = adjustments,
            ),
            hijriOffset = this[Keys.hijriOffset] ?: 0,
            quranFontSizeSp = this[Keys.quranFont] ?: UserSettings.DEFAULT_QURAN_FONT_SP,
            timeFormat = enumOrDefault(this[Keys.timeFormat], TimeFormat.SYSTEM),
            reciterId = this[Keys.reciter] ?: UserSettings.DEFAULT_RECITER,
            showTranslation = this[Keys.showTranslation] ?: false,
            translationId = this[Keys.translation] ?: UserSettings.DEFAULT_TRANSLATION,
            playbackSpeed = this[Keys.speed] ?: 1f,
            natureScene = this[Keys.scene] ?: -1,
            ambientSound = enumOrDefault(this[Keys.sound], AmbientSound.OFF),
            ambientVolume = enumOrDefault(this[Keys.soundVolume], AmbientVolume.LOW),
            ambientTimeoutMin = this[Keys.ambientTimeout] ?: 10,
            ramadanMode = enumOrDefault(this[Keys.ramadan], RamadanMode.AUTO),
            kahfReminder = this[Keys.kahf] ?: true,
            mulkReminder = this[Keys.mulk] ?: true,
        )
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val cityId = longPreferencesKey("city_id")
        val cityName = stringPreferencesKey("city_name")
        val cityArabic = stringPreferencesKey("city_name_ar")
        val cityCountry = stringPreferencesKey("city_country")
        val cityLat = doublePreferencesKey("city_lat")
        val cityLng = doublePreferencesKey("city_lng")
        val cityTz = stringPreferencesKey("city_tz")
        val method = stringPreferencesKey("calc_method")
        val madhab = stringPreferencesKey("asr_madhab")
        val highLat = stringPreferencesKey("high_latitude")
        val hijriOffset = intPreferencesKey("hijri_offset")
        val quranFont = intPreferencesKey("quran_font_sp")
        val timeFormat = stringPreferencesKey("time_format")
        val reciter = stringPreferencesKey("reciter")
        val activeProfile = longPreferencesKey("active_profile")
        val showTranslation = booleanPreferencesKey("show_translation")
        val translation = stringPreferencesKey("translation")
        val speed = floatPreferencesKey("playback_speed")
        val scene = intPreferencesKey("nature_scene")
        val sound = stringPreferencesKey("ambient_sound")
        val soundVolume = stringPreferencesKey("ambient_volume")
        val ambientTimeout = intPreferencesKey("ambient_timeout")
        val ramadan = stringPreferencesKey("ramadan_mode")
        val kahf = booleanPreferencesKey("kahf_reminder")
        val mulk = booleanPreferencesKey("mulk_reminder")
        val muezzin = stringPreferencesKey("muezzin")
        val startOnBoot = booleanPreferencesKey("start_on_boot")
        private val alertKeys = Prayer.entries.associateWith { stringPreferencesKey("alert_${it.name.lowercase()}") }
        private val iqamahKeys = Prayer.entries.associateWith { intPreferencesKey("iqamah_${it.name.lowercase()}") }
        fun alertMode(p: Prayer) = alertKeys.getValue(p)
        fun iqamah(p: Prayer) = iqamahKeys.getValue(p)
        private val adjustmentKeys = Prayer.entries.associateWith { intPreferencesKey("adj_${it.name.lowercase()}") }
        fun adjustment(prayer: Prayer) = adjustmentKeys.getValue(prayer)
    }
}
