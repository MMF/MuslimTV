package com.mfouad.muslimtv.core.common

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Source of the current time, replaceable in tests. */
interface TimeProvider {
    fun nowMillis(): Long

    /** Emits the current time once per second, aligned to the start of each second. */
    fun ticker(periodMillis: Long = 1_000L): Flow<Long> = flow {
        while (true) {
            val now = nowMillis()
            emit(now)
            delay(periodMillis - now % periodMillis)
        }
    }
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
