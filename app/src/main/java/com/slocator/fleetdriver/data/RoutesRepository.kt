package com.slocator.fleetdriver.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class RoutesRepository {

    suspend fun fetchSchedule(driverPhone: String, managerPhone: String): Result<DriverSchedule> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("RoutesRepository", "Fetching schedule from: ${BaseUrl.URL}/driver_links")
                val url = URL("${BaseUrl.URL}/driver_links")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("driver_phone", driverPhone)
                    put("manager_phone", managerPhone)
                }

                conn.outputStream.use { os ->
                    val input = payload.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = conn.responseCode
                Log.d("RoutesRepository", "Response code: $responseCode from /driver_links")
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseStr = reader.readText()
                    reader.close()

                    Log.d("RoutesRepository", "Response body: $responseStr")

                    val jsonResponse = JSONObject(responseStr)
                    val routesArray = jsonResponse.optJSONArray("routes")

                    // Parse report URLs from the top-level response.
                    // The server may return "host" or "localhost" as the hostname;
                    // patch it to the real BaseUrl so WebView can load the page.
                    val reportUrls = ReportUrls(
                        routesMapUrl = jsonResponse.optString("routes_map_url", "")
                            .ifBlank { null }?.let { normalizeReportUrl(it) },
                        shopsMapUrl = jsonResponse.optString("shops_map_url", "")
                            .ifBlank { null }?.let { normalizeReportUrl(it) },
                        clustersMapUrl = jsonResponse.optString("clusters_map_url", "")
                            .ifBlank { null }?.let { normalizeReportUrl(it) }
                    )

                    if (routesArray == null || routesArray.length() == 0) {
                        return@withContext Result.failure(Exception("No routes found"))
                    }

                    val daysList = mutableListOf<ScheduledDay>()
                    for (i in 0 until routesArray.length()) {
                        val routeObj = routesArray.getJSONObject(i)
                        val dayInt = routeObj.optInt("day", i + 1)
                        val dateStr = routeObj.optString("date", "")
                        val linksArray = routeObj.optJSONArray("links")
                        
                        val parsedDate = DayResolver.parseHeaderDate(dateStr)

                        val parts = mutableListOf<RoutePart>()
                        if (linksArray != null) {
                            for (j in 0 until linksArray.length()) {
                                val linkUrl = linksArray.getString(j)
                                parts.add(
                                    RoutePart(
                                        partNumber = j + 1,
                                        mapsUrl = linkUrl,
                                        stopCount = countStops(linkUrl)
                                    )
                                )
                            }
                        }
                        
                        if (parts.isNotEmpty()) {
                            daysList.add(
                                ScheduledDay(
                                    dayLabel = "Day $dayInt",
                                    date = parsedDate,
                                    parts = parts
                                )
                            )
                        }
                    }
                    
                    if (daysList.isEmpty()) {
                        Result.failure(Exception("No valid routes in payload"))
                    } else {
                        Result.success(
                            DriverSchedule(
                                driverId = driverPhone,
                                days = daysList,
                                reportUrls = reportUrls
                            )
                        )
                    }
                } else {
                    Log.w("RoutesRepository", "Request failed with HTTP $responseCode from /driver_links")
                    Result.failure(Exception("HTTP Error: $responseCode"))
                }
            } catch (t: Throwable) {
                Log.e("RoutesRepository", "Exception calling /driver_links: ${t.message}", t)
                Result.failure(t)
            }
        }

   // fun lastSyncedAt(): Long? = System.currentTimeMillis()

    /**
     * Replaces a placeholder hostname ("host" or "localhost") in the report URL
     * with the real server address from [BaseUrl.URL], so the WebView can load it.
     * Needed only for local development and testing
     *
     * Example:
     *   "http://host:7080/static/reports/foo.html"
     *       → "http://37.27.195.216:7080/static/reports/foo.html"
     */
    private fun normalizeReportUrl(url: String): String {
        Log.d("RoutesRepository", "Normalizing report URL with base: ${BaseUrl.URL}")
        return url
            .replaceFirst("http://localhost:7080", BaseUrl.URL)
    }

    /**
     * Cheap stop-count: Parses the 'api=1' format and counts waypoints + destination.
     */
    private fun countStops(url: String): Int {
        if (!url.contains("api=1")) return 0
        
        var stops = 0
        
        // Check for origin
        if (url.contains("origin=")) stops++
        
        // Check for destination
        if (url.contains("destination=")) stops++
        
        // Count waypoints
        val waypointsIndex = url.indexOf("waypoints=")
        if (waypointsIndex >= 0) {
            val waypointsStr = url.substring(waypointsIndex + "waypoints=".length).substringBefore("&")
            if (waypointsStr.isNotEmpty()) {
                stops += waypointsStr.split("%7C", "|").size
            }
        }
        
        // Usually stop count = origin + destination + waypoints.
        // We typically report the number of *destinations* (waypoints + final dest), 
        // so we subtract 1 if origin exists so it represents "stops from here".
        // If the user wants the absolute total, we can return `stops`.
        // Let's return total stops.
        return if (stops > 0) stops else 0
    }
}
