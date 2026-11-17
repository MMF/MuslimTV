package com.mfouad.muslimtv.core.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** The two interface languages. Switching recreates the activity with the new locale. */
enum class AppLanguage(val tag: String) {
    ARABIC("ar"),
    ENGLISH("en"),
    ;

    companion object {
        /** The chosen app language, or the device language when none was chosen yet. */
        fun current(): AppLanguage {
            val tag = AppCompatDelegate.getApplicationLocales()[0]?.language ?: Locale.getDefault().language
            return if (tag == "ar") ARABIC else ENGLISH
        }

        fun set(language: AppLanguage) {
            if (AppCompatDelegate.getApplicationLocales()[0]?.language == language.tag) return
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }
    }
}

/** True when the UI is currently Arabic. */
@Composable
fun isArabicUi(): Boolean = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]?.language == "ar"
