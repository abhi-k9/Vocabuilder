package io.github.abhik9.vocabuilder.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.abhik9.vocabuilder.core.database.model.DeckEntity
import io.github.abhik9.vocabuilder.core.database.model.PackEntity
import io.github.abhik9.vocabuilder.core.database.model.PopulatedWord
import io.github.abhik9.vocabuilder.core.database.model.PopulatedWordWithProgress
import io.github.abhik9.vocabuilder.core.database.model.SenseEntity
import io.github.abhik9.vocabuilder.core.database.model.WordEntity
import io.github.abhik9.vocabuilder.core.database.model.WordSummaryRow
import kotlinx.coroutines.flow.Flow

/** The word packs, decks and words, as copied from the bundled content. */
@Dao
interface ContentDao {
    @Query("SELECT fingerprint FROM packs WHERE id = :packId")
    suspend fun fingerprint(packId: String): String?

    @Query("SELECT * FROM packs ORDER BY position")
    fun observePacks(): Flow<List<PackEntity>>

    @Query(
        """
        SELECT decks.* FROM decks
        JOIN packs ON packs.id = decks.packId
        ORDER BY packs.position, decks.position
        """,
    )
    fun observeDecks(): Flow<List<DeckEntity>>

    @Query(
        """
        SELECT words.id, words.deckId, words.headword, words.position,
            senses.partOfSpeech, senses.definition,
            progress.wordId AS progress_wordId,
            progress.stage AS progress_stage,
            progress.dueDay AS progress_dueDay,
            progress.introducedDay AS progress_introducedDay,
            progress.lastReviewedAt AS progress_lastReviewedAt,
            progress.reviewCount AS progress_reviewCount,
            progress.lapseCount AS progress_lapseCount,
            progress.starred AS progress_starred,
            progress.alreadyKnown AS progress_alreadyKnown
        FROM words
        JOIN senses ON senses.wordId = words.id AND senses.senseIndex = 0
        LEFT JOIN progress ON progress.wordId = words.id
        """,
    )
    fun observeWordSummaries(): Flow<List<WordSummaryRow>>

    @Transaction
    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getWords(ids: List<String>): List<PopulatedWord>

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id")
    fun observeWord(id: String): Flow<PopulatedWordWithProgress?>

    /**
     * Replaces a pack's decks and words with new ones. Progress is kept: words that remain keep
     * theirs, and the progress of removed words stays in case they come back.
     */
    @Transaction
    suspend fun replacePack(
        pack: PackEntity,
        decks: List<DeckEntity>,
        words: List<WordEntity>,
        senses: List<SenseEntity>,
    ) {
        deleteSenses(pack.id)
        deleteWords(pack.id)
        deleteDecks(pack.id)
        insertDecks(decks)
        insertWords(words)
        insertSenses(senses)
        upsertPack(pack)
    }

    @Query("DELETE FROM senses WHERE wordId IN (SELECT id FROM words WHERE packId = :packId)")
    suspend fun deleteSenses(packId: String)

    @Query("DELETE FROM words WHERE packId = :packId")
    suspend fun deleteWords(packId: String)

    @Query("DELETE FROM decks WHERE packId = :packId")
    suspend fun deleteDecks(packId: String)

    @Insert
    suspend fun insertDecks(decks: List<DeckEntity>)

    @Insert
    suspend fun insertWords(words: List<WordEntity>)

    @Insert
    suspend fun insertSenses(senses: List<SenseEntity>)

    @Upsert
    suspend fun upsertPack(pack: PackEntity)
}
