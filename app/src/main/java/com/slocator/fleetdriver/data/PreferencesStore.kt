package com.slocator.fleetdriver.data

import android.content.Context

/**
 * Tiny KV wrapper for app-level preferences.
 *
 *  - [driverId]          Server-assigned driver UUID (from /api/tracking/driver-id).
 *  - [driverPhone]       Phone number the driver logged in with.
 *  - [sessionId]         Active tracking session (from /api/tracking/start-route).
 *  - [languageOverride]  "ar" (default) or "en".
 */
class PreferencesStore(context: Context) {

    private val prefs = context.getSharedPreferences("slocator_app", Context.MODE_PRIVATE)

    var driverId: String?
        get() = prefs.getString("driver_id", null)
        set(value) = prefs.edit().putString("driver_id", value).apply()

    var driverPhone: String?
        get() = prefs.getString("driver_phone", null)
        set(value) = prefs.edit().putString("driver_phone", value).apply()

    var sessionId: String?
        get() = prefs.getString("session_id", null)
        set(value) = prefs.edit().putString("session_id", value).apply()

    var languageOverride: String?
        get() = prefs.getString("language_override", null)
        set(value) = prefs.edit().putString("language_override", value).apply()
}
