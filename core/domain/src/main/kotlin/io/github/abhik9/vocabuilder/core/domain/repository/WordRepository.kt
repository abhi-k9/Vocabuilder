package io.github.abhik9.vocabuilder.core.domain.repository

import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.Pack
import io.github.abhik9.vocabuilder.core.model.Word
import io.github.abhik9.vocabuilder.core.model.WordSummary
import io.github.abhik9.vocabuilder.core.model.WordWithProgress
import kotlinx.coroutines.flow.Flow

/** The words to learn and the learner's progress on them. */
interface WordRepository {
    /** The packs, in order. */
    fun observePacks(): Flow<List<Pack>>

    /** Every deck, ordered by pack, then by position within the pack. */
    fun observeDecks(): Flow<List<Deck>>

    /** Every word with its first meaning and progress, updated whenever progress changes. */
    fun observeWordSummaries(): Flow<List<WordSummary>>

    /** A word with all its senses and its progress, or `null` if there's no such word. */
    fun observeWord(id: String): Flow<WordWithProgress?>

    /** The words with these ids, in the same order; ids without a word are left out. */
    suspend fun getWords(ids: List<String>): List<Word>
}
