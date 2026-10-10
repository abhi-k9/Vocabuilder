package io.github.abhik9.vocabuilder.core.data.repository

import io.github.abhik9.vocabuilder.core.database.dao.ProgressDao
import io.github.abhik9.vocabuilder.core.database.model.asEntity
import io.github.abhik9.vocabuilder.core.database.model.asExternalModel
import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.model.Review
import io.github.abhik9.vocabuilder.core.model.WordProgress
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class RoomProgressRepository @Inject constructor(
    private val progressDao: ProgressDao,
) : ProgressRepository {
    override suspend fun getProgress(wordId: String): WordProgress? = progressDao.get(wordId)?.asExternalModel()

    override suspend fun recordAnswer(progress: WordProgress, review: Review): Long =
        progressDao.recordAnswer(progress.asEntity(), review.copy(id = 0).asEntity())

    override suspend fun undoAnswer(wordId: String, previous: WordProgress?, reviewId: Long) =
        progressDao.undoAnswer(wordId, previous?.asEntity(), reviewId)

    override suspend fun setStarred(wordId: String, starred: Boolean) = progressDao.setStarred(wordId, starred)

    override suspend fun setAlreadyKnown(wordId: String, alreadyKnown: Boolean) =
        progressDao.setAlreadyKnown(wordId, alreadyKnown)

    override suspend fun resetDeck(deckId: String) = progressDao.resetDeck(deckId)

    override suspend fun resetAll() = progressDao.resetAll()

    override fun observeReviewCountSince(since: Instant): Flow<Int> = progressDao.observeReviewCountSince(since)
}
