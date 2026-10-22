package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.StudyMode

/** What to study: a deck, the daily review, or the starred or trouble words. */
data class StudyRequest(
    val mode: StudyMode,
    /** The deck, for [StudyMode.DECK]. */
    val deckId: String? = null,
    /** Go through words even if they aren't due, when there's nothing left to study. */
    val practice: Boolean = false,
    /** Answer by picking the meaning, or the word, among a few, rather than with flashcards. */
    val quiz: Boolean = false,
)

/** The cards of a study session, in order. */
data class StudyPlan(val wordIds: List<String>)
