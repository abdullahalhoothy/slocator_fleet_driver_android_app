package com.slocator.fleetdriver.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class RoutesRepository(private val context: Context) {

    suspend fun fetchSchedule(driverPhone: String, managerPhone: String): Result<DriverSchedule> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("http://37.27.195.216:7080/driver_links")
                val conn = url.openConnection() as HttpURLConnection
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
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseStr = reader.readText()
                    reader.close()

                    val jsonResponse = JSONObject(responseStr)
                    val routesArray = jsonResponse.optJSONArray("routes")
                    
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
                        Result.success(DriverSchedule(driverId = driverPhone, days = daysList))
                    }
                } else {
                    Result.failure(Exception("HTTP Error: $responseCode"))
                }
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    fun lastSyncedAt(): Long? = System.currentTimeMillis()

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
