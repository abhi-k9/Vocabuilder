package io.github.abhik9.vocabuilder.core.testing.repository

import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.model.Answer
import io.github.abhik9.vocabuilder.core.model.Review
import io.github.abhik9.vocabuilder.core.model.WordProgress
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Keeps progress and reviews in memory. [wordsInDeck] maps deck ids to their words, for resets. */
class FakeProgressRepository(
    private val wordsInDeck: (String) -> Set<String> = { emptySet() },
) : ProgressRepository {
    private val _progress = MutableStateFlow<Map<String, WordProgress>>(emptyMap())
    val progress: StateFlow<Map<String, WordProgress>> = _progress.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private var nextReviewId = 1L

    fun setProgress(vararg progress: WordProgress) = _progress.update { current ->
        current + progress.associateBy { it.wordId }
    }

    override suspend fun getProgress(wordId: String): WordProgress? = _progress.value[wordId]

    override suspend fun recordAnswer(progress: WordProgress, review: Review): Long {
        val id = nextReviewId++
        _progress.update { it + (progress.wordId to progress) }
        _reviews.update { it + review.copy(id = id) }
        return id
    }

    override suspend fun undoAnswer(wordId: String, previous: WordProgress?, reviewId: Long) {
        _progress.update { if (previous == null) it - wordId else it + (wordId to previous) }
        _reviews.update { reviews -> reviews.filterNot { it.id == reviewId } }
    }

    override suspend fun setStarred(wordId: String, starred: Boolean) = edit(wordId) { it.copy(starred = starred) }

    override suspend fun setAlreadyKnown(wordId: String, alreadyKnown: Boolean) =
        edit(wordId) { it.copy(alreadyKnown = alreadyKnown) }

    override suspend fun resetDeck(deckId: String) {
        val ids = wordsInDeck(deckId)
        _progress.update { it - ids }
        _reviews.update { reviews -> reviews.filterNot { it.wordId in ids } }
    }

    override suspend fun resetAll() {
        _progress.value = emptyMap()
        _reviews.value = emptyList()
    }

    override fun observeAnswers(): Flow<List<Answer>> =
        reviews.map { reviews -> reviews.sortedBy { it.reviewedAt }.map { Answer(it.reviewedAt, it.knew) } }

    override fun observeReviewCountSince(since: Instant): Flow<Int> =
        reviews.map { reviews -> reviews.count { !it.reviewedAt.isBefore(since) } }

    private fun edit(wordId: String, change: (WordProgress) -> WordProgress) = _progress.update {
        it + (wordId to change(it[wordId] ?: WordProgress(wordId)))
    }
}
