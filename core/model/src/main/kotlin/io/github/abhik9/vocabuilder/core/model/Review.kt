package io.github.abhik9.vocabuilder.core.model

import java.time.Instant

/**
 * One answer to a card, kept to show history and statistics.
 *
 * @property id assigned when the review is saved; 0 before.
 */
data class Review(
    val wordId: String,
    val reviewedAt: Instant,
    val knew: Boolean,
    val mode: StudyMode,
    val stageBefore: Int,
    val stageAfter: Int,
    val id: Long = 0,
)

/** What a study session goes through. */
enum class StudyMode {
    /** The words of one deck. */
    DECK,

    /** Words due for review across all decks, plus some new ones. */
    DAILY_REVIEW,

    /** The words the learner starred. */
    STARRED,

    /** The words the learner missed most, until they're mastered. */
    TROUBLE,
}
