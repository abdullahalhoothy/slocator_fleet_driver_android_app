package com.slocator.fleetdriver.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * API calls for the route-tracking lifecycle (start / end).
 *
 * Uses HttpURLConnection to match the existing convention established in [RoutesRepository].
 */
object RouteTrackingApi {

    /**
     * POST /start_route — notifies the backend that the driver has begun their route.
     *
     * @param driverPhone  The driver's phone number.
     * @param managerPhone The manager's phone number.
     * @param day          The day number (1-based) being started.
     */
    suspend fun startRoute(
        driverPhone: String,
        managerPhone: String,
        day: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("RouteTrackingApi", "Starting route: ${BaseUrl.URL}/start_route")
            val url = URL("${BaseUrl.URL}/start_route")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("driver_phone", driverPhone)
                put("manager_phone", managerPhone)
                put("day", day)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) Result.success(Unit)
            else Result.failure(Exception("Server error $code"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * POST /end_route — notifies the backend that the driver has finished their route.
     */
    suspend fun endRoute(driverPhone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("RouteTrackingApi", "Ending route: ${BaseUrl.URL}/end_route")
            val url = URL("${BaseUrl.URL}/end_route")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("driver_phone", driverPhone)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) Result.success(Unit)
            else Result.failure(Exception("Server error $code"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * POST /note — sends a free-text note from the driver to the backend.
     *
     * The backend logs the note as a timeline event tied to the active route session.
     */
    suspend fun postNote(driverPhone: String, noteText: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("RouteTrackingApi", "Posting note: ${BaseUrl.URL}/note")
            val url = URL("${BaseUrl.URL}/note")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("driver_phone", driverPhone)
                put("note_text", noteText)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) Result.success(Unit)
            else Result.failure(Exception("Server error $code"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
