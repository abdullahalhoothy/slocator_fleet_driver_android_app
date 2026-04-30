package com.slocator.fleetdriver.data

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.datetime.LocalDate

class DayResolverTest {
    @Test
    fun testParseHeaderDate() {
        val cases = listOf(
            "Day 1: 29/April/2026 — 31 stops" to LocalDate(2026, 4, 29),
            "اليوم 5: 28/أبريل/2026" to LocalDate(2026, 4, 28),
            "Day 3: 26-04-2026" to LocalDate(2026, 4, 26),
            "Day 2: 30/April/2026 — 31 stops" to LocalDate(2026, 4, 30),
            "day x 20/04/2026 31stops" to LocalDate(2026, 4, 20),
            "day 1 20/04/2026 31stops" to LocalDate(2026, 4, 20)
        )
        for ((header, expected) in cases) {
            assertEquals("Failed for $header", expected, DayResolver.parseHeaderDate(header))
        }
    }
}