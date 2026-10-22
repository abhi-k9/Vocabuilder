package io.github.abhik9.vocabuilder.core.domain.repository

import io.github.abhik9.vocabuilder.core.model.Answer
import io.github.abhik9.vocabuilder.core.model.Review
import io.github.abhik9.vocabuilder.core.model.WordProgress
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Stores what the learner knows of each word, and the answers they gave. */
interface ProgressRepository {
    /** The progress on a word, or `null` if it was never studied, starred or marked as known. */
    suspend fun getProgress(wordId: String): WordProgress?

    /** Saves the progress after an answer together with the answer itself. Returns the review's id. */
    suspend fun recordAnswer(progress: WordProgress, review: Review): Long

    /**
     * Takes an answer back: restores the progress the word had before it ([previous], or none) and
     * deletes the review.
     */
    suspend fun undoAnswer(wordId: String, previous: WordProgress?, reviewId: Long)

    suspend fun setStarred(wordId: String, starred: Boolean)

    suspend fun setAlreadyKnown(wordId: String, alreadyKnown: Boolean)

    /** Forgets all progress and answers for the words of a deck. */
    suspend fun resetDeck(deckId: String)

    /** Forgets all progress and answers. */
    suspend fun resetAll()

    /** Every answer given, oldest first, updated as answers are recorded. */
    fun observeAnswers(): Flow<List<Answer>>

    /** How many answers were given since [since], updated as answers are recorded. */
    fun observeReviewCountSince(since: Instant): Flow<Int>
}
