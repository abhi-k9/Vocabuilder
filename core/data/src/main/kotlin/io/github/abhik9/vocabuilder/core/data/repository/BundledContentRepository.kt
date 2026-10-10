package io.github.abhik9.vocabuilder.core.data.repository

import io.github.abhik9.vocabuilder.core.common.di.AppDispatcher
import io.github.abhik9.vocabuilder.core.common.di.Dispatcher
import io.github.abhik9.vocabuilder.core.content.ContentLoader
import io.github.abhik9.vocabuilder.core.database.dao.ContentDao
import io.github.abhik9.vocabuilder.core.database.model.PackEntity
import io.github.abhik9.vocabuilder.core.database.model.asEntity
import io.github.abhik9.vocabuilder.core.database.model.senseEntities
import io.github.abhik9.vocabuilder.core.domain.repository.ContentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Copies the word packs bundled in the app into the database, when they changed since the last
 * copy: on first launch and after an update with new or corrected words.
 */
@Singleton
internal class BundledContentRepository @Inject constructor(
    private val loader: ContentLoader,
    private val contentDao: ContentDao,
    @param:Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ContentRepository {
    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val mutex = Mutex()

    override suspend fun syncBundledContent() = mutex.withLock {
        withContext(ioDispatcher) {
            try {
                loader.packIds().forEachIndexed { position, packId ->
                    if (contentDao.fingerprint(packId) != loader.fingerprint(packId)) copyPack(packId, position)
                }
            } finally {
                // Never keep the app waiting: with a broken pack, the others are still usable.
                _isReady.value = true
            }
        }
    }

    private suspend fun copyPack(packId: String, position: Int) {
        val bundled = loader.load(packId)
        contentDao.replacePack(
            pack = PackEntity(
                id = bundled.pack.id,
                name = bundled.pack.name,
                position = position,
                fingerprint = bundled.fingerprint,
            ),
            decks = bundled.decks.map { it.asEntity() },
            words = bundled.words.map { it.asEntity(packId) },
            senses = bundled.words.flatMap { it.senseEntities() },
        )
    }
}
