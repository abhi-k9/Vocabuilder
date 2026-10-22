package io.github.abhik9.vocabuilder.core.model

/** A word as listed: its first meaning and the learner's progress. */
data class WordSummary(
    val id: String,
    val deckId: String,
    val headword: String,
    val position: Int,
    val partOfSpeech: PartOfSpeech,
    val definition: String,
    val progress: WordProgress,
)

/** How many words are in each mastery state. */
data class MasteryCounts(
    val new: Int = 0,
    val learning: Int = 0,
    val reviewing: Int = 0,
    val mastered: Int = 0,
) {
    val total: Int get() = new + learning + reviewing + mastered

    /** Words studied at least once, or marked as known. */
    val started: Int get() = total - new

    operator fun plus(other: MasteryCounts) = MasteryCounts(
        new = new + other.new,
        learning = learning + other.learning,
        reviewing = reviewing + other.reviewing,
        mastered = mastered + other.mastered,
    )

    companion object {
        fun of(states: Iterable<MasteryState>): MasteryCounts {
            var counts = MasteryCounts()
            states.forEach { state ->
                counts = when (state) {
                    MasteryState.NEW -> counts.copy(new = counts.new + 1)
                    MasteryState.LEARNING -> counts.copy(learning = counts.learning + 1)
                    MasteryState.REVIEWING -> counts.copy(reviewing = counts.reviewing + 1)
                    MasteryState.MASTERED -> counts.copy(mastered = counts.mastered + 1)
                }
            }
            return counts
        }
    }
}

/**
 * A deck with how far the learner has come.
 *
 * @property dueCount words due for review today.
 */
data class DeckOverview(
    val deck: Deck,
    val counts: MasteryCounts,
    val dueCount: Int,
) {
    /** Every word is known for weeks or marked as known. */
    val isComplete: Boolean get() = counts.total > 0 && counts.mastered == counts.total

    /** Nothing to study today: no new words and no reviews due. */
    val isCaughtUp: Boolean get() = counts.new == 0 && dueCount == 0
}

/**
 * Where the learner stands today, for the home screen.
 *
 * @property dueCount words due for review today.
 * @property newCount new words the daily review will introduce today.
 * @property reviewedToday answers given today.
 * @property counts all words by mastery state.
 * @property nextDeck the first deck with something to study, to continue with.
 * @property starredCount words the learner starred.
 * @property troubleCount words the learner missed most, not yet mastered.
 * @property streak days in a row with answers, up to today or, until today ends, yesterday.
 */
data class TodayOverview(
    val dueCount: Int,
    val newCount: Int,
    val reviewedToday: Int,
    val counts: MasteryCounts,
    val nextDeck: DeckOverview?,
    val starredCount: Int = 0,
    val troubleCount: Int = 0,
    val streak: Int = 0,
) {
    /** Cards in today's daily review. */
    val dailyReviewCount: Int get() = dueCount + newCount
}
