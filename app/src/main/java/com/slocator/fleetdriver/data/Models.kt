package com.slocator.fleetdriver.data

import kotlinx.datetime.LocalDate

// ══════════════════════════════════════════════════════════════════
// UI / domain models
// ══════════════════════════════════════════════════════════════════

/**
 * One driver's complete schedule, flattened from the server's [RoutePlanOut] list.
 *
 * @param driverId  Server-assigned driver UUID.
 * @param days      Every day across every active plan, ordered by day number.
 * @param reportUrls Report links from the most relevant plan.
 */
data class DriverSchedule(
    val driverId: String,
    val days: List<ScheduledDay>,
    val reportUrls: ReportUrls = ReportUrls(null, null, null),
    val driverName: String = ""
)

data class ScheduledDay(
    val dayLabel: String,            // e.g. "Day 1" or "Week A · Day 2"
    val date: LocalDate?,            // parsed from route_date; null when the server omits it
    val parts: List<RoutePart>,
    val routeDayId: String? = null   // server route_day_id — sent to /start-route
)

/**
 * A single navigable stop on a day's route. The server exposes one `gmap_link`
 * per stop (rather than one deep link per multi-stop segment), so a "part" now
 * maps to a stop/customer.
 */
data class RoutePart(
    val partNumber: Int,             // 1-based order within the day
    val mapsUrl: String?,            // gmap_link for this stop; null when unavailable
    val stopCount: Int = 0,          // legacy total; 0 for a single-stop part
    val customerName: String? = null,
    val plannedArrivalTime: String? = null,  // ISO datetime
    val isMandatory: Boolean = true
)

/**
 * Report/asset links returned on a [RoutePlanOut].
 */
data class ReportUrls(
    val routesMapUrl: String?,
    val shopsMapUrl: String?,
    val clustersMapUrl: String?,
    val reportHtmlUrl: String? = null
)

// ══════════════════════════════════════════════════════════════════
// Server DTOs — mirror the API schema exactly.
// ══════════════════════════════════════════════════════════════════

// ══ POST /api/tracking/start-route ══════════════════════════════════
data class StartTrackingResponse(
    val sessionId: String
)

// ══ POST /api/tracking/location ═════════════════════════════════════
data class TrackingLocationResponse(
    val driverId: String,
    val sessionId: String?,               // null when the driver has no active session
    val transitions: List<ProximityTransition>
)

data class ProximityTransition(
    val customerId: String,
    val poiName: String,
    val poiLat: Double,
    val poiLng: Double,
    val eventType: String,                // "arrived" | "departed"
    val distanceM: Double
)

// ══ POST /api/tracking/note ═════════════════════════════════════════
data class TrackingNoteResponse(
    val eventId: Long
)

// ══ POST /api/tracking/end-route ════════════════════════════════════
data class EndTrackingResponse(
    val endedSessions: List<String>
)

// ══ GET /api/tracking/driver-id?phone= ══════════════════════════════
data class DriverIdResponse(
    val driverId: String
)

// ══ GET /api/tracking/driver-routes/{driver_id} ═════════════════════
data class RoutePlanOut(
    val routePlanId: String,
    val driverId: String,
    val scenarioId: String?,              // always null (live only)
    val sourceRunId: String?,
    val planName: String?,
    val periodStart: String,              // ISO date
    val periodEnd: String,                // ISO date
    val status: String,                   // draft | published | active | completed | archived
    val routesMapUrl: String?,
    val shopsMapUrl: String?,
    val clustersMapUrl: String?,
    val shopsJsonUrl: String?,
    val reportHtmlUrl: String?,
    val reportPdfUrl: String?,
    val createdAt: String,
    val updatedAt: String,
    val driverName: String,
    val days: List<RouteDayOut>
)

data class RouteDayOut(
    val routeDayId: String,
    val routePlanId: String,
    val dayNumber: Int,
    val routeDate: String?,               // ISO date
    val plannedStartTime: String?,        // ISO datetime
    val plannedEndTime: String?,          // ISO datetime
    val plannedDistanceKm: Double?,
    val plannedDurationMinutes: Int?,
    val status: String,
    val stops: List<RouteStopOut>,
    val segments: List<RouteSegmentOut>
)

data class RouteStopOut(
    val stopId: String,
    val routeDayId: String,
    val customerId: String,
    val assignmentId: String?,
    val stopOrder: Int,
    val plannedArrivalTime: String?,      // ISO datetime
    val plannedDurationMinutes: Int?,
    val gmapLink: String?,
    val isMandatory: Boolean,
    val stopStatus: String,
    val customerName: String,
    val customerLat: Double?,
    val customerLng: Double?
)

data class RouteSegmentOut(
    val segmentId: String,
    val routeDayId: String,
    val fromStopId: String?,              // null = outbound leg from driver home base
    val toStopId: String?,                // null = final leg back home
    val sequenceOrder: Int,
    val distanceKm: Double?,
    val travelTimeMinutes: Int?,
    val polyline: String?
)

// ══ GET /api/tracking/driver-territories/{driver_id} ════════════════
data class TerritoryOut(
    val territoryId: String,
    val scenarioId: String?,              // always null (live only)
    val managerUserId: String?,
    val name: String,
    val description: String?,
    val colorHex: String?,
    val status: String,                   // active | inactive | draft
    val customerCount: Int,
    val serviceHours: Double,
    val workloadPct: Double,              // 100 = average group
    val createdAt: String,
    val updatedAt: String
)

/** [TerritoryOut] plus the locations assigned to it. */
data class TerritoryCustomersOut(
    val territoryId: String,
    val scenarioId: String?,              // always null (live only)
    val managerUserId: String?,
    val name: String,
    val description: String?,
    val colorHex: String?,
    val status: String,                   // active | inactive | draft
    val customerCount: Int,
    val serviceHours: Double,
    val workloadPct: Double,              // 100 = average group
    val createdAt: String,
    val updatedAt: String,
    val customers: List<CustomerOut>
)

data class CustomerOut(
    val customerId: String,
    val name: String,
    val address: String?,
    val district: String?,
    val postalCode: String?,
    val city: String?,
    val country: String?,
    val lat: Double,
    val lng: Double,
    val status: String,                   // active | inactive | prospect | archived
    val origin: String,                   // user_upload | system_poi | manual | import
    val refPoiId: String,
    val createdAt: String,
    val updatedAt: String
) {
    /** Deep link that drops a pin on the customer's coordinates. */
    val mapsUrl: String get() = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
}
