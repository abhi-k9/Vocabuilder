package io.github.abhik9.vocabuilder.core.domain.repository

import io.github.abhik9.vocabuilder.core.model.DictionaryEntry

/** The meanings of the synonyms and antonyms that aren't words of the packs. */
interface DictionaryRepository {
    /** The dictionary's entry for a word, or `null` if it has none; case doesn't matter. */
    suspend fun getEntry(word: String): DictionaryEntry?

    /** Reads the dictionary ahead of the first lookup, so that it's quick. */
    suspend fun preload()
}
