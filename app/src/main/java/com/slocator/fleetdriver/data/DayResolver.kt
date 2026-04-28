package com.slocator.fleetdriver.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses dates out of column-header strings like:
 *    "Day 1: 28/april/2026"
 *    "Day 5 — 24 stops"          (legacy, no date)
 *    "Day 3: 26-04-2026"
 *    "اليوم 5: 28/أبريل/2026"     (Arabic equivalent — best-effort)
 *
 * The header format will harden to "Day N: D/month/YYYY" per the user's plan,
 * but we stay tolerant so an in-flight format change doesn't brick the app.
 */
object DayResolver {

    private val MONTHS_EN = mapOf(
        "january" to 1, "jan" to 1,
        "february" to 2, "feb" to 2,
        "march" to 3, "mar" to 3,
        "april" to 4, "apr" to 4,
        "may" to 5,
        "june" to 6, "jun" to 6,
        "july" to 7, "jul" to 7,
        "august" to 8, "aug" to 8,
        "september" to 9, "sep" to 9, "sept" to 9,
        "october" to 10, "oct" to 10,
        "november" to 11, "nov" to 11,
        "december" to 12, "dec" to 12
    )

    private val MONTHS_AR = mapOf(
        "يناير" to 1, "كانون الثاني" to 1,
        "فبراير" to 2, "شباط" to 2,
        "مارس" to 3, "آذار" to 3,
        "أبريل" to 4, "ابريل" to 4, "نيسان" to 4,
        "مايو" to 5, "أيار" to 5,
        "يونيو" to 6, "حزيران" to 6,
        "يوليو" to 7, "تموز" to 7,
        "أغسطس" to 8, "اغسطس" to 8, "آب" to 8,
        "سبتمبر" to 9, "أيلول" to 9,
        "أكتوبر" to 10, "اكتوبر" to 10, "تشرين الأول" to 10,
        "نوفمبر" to 11, "تشرين الثاني" to 11,
        "ديسمبر" to 12, "كانون الأول" to 12
    )

    private val FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-M-d", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ENGLISH)
    )

    fun parseHeaderDate(header: String?): LocalDate? {
        if (header.isNullOrBlank()) return null
        val text = header.trim()

        // 1) Try exact numeric formats with the substring after a colon, em-dash, or dash.
        val tail = text.substringAfter(':', missingDelimiterValue = text)
            .substringAfter('—', missingDelimiterValue = text)
            .trim()

        for (fmt in FORMATTERS) {
            try { return LocalDate.parse(tail.replace(" ", ""), fmt) } catch (_: Exception) {}
        }

        // 2) Try "D/monthName/YYYY" with English month names.
        val partsByDelim = tail.split('/', '-', ' ').filter { it.isNotBlank() }
        if (partsByDelim.size >= 3) {
            val day = partsByDelim[0].filter { it.isDigit() }.toIntOrNull()
            val monthName = partsByDelim[1].lowercase(Locale.ROOT)
            val year = partsByDelim[2].filter { it.isDigit() }.toIntOrNull()
            val month = MONTHS_EN[monthName] ?: MONTHS_AR[partsByDelim[1]]
            if (day != null && month != null && year != null) {
                return try { LocalDate.of(year, month, day) } catch (_: Exception) { null }
            }
        }

        return null
    }

    /**
     * Pick the schedule day matching the device's current local date.
     * Falls back to: same-or-after today's index if nothing matches exactly.
     */
    fun pickDay(days: List<ScheduledDay>, today: LocalDate): ScheduledDay? {
        if (days.isEmpty()) return null
        // 1) Exact-date match.
        days.firstOrNull { it.date == today }?.let { return it }
        // 2) Earliest day on/after today.
        val nextUp = days.filter { it.date != null && !it.date.isBefore(today) }
            .minByOrNull { it.date!!.toEpochDay() }
        if (nextUp != null) return nextUp
        // 3) If all dates are before today, grab the latest.
        val latest = days.filter { it.date != null }.maxByOrNull { it.date!!.toEpochDay() }
        return latest
    }
}
