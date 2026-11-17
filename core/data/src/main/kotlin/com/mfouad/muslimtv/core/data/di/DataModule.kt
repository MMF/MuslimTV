package com.mfouad.muslimtv.core.data.di

import com.mfouad.muslimtv.core.data.city.AssetCityRepository
import com.mfouad.muslimtv.core.data.city.CityRepository
import com.mfouad.muslimtv.core.data.hijri.HijriCalendar
import com.mfouad.muslimtv.core.data.hijri.PlatformHijriCalendar
import com.mfouad.muslimtv.core.data.audio.AudioSource
import com.mfouad.muslimtv.core.data.audio.AyahTimingSource
import com.mfouad.muslimtv.core.data.audio.EveryAyahAudioSource
import com.mfouad.muslimtv.core.data.audio.FileTimingSource
import com.mfouad.muslimtv.core.data.profile.ActiveProfile
import com.mfouad.muslimtv.core.data.profile.DataStoreActiveProfile
import com.mfouad.muslimtv.core.data.quran.QuranRepository
import com.mfouad.muslimtv.core.data.quran.RoomQuranRepository
import com.mfouad.muslimtv.core.data.settings.DataStoreSettingsRepository
import com.mfouad.muslimtv.core.data.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds abstract fun settings(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds abstract fun cities(impl: AssetCityRepository): CityRepository
    @Binds abstract fun quran(impl: RoomQuranRepository): QuranRepository
    @Binds abstract fun audio(impl: EveryAyahAudioSource): AudioSource
    @Binds abstract fun timings(impl: FileTimingSource): AyahTimingSource
    @Binds abstract fun profile(impl: DataStoreActiveProfile): ActiveProfile
    @Binds abstract fun hijri(impl: PlatformHijriCalendar): HijriCalendar
}
