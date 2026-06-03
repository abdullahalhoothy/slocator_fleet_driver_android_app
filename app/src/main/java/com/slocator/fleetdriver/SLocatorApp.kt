package com.slocator.fleetdriver

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.slocator.fleetdriver.data.PreferencesStore
import com.slocator.fleetdriver.di.appModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SLocatorApp : Application() {

    val prefs: PreferencesStore by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SLocatorApp)
            modules(appModule)
        }

        // 1. If first run (no pref), set default to Arabic "ar"
        if (prefs.languageOverride == null) {
            prefs.languageOverride = "ar"
        }

        // 2. Sync AppCompat locales with our preference.
        val targetTag = prefs.languageOverride ?: "ar"
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.isEmpty || current.get(0)?.language != targetTag) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetTag))
        }
    }

    fun toggleLanguage() {
        val currentTag = prefs.languageOverride ?: "ar"
        val newTag = if (currentTag == "ar") "en" else "ar"
        
        // Update preference
        prefs.languageOverride = newTag
        
        // Update AppCompat locale (triggers Activity recreation)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newTag))
    }
}
