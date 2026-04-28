package com.slocator.fleetdriver.data

import java.time.LocalDate

/**
 * One driver's complete schedule, as decoded from one sheet of the workbook.
 *
 * @param driverId    The sheet name from the XLSX. The user enters this verbatim
 *                    on the login screen; in production this will be a phone number.
 * @param days        Each day in the schedule, with its parsed date (when present)
 *                    and the parts (route segments) within it.
 */
data class DriverSchedule(
    val driverId: String,
    val days: List<ScheduledDay>
)

data class ScheduledDay(
    val dayLabel: String,            // e.g. "Day 1: 28/april/2026" — exactly as in the sheet header
    val date: LocalDate?,            // parsed from the label; null if header doesn't carry a date
    val parts: List<RoutePart>
)

data class RoutePart(
    val partNumber: Int,             // 1-based — Part 1, Part 2, ...
    val mapsUrl: String,             // Full Google Maps directions URL
    val stopCount: Int               // approximate; counted from URL waypoints
)
