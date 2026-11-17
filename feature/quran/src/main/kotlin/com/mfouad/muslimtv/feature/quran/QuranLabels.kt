package com.mfouad.muslimtv.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mfouad.muslimtv.core.model.RecitationStyle
import com.mfouad.muslimtv.core.model.Revelation
import com.mfouad.muslimtv.core.model.Surah
import com.mfouad.muslimtv.core.ui.isArabicUi

@Composable
internal fun Revelation.label() = stringResource(if (this == Revelation.MAKKI) R.string.makki else R.string.madani)

@Composable
internal fun RecitationStyle.label() = stringResource(
    when (this) {
        RecitationStyle.MURATTAL -> R.string.style_murattal
        RecitationStyle.MUJAWWAD -> R.string.style_mujawwad
        RecitationStyle.MUALLIM -> R.string.style_muallim
    },
)

@Composable
internal fun Surah.displayName(): String = if (isArabicUi()) arabicName else transliteration
