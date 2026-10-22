package io.github.abhik9.vocabuilder.core.domain.reminder

import java.time.LocalTime
import java.time.ZonedDateTime

/** Schedules the daily reminder to study. */
interface ReminderScheduler {
    /** Sets the reminder for the next [time] of day, replacing any set before. */
    fun schedule(time: LocalTime)

    fun cancel()
}

/**
 * When a reminder at [time] of day next comes after [now]: today if it's still to come, else tomorrow. On a day
 * the clocks go forward past [time], it comes just after the change.
 */
fun nextReminderAt(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
    val today = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
    return if (today.isAfter(now)) today else ZonedDateTime.of(now.toLocalDate().plusDays(1), time, now.zone)
}
