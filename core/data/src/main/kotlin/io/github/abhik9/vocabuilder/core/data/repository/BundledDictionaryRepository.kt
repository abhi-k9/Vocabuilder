package io.github.abhik9.vocabuilder.core.data.repository

import io.github.abhik9.vocabuilder.core.common.di.AppDispatcher
import io.github.abhik9.vocabuilder.core.common.di.Dispatcher
import io.github.abhik9.vocabuilder.core.common.diagnostics.EventLog
import io.github.abhik9.vocabuilder.core.content.ContentException
import io.github.abhik9.vocabuilder.core.content.ContentLoader
import io.github.abhik9.vocabuilder.core.domain.repository.DictionaryRepository
import io.github.abhik9.vocabuilder.core.model.DictionaryEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The dictionary bundled with the app. It's only ever read, and small, so it's read when first needed and kept in
 * memory rather than copied into the database as the packs are.
 */
@Singleton
internal class BundledDictionaryRepository @Inject constructor(
    private val loader: ContentLoader,
    @param:Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
    private val log: EventLog,
) : DictionaryRepository {
    private val mutex = Mutex()
    private var entries: Map<String, DictionaryEntry>? = null

    override suspend fun getEntry(word: String): DictionaryEntry? = entries()[word.lowercase()]

    override suspend fun preload() {
        entries()
    }

    private suspend fun entries(): Map<String, DictionaryEntry> = mutex.withLock {
        entries ?: withContext(ioDispatcher) { load() }.also { entries = it }
    }

    private fun load(): Map<String, DictionaryEntry> = try {
        val (loaded, duration) = measureTimedValue { loader.loadDictionary() }
        log.record { "dictionary: read ${loaded.size} words in ${duration.inWholeMilliseconds} ms" }
        loaded.associateBy { it.word.lowercase() }
    } catch (e: ContentException) {
        // Synonyms and antonyms can still be heard without their meanings.
        log.record { "dictionary: couldn't be read, ${e.message}" }
        emptyMap()
    }
}
