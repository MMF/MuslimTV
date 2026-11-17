package com.mfouad.muslimtv.core.model

/** A city from the bundled GeoNames list. Coordinates and time zone are all prayer times need. */
data class City(
    val id: Long,
    val name: String,
    val arabicName: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val population: Long = 0,
) {
    fun displayName(arabic: Boolean): String = if (arabic && arabicName.isNotBlank()) arabicName else name
}
