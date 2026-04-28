package com.slocator.fleetdriver

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.slocator.fleetdriver.data.PreferencesStore

class SLocatorApp : Application() {

    lateinit var prefs: PreferencesStore
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesStore(this)

        // Default to Arabic on first run; persist across launches via AppCompat per-app locale.
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.isEmpty) {
            val tag = prefs.languageOverride ?: "ar"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            prefs.languageOverride = tag
        }
    }

    fun toggleLanguage() {
        val tag = if ((prefs.languageOverride ?: "ar") == "ar") "en" else "ar"
        prefs.languageOverride = tag
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
