package com.mfouad.muslimtv.core.model

data class Muezzin(val id: String, val nameEn: String, val nameAr: String)

/** Bundled CC0 recordings (Wikimedia Commons). */
object Muezzins {
    val all = listOf(
        Muezzin("full", "Full adhan", "الأذان كاملًا"),
        Muezzin("short", "Short adhan", "أذان قصير"),
    )
}
