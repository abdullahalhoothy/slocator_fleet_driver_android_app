package com.slocator.fleetdriver.data

import android.content.Context

/**
 * Tiny KV wrapper for app-level preferences (last logged-in driver, language toggle).
 */
class PreferencesStore(context: Context) {

    private val prefs = context.getSharedPreferences("slocator_app", Context.MODE_PRIVATE)

    var lastDriverId: String?
        get() = prefs.getString("last_driver_id", null)
        set(value) = prefs.edit().putString("last_driver_id", value).apply()

    var languageOverride: String?
        get() = prefs.getString("language_override", null)
        set(value) = prefs.edit().putString("language_override", value).apply()
}
