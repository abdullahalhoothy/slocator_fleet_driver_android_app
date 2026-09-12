package com.slocator.fleetdriver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.slocator.fleetdriver.data.PreferencesStore
import com.slocator.fleetdriver.data.RouteTrackingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Foreground service that continuously tracks the driver's location
 * and POSTs pings to the backend every ~5 seconds while the route is active.
 *
 * Started via [ACTION_START], stopped via [ACTION_STOP].
 */
class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var prefs: PreferencesStore

    private var driverId: String = ""
    private var sessionId: String? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "location_tracking"
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesStore(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopLocationUpdates()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // Read identity from prefs so a START_STICKY restart keeps working.
                if (driverId.isBlank()) driverId = prefs.driverId.orEmpty()
                if (sessionId == null) sessionId = prefs.sessionId
                if (driverId.isBlank()) {
                    Log.w("LocationService", "No driver id stored — stopping tracking service")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification())
                if (!::locationCallback.isInitialized) startLocationUpdates()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------------------------------------------------------

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5_000L
        )
            .setMinUpdateDistanceMeters(10f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch { sendPing(location) }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private suspend fun sendPing(location: Location) {
        // Skip very inaccurate fixes
        if (location.accuracy > 50f) return

        val id = driverId.ifBlank { prefs.driverId.orEmpty() }
        if (id.isBlank()) return

        RouteTrackingApi.sendLocation(
            driverId = id,
            lat = location.latitude,
            lng = location.longitude,
            accuracyM = location.accuracy.toDouble(),
            timestamp = Clock.System.now().toString()
        ).onSuccess { response ->
            // Keep the latest server-side session id (null = no active session).
            response.sessionId?.let { newSessionId ->
                if (newSessionId != sessionId) {
                    sessionId = newSessionId
                    prefs.sessionId = newSessionId
                }
            }
            response.transitions.forEach { transition ->
                Log.i(
                    "LocationService",
                    "Proximity ${transition.eventType}: ${transition.poiName} " +
                        "(customer=${transition.customerId}, ${transition.distanceM} m)"
                )
            }
        }.onFailure { t ->
            Log.w("LocationService", "Ping failed: ${t.message}")
        }
    }

    // ---------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(R.drawable.ic_navigation)
            .setOngoing(true)
            .build()
    }
}
