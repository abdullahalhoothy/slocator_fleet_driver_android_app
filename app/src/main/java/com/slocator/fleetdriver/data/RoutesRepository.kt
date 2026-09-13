package com.slocator.fleetdriver.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Read-only tracking endpoints:
 *
 *   GET /api/tracking/driver-id?phone=:string        -> DriverIdResponse
 *   GET /api/tracking/driver-routes/:driverId        -> RoutePlanOut[]
 *   GET /api/tracking/driver-territories/:driverId   -> TerritoryCustomersOut[]
 */
class RoutesRepository {

    /** Resolve the server-side driver UUID from the phone number entered at login. */
    suspend fun resolveDriverId(phone: String): Result<String> = withContext(Dispatchers.IO) {
        val query = URLEncoder.encode(phone, "UTF-8")
        getJson("/api/tracking/driver-id?phone=$query").mapCatching { body ->
            val driverId = JSONObject(body).optString("driver_id", "")
            if (driverId.isBlank()) throw Exception("Driver id missing in response")
            driverId
        }
    }

    /** Fetch and flatten every route plan belonging to [driverId]. */
    suspend fun fetchSchedule(driverId: String): Result<DriverSchedule> =
        withContext(Dispatchers.IO) {
            try {
                val path = "/api/tracking/driver-routes/${URLEncoder.encode(driverId, "UTF-8")}"
                val body = getJson(path).getOrElse { error ->
                    // A driver with nothing assigned yet may 404 here — that is an
                    // empty schedule, not a connectivity failure.
                    return@withContext if (error.message?.contains("HTTP Error: 404") == true) {
                        Result.success(DriverSchedule(driverId = driverId, days = emptyList()))
                    } else {
                        Result.failure(error)
                    }
                }

                val array = JSONArray(body)
                val plans = (0 until array.length()).map { parseRoutePlan(array.getJSONObject(it)) }
                // An empty plan list is a valid state (driver simply has no routes).
                Result.success(toSchedule(driverId, plans))
            } catch (t: Throwable) {
                Log.e("RoutesRepository", "Exception calling driver-routes: ${t.message}", t)
                Result.failure(t)
            }
        }

    /** Fetch the driver's territories (live only), each with its customer locations. */
    suspend fun fetchTerritories(driverId: String): Result<List<TerritoryCustomersOut>> =
        withContext(Dispatchers.IO) {
            try {
                val path = "/api/tracking/driver-territories/${URLEncoder.encode(driverId, "UTF-8")}"
                val body = getJson(path).getOrThrow()
                val array = JSONArray(body)
                Result.success(
                    (0 until array.length()).map { parseTerritoryCustomers(array.getJSONObject(it)) }
                )
            } catch (t: Throwable) {
                Log.e("RoutesRepository", "Exception calling driver-territories: ${t.message}", t)
                Result.failure(t)
            }
        }

    // ── mapping: server plans -> UI schedule ────────────────────────

    private fun toSchedule(driverId: String, plans: List<RoutePlanOut>): DriverSchedule {
        val days = mutableListOf<ScheduledDay>()
        for (plan in plans) {
            for (day in plan.days.sortedBy { it.dayNumber }) {
                // Stops that share a Google Maps route link belong to the same route,
                // so they collapse into one entry. Stops with no link are grouped too
                // and navigated via a generated directions link.
                val groups = LinkedHashMap<String?, MutableList<RouteStopOut>>()
                for (stop in day.stops.sortedBy { it.stopOrder }) {
                    val key = stop.gmapLink?.takeIf { it.isNotBlank() }
                    groups.getOrPut(key) { mutableListOf() }.add(stop)
                }

                val parts = groups.entries.mapIndexed { index, (link, stops) ->
                    val singleStop = stops.size == 1
                    RoutePart(
                        partNumber = index + 1,
                        mapsUrl = link ?: directionsUrlFor(stops),
                        stopCount = stops.size,
                        customerName = if (singleStop) {
                            stops.first().customerName.takeIf { it.isNotBlank() }
                        } else {
                            null
                        },
                        plannedArrivalTime = stops.first().plannedArrivalTime,
                        isMandatory = stops.all { it.isMandatory }
                    )
                }
                days.add(
                    ScheduledDay(
                        dayLabel = buildDayLabel(plan.planName, day.dayNumber),
                        date = DayResolver.parseHeaderDate(day.routeDate),
                        parts = parts,
                        routeDayId = day.routeDayId
                    )
                )
            }
        }
        return DriverSchedule(
            driverId = driverId,
            days = days,
            reportUrls = pickReportUrls(plans),
            driverName = plans.firstOrNull()?.driverName.orEmpty()
        )
    }

    private fun buildDayLabel(planName: String?, dayNumber: Int): String =
        if (planName.isNullOrBlank()) "Day $dayNumber" else "$planName · Day $dayNumber"

    private fun pickReportUrls(plans: List<RoutePlanOut>): ReportUrls {
        val plan = plans.firstOrNull { it.hasReports() } ?: plans.firstOrNull()
        return ReportUrls(
            routesMapUrl = plan?.routesMapUrl.normalized(),
            shopsMapUrl = plan?.shopsMapUrl.normalized(),
            clustersMapUrl = plan?.clustersMapUrl.normalized(),
            reportHtmlUrl = plan?.reportHtmlUrl.normalized()
        )
    }

    private fun RoutePlanOut.hasReports(): Boolean =
        !routesMapUrl.isNullOrBlank() || !shopsMapUrl.isNullOrBlank() ||
            !clustersMapUrl.isNullOrBlank() || !reportHtmlUrl.isNullOrBlank()

    /**
     * Builds a Google Maps *directions* link that visits every stop with known
     * coordinates, in order. Used when the server does not supply a `gmap_link`, so
     * the driver still gets route navigation instead of a single-location pin.
     * Returns null when none of the stops carry coordinates.
     */
    private fun directionsUrlFor(stops: List<RouteStopOut>): String? {
        val coords = stops.mapNotNull { stop ->
            val lat = stop.customerLat
            val lng = stop.customerLng
            if (lat == null || lng == null) null else "$lat,$lng"
        }
        if (coords.isEmpty()) return null

        val origin = coords.first()
        if (coords.size == 1) {
            return "https://www.google.com/maps/dir/?api=1&destination=$origin"
        }

        return buildString {
            append("https://www.google.com/maps/dir/?api=1")
            append("&origin=").append(origin)
            val waypoints = coords.subList(1, coords.size - 1)
            if (waypoints.isNotEmpty()) {
                append("&waypoints=").append(waypoints.joinToString("%7C"))
            }
            append("&destination=").append(coords.last())
        }
    }

    /**
     * The server may return a placeholder host ("host" / "localhost") during local
     * development; patch it to the real [BaseUrl.URL] so the WebView can load it.
     */
    private fun String?.normalized(): String? {
        val value = this?.takeIf { it.isNotBlank() } ?: return null
        return value
            .replaceFirst("http://localhost:7080", BaseUrl.URL)
            .replaceFirst("http://host:7080", BaseUrl.URL)
    }

    // ── JSON parsing ────────────────────────────────────────────────

    private fun parseRoutePlan(o: JSONObject): RoutePlanOut {
        val daysArr = o.optJSONArray("days") ?: JSONArray()
        return RoutePlanOut(
            routePlanId = o.optString("route_plan_id"),
            driverId = o.optString("driver_id"),
            scenarioId = o.optNullableString("scenario_id"),
            sourceRunId = o.optNullableString("source_run_id"),
            planName = o.optNullableString("plan_name"),
            periodStart = o.optString("period_start"),
            periodEnd = o.optString("period_end"),
            status = o.optString("status"),
            routesMapUrl = o.optNullableString("routes_map_url"),
            shopsMapUrl = o.optNullableString("shops_map_url"),
            clustersMapUrl = o.optNullableString("clusters_map_url"),
            shopsJsonUrl = o.optNullableString("shops_json_url"),
            reportHtmlUrl = o.optNullableString("report_html_url"),
            reportPdfUrl = o.optNullableString("report_pdf_url"),
            createdAt = o.optString("created_at"),
            updatedAt = o.optString("updated_at"),
            driverName = o.optString("driver_name"),
            days = (0 until daysArr.length()).map { parseRouteDay(daysArr.getJSONObject(it)) }
        )
    }

    private fun parseRouteDay(o: JSONObject): RouteDayOut {
        val stopsArr = o.optJSONArray("stops") ?: JSONArray()
        val segmentsArr = o.optJSONArray("segments") ?: JSONArray()
        return RouteDayOut(
            routeDayId = o.optString("route_day_id"),
            routePlanId = o.optString("route_plan_id"),
            dayNumber = o.optInt("day_number"),
            routeDate = o.optNullableString("route_date"),
            plannedStartTime = o.optNullableString("planned_start_time"),
            plannedEndTime = o.optNullableString("planned_end_time"),
            plannedDistanceKm = o.optNullableDouble("planned_distance_km"),
            plannedDurationMinutes = o.optNullableInt("planned_duration_minutes"),
            status = o.optString("status"),
            stops = (0 until stopsArr.length()).map { parseRouteStop(stopsArr.getJSONObject(it)) },
            segments = (0 until segmentsArr.length()).map { parseRouteSegment(segmentsArr.getJSONObject(it)) }
        )
    }

    private fun parseRouteStop(o: JSONObject) = RouteStopOut(
        stopId = o.optString("stop_id"),
        routeDayId = o.optString("route_day_id"),
        customerId = o.optString("customer_id"),
        assignmentId = o.optNullableString("assignment_id"),
        stopOrder = o.optInt("stop_order"),
        plannedArrivalTime = o.optNullableString("planned_arrival_time"),
        plannedDurationMinutes = o.optNullableInt("planned_duration_minutes"),
        gmapLink = o.optNullableString("gmap_link"),
        isMandatory = o.optBoolean("is_mandatory", true),
        stopStatus = o.optString("stop_status"),
        customerName = o.optString("customer_name"),
        customerLat = o.optNullableDouble("customer_lat"),
        customerLng = o.optNullableDouble("customer_lng")
    )

    private fun parseRouteSegment(o: JSONObject) = RouteSegmentOut(
        segmentId = o.optString("segment_id"),
        routeDayId = o.optString("route_day_id"),
        fromStopId = o.optNullableString("from_stop_id"),
        toStopId = o.optNullableString("to_stop_id"),
        sequenceOrder = o.optInt("sequence_order"),
        distanceKm = o.optNullableDouble("distance_km"),
        travelTimeMinutes = o.optNullableInt("travel_time_minutes"),
        polyline = o.optNullableString("polyline")
    )

    private fun parseTerritoryCustomers(o: JSONObject): TerritoryCustomersOut {
        val customersArr = o.optJSONArray("customers") ?: JSONArray()
        return TerritoryCustomersOut(
            territoryId = o.optString("territory_id"),
            scenarioId = o.optNullableString("scenario_id"),
            managerUserId = o.optNullableString("manager_user_id"),
            name = o.optString("name"),
            description = o.optNullableString("description"),
            colorHex = o.optNullableString("color_hex"),
            status = o.optString("status"),
            customerCount = o.optInt("customer_count"),
            serviceHours = o.optDouble("service_hours"),
            workloadPct = o.optDouble("workload_pct"),
            createdAt = o.optString("created_at"),
            updatedAt = o.optString("updated_at"),
            customers = (0 until customersArr.length()).map { parseCustomer(customersArr.getJSONObject(it)) }
        )
    }

    private fun parseCustomer(o: JSONObject) = CustomerOut(
        customerId = o.optString("customer_id"),
        name = o.optString("name"),
        address = o.optNullableString("address"),
        district = o.optNullableString("district"),
        postalCode = o.optNullableString("postal_code"),
        city = o.optNullableString("city"),
        country = o.optNullableString("country"),
        lat = o.optDouble("lat"),
        lng = o.optDouble("lng"),
        status = o.optString("status"),
        origin = o.optString("origin"),
        refPoiId = o.optString("ref_poi_id"),
        createdAt = o.optString("created_at"),
        updatedAt = o.optString("updated_at")
    )

    // ── HTTP ────────────────────────────────────────────────────────

    private fun getJson(path: String): Result<String> {
        return try {
            Log.d("RoutesRepository", "GET ${BaseUrl.URL}$path")
            val conn = URL("${BaseUrl.URL}$path").openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            if (code in 200..299) {
                val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                conn.disconnect()
                Result.success(body)
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP Error: $code"))
            }
        } catch (t: Throwable) {
            Log.e("RoutesRepository", "GET $path failed: ${t.message}", t)
            Result.failure(t)
        }
    }
}

// ── JSON null-safe helpers ──────────────────────────────────────────

private fun JSONObject.optNullableString(key: String): String? {
    if (isNull(key)) return null
    return optString(key, "").ifBlank { null }
}

private fun JSONObject.optNullableDouble(key: String): Double? {
    if (isNull(key) || !has(key)) return null
    return optDouble(key)
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (isNull(key) || !has(key)) return null
    return optInt(key)
}
