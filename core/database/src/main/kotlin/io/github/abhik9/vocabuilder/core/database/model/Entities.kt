package io.github.abhik9.vocabuilder.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

// Content tables hold a copy of the bundled packs and are replaced when a pack changes. Progress
// and reviews refer to words by id without foreign keys, so they survive those replacements.

/**
 * @property fingerprint of the bundled files the pack was copied from.
 * @property position the pack's place in the bundled index.
 */
@Entity(tableName = "packs")
data class PackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int,
    val fingerprint: String,
)

@Entity(tableName = "decks", indices = [Index("packId")])
data class DeckEntity(
    @PrimaryKey val id: String,
    val packId: String,
    /** A [io.github.abhik9.vocabuilder.core.model.Level] name. */
    val level: String,
    val number: Int,
    val position: Int,
)

@Entity(tableName = "words", indices = [Index("deckId"), Index("packId")])
data class WordEntity(
    @PrimaryKey val id: String,
    val packId: String,
    val deckId: String,
    val headword: String,
    val position: Int,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val note: String?,
)

@Entity(tableName = "senses", primaryKeys = ["wordId", "senseIndex"])
data class SenseEntity(
    val wordId: String,
    /** The sense's place among the word's senses, from 0 for the most common. */
    val senseIndex: Int,
    /** A [io.github.abhik9.vocabuilder.core.model.PartOfSpeech] name. */
    val partOfSpeech: String,
    val definition: String,
    val example: String,
)

@Entity(tableName = "progress", indices = [Index("dueDay")])
data class ProgressEntity(
    @PrimaryKey val wordId: String,
    val stage: Int = 0,
    val dueDay: LocalDate? = null,
    val introducedDay: LocalDate? = null,
    val lastReviewedAt: Instant? = null,
    val reviewCount: Int = 0,
    val lapseCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val starred: Boolean = false,
    @ColumnInfo(defaultValue = "0") val alreadyKnown: Boolean = false,
)

@Entity(tableName = "reviews", indices = [Index("wordId"), Index("reviewedAt")])
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: String,
    val reviewedAt: Instant,
    val knew: Boolean,
    /** A [io.github.abhik9.vocabuilder.core.model.StudyMode] name. */
    val mode: String,
    val stageBefore: Int,
    val stageAfter: Int,
)
