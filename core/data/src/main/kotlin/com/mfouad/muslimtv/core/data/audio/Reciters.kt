package com.mfouad.muslimtv.core.data.audio

import com.mfouad.muslimtv.core.model.RecitationStyle
import com.mfouad.muslimtv.core.model.Reciter

/**
 * Reciters offered in the app. Folders are EveryAyah per-ayah recitations.
 * Word timings are only marked where per-word timing data is known to exist (QUL).
 */
object Reciters {
    val all: List<Reciter> = listOf(
        Reciter("alafasy", "Mishary Rashid Alafasy", "مشاري راشد العفاسي", RecitationStyle.MURATTAL, "Alafasy_128kbps", 128, hasWordTimings = true),
        Reciter("husary", "Mahmoud Khalil Al-Husary", "محمود خليل الحصري", RecitationStyle.MURATTAL, "Husary_128kbps", 128),
        Reciter("husary_mujawwad", "Al-Husary (Mujawwad)", "الحصري (مجوّد)", RecitationStyle.MUJAWWAD, "Husary_128kbps_Mujawwad", 128),
        Reciter("husary_muallim", "Al-Husary (teaching)", "الحصري (المعلم)", RecitationStyle.MUALLIM, "Husary_Muallim_128kbps", 128, forKids = true),
        Reciter("minshawi", "Mohamed Siddiq El-Minshawi", "محمد صديق المنشاوي", RecitationStyle.MURATTAL, "Minshawy_Murattal_128kbps", 128),
        Reciter("minshawi_mujawwad", "El-Minshawi (Mujawwad)", "المنشاوي (مجوّد)", RecitationStyle.MUJAWWAD, "Minshawy_Mujawwad_192kbps", 192),
        Reciter("abdulbasit", "Abdul Basit Abdul Samad", "عبد الباسط عبد الصمد", RecitationStyle.MURATTAL, "Abdul_Basit_Murattal_192kbps", 192),
        Reciter("abdulbasit_mujawwad", "Abdul Basit (Mujawwad)", "عبد الباسط (مجوّد)", RecitationStyle.MUJAWWAD, "Abdul_Basit_Mujawwad_128kbps", 128),
        Reciter("sudais", "Abdurrahman As-Sudais", "عبد الرحمن السديس", RecitationStyle.MURATTAL, "Abdurrahmaan_As-Sudais_192kbps", 192),
        Reciter("shuraim", "Saud Ash-Shuraim", "سعود الشريم", RecitationStyle.MURATTAL, "Saood_ash-Shuraym_128kbps", 128),
        Reciter("muaiqly", "Maher Al-Muaiqly", "ماهر المعيقلي", RecitationStyle.MURATTAL, "MaherAlMuaiqly128kbps", 128),
        Reciter("ghamdi", "Saad Al-Ghamdi", "سعد الغامدي", RecitationStyle.MURATTAL, "Ghamadi_40kbps", 40),
        Reciter("shatri", "Abu Bakr Ash-Shatri", "أبو بكر الشاطري", RecitationStyle.MURATTAL, "Abu_Bakr_Ash-Shaatree_128kbps", 128),
        Reciter("dossari", "Yasser Ad-Dossari", "ياسر الدوسري", RecitationStyle.MURATTAL, "Yasser_Ad-Dussary_128kbps", 128),
        Reciter("hudhaify", "Ali Al-Hudhaify", "علي الحذيفي", RecitationStyle.MURATTAL, "Hudhaify_128kbps", 128),
    )

    fun byId(id: String): Reciter = all.firstOrNull { it.id == id } ?: all.first()

    /** Muallim-style reciter for the kids' Tahfeez preset. */
    val kids: Reciter get() = all.first { it.forKids }
}
