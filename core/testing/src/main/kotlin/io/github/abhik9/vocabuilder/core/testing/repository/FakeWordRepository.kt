package io.github.abhik9.vocabuilder.core.testing.repository

import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.Pack
import io.github.abhik9.vocabuilder.core.model.Word
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.model.WordSummary
import io.github.abhik9.vocabuilder.core.model.WordWithProgress
import io.github.abhik9.vocabuilder.core.testing.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Serves [words], in the test data's decks, from memory, with the progress kept by [progressRepository]. */
class FakeWordRepository private constructor(
    private val words: List<Word>,
    val progressRepository: FakeProgressRepository,
) : WordRepository {
    override fun observePacks(): Flow<List<Pack>> = flowOf(listOf(TestData.pack))

    override fun observeDecks(): Flow<List<Deck>> = flowOf(TestData.decks)

    override fun observeWordSummaries(): Flow<List<WordSummary>> = progressRepository.progress.map { progress ->
        words.map { word ->
            WordSummary(
                id = word.id,
                deckId = word.deckId,
                headword = word.headword,
                position = word.position,
                partOfSpeech = word.primarySense.partOfSpeech,
                definition = word.primarySense.definition,
                progress = progress[word.id] ?: WordProgress(word.id),
            )
        }
    }

    override fun observeWord(id: String): Flow<WordWithProgress?> = progressRepository.progress.map { progress ->
        words.firstOrNull { it.id == id }?.let { WordWithProgress(it, progress[id] ?: WordProgress(id)) }
    }

    override suspend fun getWords(ids: List<String>): List<Word> {
        val byId = words.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    companion object {
        /** A word repository and the progress repository behind it, sharing the same words. */
        fun withProgress(words: List<Word> = TestData.words): FakeWordRepository {
            val progress =
                FakeProgressRepository { deckId -> words.filter { it.deckId == deckId }.map { it.id }.toSet() }
            return FakeWordRepository(words = words, progressRepository = progress)
        }
    }
}
