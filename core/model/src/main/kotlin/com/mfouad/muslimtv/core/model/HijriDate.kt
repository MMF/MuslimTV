package com.mfouad.muslimtv.core.model

data class HijriDate(val year: Int, val month: Int, val day: Int) {
    init {
        require(month in 1..12) { "month must be 1..12" }
        require(day in 1..30) { "day must be 1..30" }
    }

    val isRamadan: Boolean get() = month == RAMADAN

    companion object {
        const val RAMADAN = 9
    }
}
