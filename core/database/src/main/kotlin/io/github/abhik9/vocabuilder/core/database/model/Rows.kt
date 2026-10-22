package io.github.abhik9.vocabuilder.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import io.github.abhik9.vocabuilder.core.model.Answer
import java.time.Instant

/** A word with its first sense and its progress, if any, for lists. */
data class WordSummaryRow(
    val id: String,
    val deckId: String,
    val headword: String,
    val position: Int,
    val partOfSpeech: String,
    val definition: String,
    @Embedded(prefix = "progress_") val progress: ProgressEntity?,
)

/** An answer's time and outcome, for history. */
data class AnswerRow(
    val reviewedAt: Instant,
    val knew: Boolean,
) {
    fun asExternalModel() = Answer(at = reviewedAt, knew = knew)
}

data class PopulatedWord(
    @Embedded val word: WordEntity,
    @Relation(parentColumn = "id", entityColumn = "wordId") val senses: List<SenseEntity>,
)

data class PopulatedWordWithProgress(
    @Embedded val word: WordEntity,
    @Relation(parentColumn = "id", entityColumn = "wordId") val senses: List<SenseEntity>,
    @Relation(parentColumn = "id", entityColumn = "wordId") val progress: ProgressEntity?,
)
