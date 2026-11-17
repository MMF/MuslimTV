package com.mfouad.muslimtv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.FocusCard
import com.mfouad.muslimtv.core.designsystem.component.ScreenHeader
import com.mfouad.muslimtv.core.designsystem.focus.RestoreFocusEffect
import com.mfouad.muslimtv.core.designsystem.focus.rememberFocusRestoreState
import com.mfouad.muslimtv.core.designsystem.focus.restorableFocus
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme

/** Everything bundled in the app, with its source and license. Kept in sync with CREDITS.md. */
internal data class Credit(val title: String, val detail: String, val license: String, val link: String)

internal val credits = listOf(
    Credit("Quran text (Uthmani, v1.1)", "Tanzil Project. Used verbatim, unmodified.", "CC BY 3.0", "tanzil.net"),
    Credit("Quran metadata", "Surah names, ayah counts, juz. Tanzil Project.", "CC BY 3.0", "tanzil.net"),
    Credit("English translation · Pickthall (1930)", "Marmaduke Pickthall, public domain; digital text from Tanzil.", "Public domain", "tanzil.net"),
    Credit("Tafsir al-Jalalayn (Arabic)", "Digital text from Tanzil, verbatim.", "Tanzil terms: non-commercial use", "tanzil.net/trans"),
    Credit("Hisn al-Muslim (Arabic azkar with references)", "Sa'id al-Qahtani; dataset by Abdellah Sellam.", "MIT", "github.com/asellam/HisnElMuslim"),
    Credit("Asma ul-Husna names and meanings", "99 Names contributors.", "MIT", "github.com/teamsazen/99-Names"),
    Credit("Hadith (Bukhari, Muslim, Forty Nawawi), Arabic", "fawazahmed0/hadith-api.", "Unlicense (public domain)", "github.com/fawazahmed0/hadith-api"),
    Credit("Adhan recordings", "\"Beautiful adhan\" by Adam-synagda; \"Adhan\" by Aishatu98. Wikimedia Commons.", "CC0", "commons.wikimedia.org"),
    Credit("Nature scenes", "DimiTalen, Bernard Gagnon, W.carter, Jebulon, Leonhard Lenz. Wikimedia Commons.", "CC0", "commons.wikimedia.org"),
    Credit("Ambient sounds (rain, stream, birds)", "cori, Karlunun, nille. Wikimedia Commons / pdsounds.org.", "Public domain / CC0", "commons.wikimedia.org"),
    Credit("World map", "Natural Earth 1:110m land.", "Public domain", "naturalearthdata.com"),
    Credit("Recitation audio", "EveryAyah per-ayah recordings, streamed and downloaded on request.", "Used with permission (pending)", "everyayah.com"),
    Credit("City list", "Names, coordinates and time zones of cities above 15,000 people. GeoNames.", "CC BY 4.0", "geonames.org"),
    Credit("Adhan prayer times library", "Batoul Apps. Prayer times calculated on the device.", "MIT", "github.com/batoulapps/adhan-java"),
    Credit("Hijri date (Umm al-Qura)", "Android ICU calendar data, Unicode, Inc.", "Unicode License v3", "unicode.org/copyright.html"),
    Credit("Amiri Quran font", "The Amiri Quran Project Authors.", "SIL Open Font License 1.1", "github.com/aliftype/amiri"),
    Credit("Noto Naskh Arabic font", "The Noto Project Authors.", "SIL Open Font License 1.1", "github.com/notofonts/arabic"),
    Credit("Inter font", "The Inter Project Authors.", "SIL Open Font License 1.1", "github.com/rsms/inter"),
    Credit("AndroidX, Compose for TV, Media3, Room, WorkManager, Hilt, OkHttp", "Google and the Android Open Source Project.", "Apache 2.0", "developer.android.com"),
    Credit("Kotlin and kotlinx libraries", "JetBrains.", "Apache 2.0", "kotlinlang.org"),
)

@Composable
internal fun CreditsScreen() {
    val focus = rememberFocusRestoreState()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        ScreenHeader(stringResource(R.string.credits_title), subtitle = stringResource(R.string.credits_body))
        LazyColumn(
            modifier = Modifier.widthIn(max = 980.dp),
            verticalArrangement = Arrangement.spacedBy(MtvSpacing.s),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        ) {
            items(credits, key = { it.title }) { c ->
                // Focusable so the remote can scroll the list; there is nothing to open.
                FocusCard(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().restorableFocus(focus, c.title),
                    shape = MtvShapes.small,
                    focusedScale = 1.02f,
                    contentPadding = MtvSpacing.m,
                ) {
                    Column {
                        Text(c.title, style = MtvTheme.type.bodyStrong)
                        Text(c.detail, style = MtvTheme.type.body, color = MtvTheme.colors.onBackgroundMuted)
                        Text("${c.license} · ${c.link}", style = MtvTheme.type.caption, color = MtvTheme.colors.goldSoft)
                    }
                }
            }
        }
    }
    RestoreFocusEffect(focus, credits.first().title)
}
