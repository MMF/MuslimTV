package com.mfouad.muslimtv.core.common

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CommonModule {
    @Binds
    abstract fun timeProvider(impl: SystemTimeProvider): TimeProvider

    @Binds
    abstract fun networkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}
