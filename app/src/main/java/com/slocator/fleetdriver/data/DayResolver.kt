package com.slocator.fleetdriver.data

import kotlinx.datetime.LocalDate

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

//    private val FORMAT_DMY_SLASH = LocalDate.Format {
//        day()
//        char('/')
//        monthNumber()
//        char('/')
//        year()
//    }
//
//    private val FORMAT_DMY_DASH = LocalDate.Format {
//        day()
//        char('-')
//        monthNumber()
//        char('-')
//        year()
//    }
//
//    private val FORMAT_YMD_DASH = LocalDate.Format {
//        year()
//        char('-')
//        monthNumber()
//        char('-')
//        day()
//    }
//
//    private val FORMAT_DMY_DOT = LocalDate.Format {
//        day()
//        char('.')
//        monthNumber()
//        char('.')
//        year()
//    }
//
//    private val FORMATS = listOf(
//        FORMAT_DMY_SLASH,
//        FORMAT_DMY_DASH,
//        FORMAT_YMD_DASH,
//        FORMAT_DMY_DOT
//    )

    fun parseHeaderDate(header: String?): LocalDate? {
        if (header.isNullOrBlank()) return null
        val text = header.trim()

        // Try to find regex dd/mm/yyyy or dd-mm-yyyy or dd.mm.yyyy with alphabetic months
        // Support \p{L} for letters (Arabic, English, etc)
        val regex = Regex("""(\d{1,2})[/.\-](\p{L}+|\d{1,2})[/.\-](\d{4})""")
        val match = regex.find(text)
        if (match != null) {
            val (dayStr, monthStr, yearStr) = match.destructured
            val dayVal = dayStr.toIntOrNull()
            val yearVal = yearStr.toIntOrNull()
            val monthVal = monthStr.toIntOrNull() ?: MONTHS_EN[monthStr.lowercase()] ?: MONTHS_AR[monthStr]
            if (dayVal != null && monthVal != null && yearVal != null) {
                return try { LocalDate(yearVal, monthVal, dayVal) } catch (_: Exception) { null }
            }
        }

        // Try YYYY-MM-DD
        val regexYMD = Regex("""(\d{4})[/.\-](\d{1,2})[/.\-](\d{1,2})""")
        val matchYMD = regexYMD.find(text)
        if (matchYMD != null) {
            val (yearStr, monthStr, dayStr) = matchYMD.destructured
            val dayVal = dayStr.toIntOrNull()
            val yearVal = yearStr.toIntOrNull()
            val monthVal = monthStr.toIntOrNull()
            if (dayVal != null && monthVal != null && yearVal != null) {
                return try { LocalDate(yearVal, monthVal, dayVal) } catch (_: Exception) { null }
            }
        }

        return null
    }

    /**
     * Pick the schedule day matching the device's current local date.
     * Returns null if no exact date match is found.
     */
    fun pickDay(days: List<ScheduledDay>, today: LocalDate): ScheduledDay? {
        if (days.isEmpty()) return null
        
        // Exact-date match only.
        return days.firstOrNull { it.date == today }
    }
}
