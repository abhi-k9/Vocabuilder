package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.StudyMode

/** What to study: a deck, the daily review or the starred words. */
data class StudyRequest(
    val mode: StudyMode,
    /** The deck, for [StudyMode.DECK]. */
    val deckId: String? = null,
    /** Go through words even if they aren't due, when there's nothing left to study. */
    val practice: Boolean = false,
)

/**
 * The cards of a study session, in order.
 *
 * @property isPractice the words aren't due: answering "knew it" won't move them along their
 * schedule, but missing one still does.
 */
data class StudyPlan(
    val request: StudyRequest,
    val wordIds: List<String>,
    val isPractice: Boolean = false,
) {
    val isEmpty: Boolean get() = wordIds.isEmpty()
}
