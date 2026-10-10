package io.github.abhik9.vocabuilder.core.model

import java.time.Instant

/** One answer to a card, kept to show history and statistics. */
data class Review(
    val wordId: String,
    val reviewedAt: Instant,
    val knew: Boolean,
    val mode: StudyMode,
    val stageBefore: Int,
    val stageAfter: Int,
)

/** What a study session goes through. */
enum class StudyMode {
    /** The words of one deck. */
    DECK,

    /** Words due for review across all decks, plus some new ones. */
    DAILY_REVIEW,

    /** The words the learner starred. */
    STARRED,
}
