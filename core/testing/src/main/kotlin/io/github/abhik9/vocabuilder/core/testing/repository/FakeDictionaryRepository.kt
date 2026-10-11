package io.github.abhik9.vocabuilder.core.testing.repository

import io.github.abhik9.vocabuilder.core.domain.repository.DictionaryRepository
import io.github.abhik9.vocabuilder.core.model.DictionaryEntry

class FakeDictionaryRepository(vararg entries: DictionaryEntry) : DictionaryRepository {
    private val entries = entries.associateBy { it.word.lowercase() }

    override suspend fun getEntry(word: String): DictionaryEntry? = entries[word.lowercase()]

    override suspend fun preload() = Unit
}
