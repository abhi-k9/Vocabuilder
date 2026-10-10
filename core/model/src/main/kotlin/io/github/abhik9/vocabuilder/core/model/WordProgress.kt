package io.github.abhik9.vocabuilder.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * What the learner knows of a word. A word without progress has never been studied.
 *
 * @property stage how far along the review schedule the word is, from 0 (just missed or being
 * learned) up; see the scheduler for the interval of each stage.
 * @property dueDay the day the word should be reviewed next.
 * @property introducedDay the day the word was first studied, to count new words per day.
 * @property reviewCount answers given for the word, right or wrong.
 * @property lapseCount answers of "didn't know".
 * @property starred marked by the learner to study again.
 * @property alreadyKnown marked by the learner as known, so it's left out of study sessions.
 */
data class WordProgress(
    val wordId: String,
    val stage: Int = 0,
    val dueDay: LocalDate? = null,
    val introducedDay: LocalDate? = null,
    val lastReviewed: Instant? = null,
    val reviewCount: Int = 0,
    val lapseCount: Int = 0,
    val starred: Boolean = false,
    val alreadyKnown: Boolean = false,
) {
    /** Never answered: a new word, even if it was starred or marked as known. */
    val isNew: Boolean get() = reviewCount == 0

    val masteryState: MasteryState
        get() = when {
            alreadyKnown || stage >= MASTERED_STAGE -> MasteryState.MASTERED
            isNew -> MasteryState.NEW
            stage == 0 -> MasteryState.LEARNING
            else -> MasteryState.REVIEWING
        }

    /** Whether the word should be reviewed on [today]: it was studied and its review day came. */
    fun isDue(today: LocalDate): Boolean = !alreadyKnown && !isNew && dueDay != null && !dueDay.isAfter(today)

    companion object {
        /** From this stage on, a word is reviewed weeks apart and counts as mastered. */
        const val MASTERED_STAGE = 4
    }
}

/** The learner's grasp of a word, as shown in progress bars and lists. */
enum class MasteryState {
    /** Never studied. */
    NEW,

    /** Missed the last time, being learned again. */
    LEARNING,

    /** Known, reviewed at growing intervals. */
    REVIEWING,

    /** Known for weeks, or marked as already known. */
    MASTERED,
}

/** A word with its progress, or a fresh progress for a word never studied. */
data class WordWithProgress(
    val word: Word,
    val progress: WordProgress,
)
