package io.github.abhik9.vocabuilder.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.abhik9.vocabuilder.core.database.model.AnswerRow
import io.github.abhik9.vocabuilder.core.database.model.ProgressEntity
import io.github.abhik9.vocabuilder.core.database.model.ReviewEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** The learner's progress on each word, and the answers they gave. */
@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE wordId = :wordId")
    suspend fun get(wordId: String): ProgressEntity?

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("DELETE FROM progress WHERE wordId = :wordId")
    suspend fun delete(wordId: String)

    @Insert
    suspend fun insertReview(review: ReviewEntity): Long

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReview(id: Long)

    /** Saves the progress after an answer and the answer itself. Returns the review's id. */
    @Transaction
    suspend fun recordAnswer(progress: ProgressEntity, review: ReviewEntity): Long {
        upsert(progress)
        return insertReview(review)
    }

    /** Restores the progress from before an answer ([previous], or none) and deletes its review. */
    @Transaction
    suspend fun undoAnswer(wordId: String, previous: ProgressEntity?, reviewId: Long) {
        if (previous == null) delete(wordId) else upsert(previous)
        deleteReview(reviewId)
    }

    @Transaction
    suspend fun setStarred(wordId: String, starred: Boolean) {
        upsert((get(wordId) ?: ProgressEntity(wordId)).copy(starred = starred))
    }

    @Transaction
    suspend fun setAlreadyKnown(wordId: String, alreadyKnown: Boolean) {
        upsert((get(wordId) ?: ProgressEntity(wordId)).copy(alreadyKnown = alreadyKnown))
    }

    @Transaction
    suspend fun resetDeck(deckId: String) {
        deleteDeckReviews(deckId)
        deleteDeckProgress(deckId)
    }

    @Query("DELETE FROM progress WHERE wordId IN (SELECT id FROM words WHERE deckId = :deckId)")
    suspend fun deleteDeckProgress(deckId: String)

    @Query("DELETE FROM reviews WHERE wordId IN (SELECT id FROM words WHERE deckId = :deckId)")
    suspend fun deleteDeckReviews(deckId: String)

    @Transaction
    suspend fun resetAll() {
        deleteAllReviews()
        deleteAllProgress()
    }

    @Query("DELETE FROM progress")
    suspend fun deleteAllProgress()

    @Query("DELETE FROM reviews")
    suspend fun deleteAllReviews()

    @Query("SELECT reviewedAt, knew FROM reviews ORDER BY reviewedAt")
    fun observeAnswers(): Flow<List<AnswerRow>>

    @Query("SELECT COUNT(*) FROM reviews WHERE reviewedAt >= :since")
    fun observeReviewCountSince(since: Instant): Flow<Int>
}
