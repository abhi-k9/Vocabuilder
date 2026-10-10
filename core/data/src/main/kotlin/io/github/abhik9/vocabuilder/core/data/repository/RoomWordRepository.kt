package io.github.abhik9.vocabuilder.core.data.repository

import io.github.abhik9.vocabuilder.core.database.dao.ContentDao
import io.github.abhik9.vocabuilder.core.database.model.asExternalModel
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.Pack
import io.github.abhik9.vocabuilder.core.model.Word
import io.github.abhik9.vocabuilder.core.model.WordSummary
import io.github.abhik9.vocabuilder.core.model.WordWithProgress
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class RoomWordRepository @Inject constructor(
    private val contentDao: ContentDao,
) : WordRepository {
    override fun observePacks(): Flow<List<Pack>> =
        contentDao.observePacks().map { packs -> packs.map { it.asExternalModel() } }

    override fun observeDecks(): Flow<List<Deck>> =
        contentDao.observeDecks().map { decks -> decks.map { it.asExternalModel() } }.distinctUntilChanged()

    override fun observeWordSummaries(): Flow<List<WordSummary>> =
        contentDao.observeWordSummaries().map { rows -> rows.map { it.asExternalModel() } }.distinctUntilChanged()

    override fun observeWord(id: String): Flow<WordWithProgress?> =
        contentDao.observeWord(id).map { it?.asExternalModel() }.distinctUntilChanged()

    override suspend fun getWords(ids: List<String>): List<Word> {
        // SQLite limits the number of parameters in a query.
        val byId = ids.distinct().chunked(MAX_QUERY_PARAMETERS)
            .flatMap { chunk -> contentDao.getWords(chunk) }
            .associate { it.word.id to it.asExternalModel() }
        return ids.mapNotNull { byId[it] }
    }

    private companion object {
        const val MAX_QUERY_PARAMETERS = 500
    }
}
