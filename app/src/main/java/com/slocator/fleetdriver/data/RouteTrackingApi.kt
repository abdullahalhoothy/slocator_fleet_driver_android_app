package com.slocator.fleetdriver.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Calls for the route-tracking lifecycle.
 *
 * Endpoints:
 *   POST /api/tracking/start-route   -> StartTrackingResponse
 *   POST /api/tracking/location      -> TrackingLocationResponse
 *   POST /api/tracking/note          -> TrackingNoteResponse
 *   POST /api/tracking/end-route     -> EndTrackingResponse
 *
 * Uses HttpURLConnection to match the convention established in [RoutesRepository].
 */
object RouteTrackingApi {

    private const val DEFAULT_TIMEOUT_MS = 10_000
    private const val PING_TIMEOUT_MS = 5_000

    /**
     * POST /api/tracking/start-route
     * body: { driver_id: string; route_day_id?: string | null }
     */
    suspend fun startRoute(
        driverId: String,
        routeDayId: String?
    ): Result<StartTrackingResponse> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("driver_id", driverId)
            put("route_day_id", routeDayId ?: JSONObject.NULL)
        }
        postJson("/api/tracking/start-route", payload).mapCatching { body ->
            StartTrackingResponse(sessionId = JSONObject(body).optString("session_id", ""))
        }
    }

    /**
     * POST /api/tracking/location
     * body: { driver_id: string; lat: number; lng: number;
     *         accuracy_m: number; timestamp: string /* ISO datetime */ }
     */
    suspend fun sendLocation(
        driverId: String,
        lat: Double,
        lng: Double,
        accuracyM: Double,
        timestamp: String
    ): Result<TrackingLocationResponse> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("driver_id", driverId)
            put("lat", lat)
            put("lng", lng)
            put("accuracy_m", accuracyM)
            put("timestamp", timestamp)
        }
        postJson("/api/tracking/location", payload, PING_TIMEOUT_MS).mapCatching { body ->
            parseLocationResponse(JSONObject(body))
        }
    }

    /**
     * POST /api/tracking/note
     * body: { driver_id: string; note_text: string; session_id?: string | null }
     */
    suspend fun postNote(
        driverId: String,
        noteText: String,
        sessionId: String?
    ): Result<TrackingNoteResponse> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("driver_id", driverId)
            put("note_text", noteText)
            put("session_id", sessionId ?: JSONObject.NULL)
        }
        postJson("/api/tracking/note", payload).mapCatching { body ->
            TrackingNoteResponse(eventId = JSONObject(body).optLong("event_id"))
        }
    }

    /**
     * POST /api/tracking/end-route
     * body: { driver_id: string }
     */
    suspend fun endRoute(driverId: String): Result<EndTrackingResponse> =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply { put("driver_id", driverId) }
            postJson("/api/tracking/end-route", payload).mapCatching { body ->
                EndTrackingResponse(
                    endedSessions = parseStringArray(JSONObject(body).optJSONArray("ended_sessions"))
                )
            }
        }

    // ── parsing ─────────────────────────────────────────────────────

    private fun parseLocationResponse(o: JSONObject): TrackingLocationResponse {
        val transitionsArr = o.optJSONArray("transitions") ?: JSONArray()
        val transitions = (0 until transitionsArr.length()).map { i ->
            val t = transitionsArr.getJSONObject(i)
            ProximityTransition(
                customerId = t.optString("customer_id"),
                poiName = t.optString("poi_name"),
                poiLat = t.optDouble("poi_lat"),
                poiLng = t.optDouble("poi_lng"),
                eventType = t.optString("event_type"),
                distanceM = t.optDouble("distance_m")
            )
        }
        return TrackingLocationResponse(
            driverId = o.optString("driver_id"),
            sessionId = if (o.isNull("session_id")) null else o.optString("session_id").ifBlank { null },
            transitions = transitions
        )
    }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it) }
    }

    // ── HTTP ────────────────────────────────────────────────────────

    private fun postJson(
        path: String,
        payload: JSONObject,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS
    ): Result<String> {
        return try {
            Log.d("RouteTrackingApi", "POST ${BaseUrl.URL}$path")
            val conn = URL("${BaseUrl.URL}$path").openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            } else {
                ""
            }
            conn.disconnect()

            if (code in 200..299) Result.success(body)
            else Result.failure(Exception("HTTP Error: $code"))
        } catch (t: Throwable) {
            Log.e("RouteTrackingApi", "POST $path failed: ${t.message}", t)
            Result.failure(t)
        }
    }
}
