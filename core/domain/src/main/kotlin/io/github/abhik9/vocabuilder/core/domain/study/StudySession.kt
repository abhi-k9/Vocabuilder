package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.model.WordProgress

/**
 * The cards left in a study session and the answers given so far, as in Magoosh's decks: a missed
 * card comes back a few cards later, until the learner knows it. Immutable: each answer returns a
 * new session.
 *
 * @property queue the cards still to answer; the first is the current card.
 */
data class StudySession(
    val queue: List<String>,
    val answers: List<SessionAnswer> = emptyList(),
) {
    val currentWordId: String? get() = queue.firstOrNull()

    val isFinished: Boolean get() = queue.isEmpty()

    /** Distinct words in the session, answered or not. */
    val wordCount: Int get() = (queue + answers.map { it.wordId }).distinct().size

    /** Distinct words answered "knew it", so done for this session. */
    val doneCount: Int get() = answers.filter { it.knew }.map { it.wordId }.distinct().size

    val canUndo: Boolean get() = answers.isNotEmpty()

    val lastAnswer: SessionAnswer? get() = answers.lastOrNull()

    /**
     * Answers the current card. A missed card goes back into the queue [REQUEUE_GAP] cards later,
     * or at the end if fewer are left.
     */
    fun answer(answer: SessionAnswer): StudySession {
        val wordId = checkNotNull(currentWordId) { "The session is finished" }
        require(answer.wordId == wordId) { "Answered ${answer.wordId}, but the current card is $wordId" }
        val rest = queue.drop(1)
        val next = if (answer.knew) {
            rest
        } else {
            val index = REQUEUE_GAP.coerceAtMost(rest.size)
            rest.take(index) + wordId + rest.drop(index)
        }
        return StudySession(queue = next, answers = answers + answer.copy(queueBefore = queue))
    }

    /** Takes back the last answer, putting its card back on top. */
    fun undo(): StudySession {
        val last = answers.lastOrNull() ?: return this
        return StudySession(queue = last.queueBefore, answers = answers.dropLast(1))
    }

    fun summary(): SessionSummary {
        val byWord = answers.groupBy { it.wordId }
        return SessionSummary(
            studied = byWord.size,
            knewFirstTime = byWord.values.count { it.first().knew },
            missed = byWord.values.count { list -> list.any { !it.knew } },
            newlyMastered = byWord.values.count { list ->
                list.first().before?.masteryState != MasteryState.MASTERED &&
                    list.last().after.masteryState == MasteryState.MASTERED
            },
        )
    }

    companion object {
        /** How many cards come between a missed card and its next showing. */
        const val REQUEUE_GAP = 4
    }
}

/**
 * An answer given in a session, with what's needed to take it back.
 *
 * @property before the word's progress before the answer, or `null` if it had none.
 * @property after the word's progress after the answer.
 * @property reviewId the id of the saved review.
 * @property queueBefore the session's queue when the card was answered.
 */
data class SessionAnswer(
    val wordId: String,
    val knew: Boolean,
    val before: WordProgress?,
    val after: WordProgress,
    val reviewId: Long,
    val queueBefore: List<String> = emptyList(),
)

/** How a session went, for the summary at its end. */
data class SessionSummary(
    val studied: Int,
    val knewFirstTime: Int,
    val missed: Int,
    val newlyMastered: Int,
)
