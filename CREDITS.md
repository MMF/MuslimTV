# Credits and licenses

Everything bundled in Muslim TV, with its source and license. The in-app Credits screen
(`feature/settings/.../CreditsScreen.kt`) lists the same items.

## Bundled (license confirmed)

| Item | Source | License | Where |
|---|---|---|---|
| Quran text, Uthmani v1.1 (verbatim; SHA-256 checked by a unit test) | Tanzil Project | CC BY 3.0, verbatim only | `core/data/src/main/assets/quran/quran-uthmani.txt` |
| Quran metadata (surahs, juz, pages) | Tanzil Project | CC BY 3.0 | `.../quran/quran-data.xml` |
| English translation: Pickthall (1930) | Tanzil digital text | Public domain (translator d. 1936) | `.../quran/en.pickthall.txt` |
| Hisn al-Muslim, Arabic, with counts and references | asellam/HisnElMuslim | MIT (book: al-Qahtani; not sold) | `.../azkar/hisn.json` |
| Asma ul-Husna (names, transliteration, meaning, note) | teamsazen/99-Names | MIT | `.../azkar/asma.json` |
| Hadith: short Bukhari/Muslim selection, Forty Nawawi (Arabic) | fawazahmed0/hadith-api | Unlicense | `.../hadith/` |
| City list | GeoNames | CC BY 4.0 | `.../cities/cities.txt` |
| World map land outlines | Natural Earth 1:110m | Public domain | `.../map/land.json` |
| Adhan recordings (full, short) | Wikimedia Commons: Beautiful_adhan.ogg (Adam-synagda), Adhan.ogg (Aishatu98) | CC0 | `feature/prayer/src/main/res/raw/` |
| Nature scenes ×8 | Wikimedia Commons (DimiTalen, Bernard Gagnon, W.carter, Jebulon, Leonhard Lenz) | CC0 | `core/designsystem/src/main/res/drawable-nodpi/` |
| Ambient sounds: rain, stream, birds | Wikimedia Commons (cori, Karlunun, nille) | PD / CC0 | `core/media/src/main/res/raw/` |
| Fonts: Amiri Quran, Noto Naskh Arabic, Inter | Google Fonts | SIL OFL 1.1 | `core/designsystem/src/main/res/font/` |
| Adhan prayer-time library | Batoul Apps | MIT | Gradle |
| Umm al-Qura calendar (API 24+) | Android ICU | Unicode License v3 | Platform |
| AndroidX, Compose for TV, Media3, Room, WorkManager, Hilt, OkHttp, Kotlin | Google / JetBrains / Square | Apache 2.0 | Gradle |

License texts ship in `core/designsystem/src/main/assets/licenses/`, `core/data/src/main/assets/azkar/`
and `core/data/src/main/assets/hadith/`.

## Bundled with conditions (check before a paid or ad-supported release)

| Item | Condition |
|---|---|
| Tafsir al-Jalalayn (Arabic, Tanzil) | Tanzil translation terms: "for non-commercial purposes only". Fine while the app is free with no ads. |
| Downloadable translations (Saheeh International, Hamidullah, …) from tanzil.net | Same Tanzil terms; Tanzil also asks for a link back when more than three translations are used (shown in Credits). |

## Not bundled: needs permission before the Play release

| Item | Status |
|---|---|
| Per-ayah recitation audio (EveryAyah) | Streamed and downloaded on request; no published terms. Ask EveryAyah for written permission. |
| Word timing data (QUL / Tarteel) | Requires login; licence page says "We don't have copyright information." Not included; follow-along highlights whole ayahs. The app reads timing files if they are added through the download manager later. |
| Live streams (Saudi Quran TV, Sunnah TV) and Quran radios (mp3quran) | Links only, in `core/data/src/main/assets/config/remote.json`. Confirm the broadcasters allow third-party playback. |
| KFGQPC Uthmanic Hafs font | Not used; Amiri Quran instead. |
| English azkar/hadith translations, azkar and Asma ul-Husna audio | No safely licensed source found; not included. |
