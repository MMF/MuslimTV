package com.mfouad.muslimtv.core.data.profile

import kotlinx.coroutines.flow.Flow

/** The family member using the TV now. Bookmarks, positions, khatmah and tahfeez are per profile. */
interface ActiveProfile {
    val id: Flow<Long>

    companion object {
        const val DEFAULT_ID = 1L
    }
}
