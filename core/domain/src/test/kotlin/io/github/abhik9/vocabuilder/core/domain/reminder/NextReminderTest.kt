package io.github.abhik9.vocabuilder.core.domain.reminder

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class NextReminderTest {
    private val zone = ZoneId.of("America/New_York")

    private fun at(text: String) = ZonedDateTime.of(LocalDateTime.parse(text), zone)

    @Test
    fun comesTodayWhileItsTimeIsToComeElseTomorrow() {
        val seven = LocalTime.of(19, 0)

        assertEquals(at("2026-10-22T19:00"), nextReminderAt(at("2026-10-22T08:30"), seven))
        assertEquals(at("2026-10-23T19:00"), nextReminderAt(at("2026-10-22T19:00"), seven))
        assertEquals(at("2026-10-23T19:00"), nextReminderAt(at("2026-10-22T21:15"), seven))
    }

    @Test
    fun keepsItsTimeOfDayAcrossAChangeOfClocks() {
        // The clocks go back an hour on November 1, 2026, in New York.
        val next = nextReminderAt(at("2026-10-31T20:00"), LocalTime.of(19, 0))

        assertEquals(LocalTime.of(19, 0), next.toLocalTime())
        assertEquals(25, Duration.between(at("2026-10-31T19:00"), next).toHours())
    }
}
