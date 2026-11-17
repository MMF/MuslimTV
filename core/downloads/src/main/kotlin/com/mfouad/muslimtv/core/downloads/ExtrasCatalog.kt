package com.mfouad.muslimtv.core.downloads

/** Extra content offered in the download manager besides recitations. */
data class ExtraContent(
    val id: String,
    val kind: DownloadKind,
    val title: String,
    val url: String,
    val license: String,
)

/**
 * Translations from Tanzil (verbatim, "for non-commercial purposes only" per Tanzil's terms;
 * Muslim TV is free). Scenes and timing data are added through the remote config.
 */
object ExtrasCatalog {
    private fun tanzil(id: String, title: String) =
        ExtraContent(id, DownloadKind.TRANSLATION, title, "https://tanzil.net/trans/?transID=$id&type=txt-2", "Tanzil translations, non-commercial use")

    val translations = listOf(
        tanzil("en.sahih", "English · Saheeh International"),
        tanzil("fr.hamidullah", "Français · Hamidullah"),
        tanzil("ur.jalandhry", "اردو · جالندھری"),
        tanzil("id.indonesian", "Bahasa Indonesia · Kemenag"),
        tanzil("tr.diyanet", "Türkçe · Diyanet"),
        tanzil("ms.basmeih", "Bahasa Melayu · Basmeih"),
        tanzil("bn.bengali", "বাংলা · Muhiuddin Khan"),
    )
}
