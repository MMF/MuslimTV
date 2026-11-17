package com.mfouad.muslimtv.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.designsystem.theme.quranTextStyle

/**
 * Quran text in the Quran font, always right to left whatever the UI language is.
 * It is never truncated: with [minSizeSp] set, the text shrinks to fit its box instead.
 */
@Composable
fun QuranText(
    text: String,
    sizeSp: Int,
    modifier: Modifier = Modifier,
    color: Color = MtvTheme.colors.onBackground,
    textAlign: TextAlign = TextAlign.Center,
    minSizeSp: Int? = null,
    lineHeightEm: Float = 1.9f,
) {
    val style = quranTextStyle(sizeSp, lineHeightEm).copy(textDirection = TextDirection.Rtl, textAlign = textAlign, color = color)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BasicText(
            text = text,
            style = style,
            modifier = modifier,
            autoSize = minSizeSp?.let { TextAutoSize.StepBased(minFontSize = it.sp, maxFontSize = sizeSp.sp, stepSize = 2.sp) },
        )
    }
}

/** Annotated variant, for follow-along highlighting of the word being recited. */
@Composable
fun QuranText(
    text: AnnotatedString,
    sizeSp: Int,
    modifier: Modifier = Modifier,
    color: Color = MtvTheme.colors.onBackground,
    textAlign: TextAlign = TextAlign.Start,
    lineHeightEm: Float = 1.9f,
    minSizeSp: Int? = null,
) {
    val style = quranTextStyle(sizeSp, lineHeightEm).copy(textDirection = TextDirection.Rtl, textAlign = textAlign, color = color)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BasicText(
            text = text,
            style = style,
            modifier = modifier,
            autoSize = minSizeSp?.let { TextAutoSize.StepBased(minFontSize = it.sp, maxFontSize = sizeSp.sp, stepSize = 2.sp) },
        )
    }
}
