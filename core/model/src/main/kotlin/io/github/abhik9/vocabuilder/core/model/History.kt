package io.github.abhik9.vocabuilder.core.model

import java.time.Instant
import java.time.LocalDate

/** An answer to a card, as history counts it: when it was given, and whether the learner knew the word. */
data class Answer(
    val at: Instant,
    val knew: Boolean,
)

/** One day's answers. */
data class DayActivity(
    val day: LocalDate,
    val answers: Int,
    val knew: Int,
) {
    val missed: Int get() = answers - knew
}

/**
 * The learner's history, for the history screen.
 *
 * @property recentDays the last [RECENT_DAYS] days, oldest first, days without answers included.
 * @property currentStreak days in a row with answers, up to today. Until today ends, a streak up to yesterday still
 *   counts.
 * @property longestStreak the most days in a row with answers, ever.
 * @property totalAnswers answers ever given.
 * @property studiedWords words answered at least once.
 * @property mostMissed the words missed most, most first, at most [MOST_MISSED].
 */
data class History(
    val recentDays: List<DayActivity>,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalAnswers: Int,
    val studiedWords: Int,
    val mostMissed: List<WordSummary>,
) {
    val recentAnswers: Int get() = recentDays.sumOf { it.answers }

    val recentKnew: Int get() = recentDays.sumOf { it.knew }

    companion object {
        const val RECENT_DAYS = 28
        const val MOST_MISSED = 10
    }
}
