package com.slocator.fleetdriver.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Owns fetching, caching, and parsing the routes XLSX.
 *
 * Cache strategy:
 *  - Write the downloaded bytes to filesDir/routes_cache.xlsx atomically (.tmp -> rename).
 *  - On startup we always *try* the network first with a short timeout, then fall back to cache.
 *  - This satisfies the "must work offline" App Store requirement without complicated sync logic.
 */
class RoutesRepository(private val context: Context) {

    private val cacheFile: File by lazy { File(context.filesDir, "routes_cache.xlsx") }

    suspend fun loadAndParse(forceRefresh: Boolean = false): Result<XlsxParser.Workbook> =
        withContext(Dispatchers.IO) {
            val networkResult = if (forceRefresh || !cacheFile.exists()) {
                downloadToCache()
            } else {
                // best-effort refresh, but we tolerate a network failure if cache is fresh
                downloadToCache().recover { Unit }
            }

            // Fall through to whatever bytes we have on disk.
            if (!cacheFile.exists()) {
                return@withContext Result.failure(
                    networkResult.exceptionOrNull() ?: IllegalStateException("No cached routes available")
                )
            }

            try {
                val wb = cacheFile.inputStream().use { XlsxParser.parse(it) }
                Result.success(wb)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    fun lastSyncedAt(): Long? =
        if (cacheFile.exists()) cacheFile.lastModified() else null

    /** Find a single driver's schedule. Sheet names are the driver IDs. */
    fun extractSchedule(workbook: XlsxParser.Workbook, driverId: String): DriverSchedule? {
        val target = driverId.trim()
        val sheet = workbook.sheets.firstOrNull { it.name.equals(target, ignoreCase = true) }
            ?: workbook.sheets.firstOrNull { normalize(it.name) == normalize(target) }
            ?: return null

        // Row 0 is the header (day labels). Subsequent rows are part 1, part 2, ...
        val rows = sheet.rows
        if (rows.isEmpty()) return DriverSchedule(sheet.name, emptyList())

        val headers = rows[0]
        val partRows = rows.drop(1)

        val days = headers.mapIndexedNotNull { col, label ->
            if (label.isBlank()) return@mapIndexedNotNull null
            val parts = partRows.mapIndexed { idx, row ->
                val cell = row.getOrNull(col).orEmpty().trim()
                if (cell.isBlank()) null
                else RoutePart(
                    partNumber = idx + 1,
                    mapsUrl = cell,
                    stopCount = countStops(cell)
                )
            }.filterNotNull()
            ScheduledDay(
                dayLabel = label,
                date = DayResolver.parseHeaderDate(label),
                parts = parts
            )
        }
        return DriverSchedule(driverId = sheet.name, days = days)
    }

    private fun normalize(s: String): String =
        s.trim().replace(Regex("[\\s\\-_+()]+"), "").lowercase()

    /**
     * Cheap stop-count: number of "/" segments in the maps URL after the host path.
     * E.g. "https://www.google.com/maps/dir/A/B/C/D/" -> 4 segments. We subtract the
     * origin (1) to report waypoint count, then add 1 for the destination.
     * The header label ("23 stops") is also a fine source if present, but we don't
     * always have it post-format-change.
     */
    private fun countStops(url: String): Int {
        val marker = "/maps/dir/"
        val idx = url.indexOf(marker)
        if (idx < 0) return 0
        val after = url.substring(idx + marker.length)
            .trim('/').trim()
        if (after.isEmpty()) return 0
        // Each "lat,lng" segment between slashes is one stop.
        return after.split('/').count { seg ->
            seg.contains(',') && seg.split(',').all { it.toDoubleOrNull() != null }
        }
    }

    private fun downloadToCache(): Result<Unit> = try {
        val url = URL(ROUTES_URL)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TimeUnit.SECONDS.toMillis(8).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(15).toInt()
            requestMethod = "GET"
            setRequestProperty("Accept", "*/*")
            setRequestProperty("User-Agent", "S-LocatorFleetDriver/1.0 (Android)")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                return Result.failure(IllegalStateException("HTTP $code"))
            }
            val tmp = File(context.filesDir, "routes_cache.xlsx.tmp")
            conn.inputStream.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            // atomic-ish rename
            if (cacheFile.exists()) cacheFile.delete()
            tmp.renameTo(cacheFile)
            Result.success(Unit)
        } finally {
            conn.disconnect()
        }
    } catch (t: Throwable) {
        Result.failure(t)
    }

    companion object {
        // The user-supplied source of truth. If this URL changes, edit here.
        const val ROUTES_URL =
            "http://37.27.195.216:7080/static/reports/SDLacH0vD1drZleCF2zxTigs3043_territory_20260424193332_routes.xlsx"
    }
}
