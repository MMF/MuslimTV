package com.mfouad.muslimtv.core.data

import com.mfouad.muslimtv.core.data.audio.EveryAyahAudioSource
import com.mfouad.muslimtv.core.model.AyahRef
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AudioFileNameTest {
    @Test
    fun fileNameUsesAsciiDigitsOnArabicDevices() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"))
            assertEquals("001001.mp3", EveryAyahAudioSource.fileName(AyahRef(1, 1)))
            assertEquals("067030.mp3", EveryAyahAudioSource.fileName(AyahRef(67, 30)))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
