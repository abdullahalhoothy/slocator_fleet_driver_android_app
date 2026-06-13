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
import com.slocator.fleetdriver.data.BaseUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Foreground service that continuously tracks the driver's location
 * and POSTs pings to the backend every ~5 seconds while the route is active.
 *
 * Started via [ACTION_START], stopped via [ACTION_STOP].
 */
class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var driverPhone: String = ""

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_DRIVER_PHONE = "EXTRA_DRIVER_PHONE"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "location_tracking"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                driverPhone = intent.getStringExtra(EXTRA_DRIVER_PHONE) ?: ""
                startForeground(NOTIFICATION_ID, buildNotification())
                startLocationUpdates()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopSelf()
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

        try {
            val url = URL("${BaseUrl.URL}/location")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("driver_phone", driverPhone)
                put("lat", location.latitude)
                put("lng", location.longitude)
                put("accuracy_m", location.accuracy)
                put("timestamp", Clock.System.now().toString())
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            conn.disconnect()

            if (code !in 200..299) {
                Log.w("LocationService", "Ping failed with HTTP $code")
            }
        } catch (t: Throwable) {
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
