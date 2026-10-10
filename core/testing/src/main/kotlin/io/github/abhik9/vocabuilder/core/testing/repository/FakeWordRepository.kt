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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Serves [words] from memory, with the progress kept by [progressRepository]. */
class FakeWordRepository(
    private val words: List<Word> = TestData.words,
    private val decks: List<Deck> = TestData.decks,
    private val packs: List<Pack> = listOf(TestData.pack),
    val progressRepository: FakeProgressRepository = FakeProgressRepository(),
) : WordRepository {
    private val wordsFlow = MutableStateFlow(words)

    override fun observePacks(): Flow<List<Pack>> = flowOf(packs)

    override fun observeDecks(): Flow<List<Deck>> = flowOf(decks)

    override fun observeWordSummaries(): Flow<List<WordSummary>> =
        combine(wordsFlow, progressRepository.progress) { words, progress ->
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
