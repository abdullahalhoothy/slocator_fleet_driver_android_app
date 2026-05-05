package com.slocator.fleetdriver.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun testPickDay_exactMatch() {
        val days = listOf(
            ScheduledDay("Day 1", LocalDate(2026, 5, 1), emptyList()),
            ScheduledDay("Day 2", LocalDate(2026, 5, 2), emptyList()),
            ScheduledDay("Day 3", LocalDate(2026, 5, 3), emptyList())
        )
        val today = LocalDate(2026, 5, 2)
        val result = DayResolver.pickDay(days, today)
        assertEquals(days[1], result)
    }

    @Test
    fun testPickDay_noMatch_returnsNull() {
        val days = listOf(
            ScheduledDay("Day 1", LocalDate(2026, 5, 1), emptyList()),
            ScheduledDay("Day 2", LocalDate(2026, 5, 2), emptyList())
        )
        val today = LocalDate(2026, 5, 3)
        val result = DayResolver.pickDay(days, today)
        assertNull("Should return null when there is no exact date match", result)
    }
}