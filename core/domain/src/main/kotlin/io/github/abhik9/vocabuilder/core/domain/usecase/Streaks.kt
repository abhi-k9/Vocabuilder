package io.github.abhik9.vocabuilder.core.domain.usecase

import io.github.abhik9.vocabuilder.core.model.Answer
import java.time.LocalDate
import java.time.ZoneId

/** The day, in [zone], the answer was given. */
internal fun Answer.day(zone: ZoneId): LocalDate = at.atZone(zone).toLocalDate()

/** The days in a row with answers up to [today], or, while today has none yet, up to yesterday. */
internal fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
    val last = if (today in days) today else today.minusDays(1)
    return generateSequence(last) { it.minusDays(1) }.takeWhile { it in days }.count()
}

/** The most days in a row among [days]. */
internal fun longestStreak(days: Set<LocalDate>): Int {
    var longest = 0
    var run = 0
    var previous: LocalDate? = null
    for (day in days.sorted()) {
        run = if (previous != null && day == previous.plusDays(1)) run + 1 else 1
        longest = maxOf(longest, run)
        previous = day
    }
    return longest
}
