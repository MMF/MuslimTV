package com.mfouad.muslimtv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mfouad.muslimtv.core.designsystem.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Inter (OFL) for Latin UI text. Variable font: exact weights on API 26+, regular below. */
val InterFamily = FontFamily(
    variable(R.font.inter, 400),
    variable(R.font.inter, 500),
    variable(R.font.inter, 600),
    variable(R.font.inter, 700),
)

/** Noto Naskh Arabic (OFL) for Arabic UI text. */
val NaskhFamily = FontFamily(
    variable(R.font.noto_naskh_arabic, 400),
    variable(R.font.noto_naskh_arabic, 500),
    variable(R.font.noto_naskh_arabic, 600),
    variable(R.font.noto_naskh_arabic, 700),
)

/** Amiri Quran (OFL), designed for Quran text with full Uthmani marks. */
val QuranFamily = FontFamily(Font(R.font.amiri_quran))

/**
 * Sized for a 3 m viewing distance on a 960×540 dp TV canvas: body text never below 24sp.
 */
@Immutable
data class MtvTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val numeric: TextStyle,
)

fun mtvTypography(arabic: Boolean): MtvTypography {
    val family = if (arabic) NaskhFamily else InterFamily
    // Naskh needs more vertical room for its marks.
    fun lh(size: Int): TextUnit = if (arabic) (size * 1.55f).sp else (size * 1.3f).sp
    fun style(size: Int, weight: FontWeight) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lh(size),
        letterSpacing = if (arabic) 0.em else 0.005.em,
    )
    return MtvTypography(
        display = style(56, FontWeight.SemiBold),
        headline = style(40, FontWeight.SemiBold),
        title = style(30, FontWeight.SemiBold),
        body = style(24, FontWeight.Normal),
        bodyStrong = style(24, FontWeight.SemiBold),
        label = style(24, FontWeight.Medium),
        // Only for secondary metadata (credits, units); still readable at distance.
        caption = style(20, FontWeight.Normal),
        numeric = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 56.sp, lineHeight = 64.sp),
    )
}

val LocalMtvTypography = staticCompositionLocalOf { mtvTypography(arabic = false) }

/** Line height is relative (em) so it scales when auto-size shrinks the text. */
fun quranTextStyle(sizeSp: Int, lineHeightEm: Float = 1.9f) = TextStyle(
    fontFamily = QuranFamily,
    fontSize = sizeSp.sp,
    lineHeight = lineHeightEm.em,
)
