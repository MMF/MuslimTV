package com.mfouad.muslimtv.core.data.profile

import com.mfouad.muslimtv.core.datastore.SettingsDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
class DataStoreActiveProfile @Inject constructor(source: SettingsDataSource) : ActiveProfile {
    override val id: Flow<Long> = source.activeProfileId.distinctUntilChanged()
}
