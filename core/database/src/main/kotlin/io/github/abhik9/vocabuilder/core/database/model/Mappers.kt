package io.github.abhik9.vocabuilder.core.database.model

import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.Level
import io.github.abhik9.vocabuilder.core.model.Pack
import io.github.abhik9.vocabuilder.core.model.PartOfSpeech
import io.github.abhik9.vocabuilder.core.model.Review
import io.github.abhik9.vocabuilder.core.model.Sense
import io.github.abhik9.vocabuilder.core.model.Word
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.model.WordSummary
import io.github.abhik9.vocabuilder.core.model.WordWithProgress

fun PackEntity.asExternalModel() = Pack(id = id, name = name)

fun DeckEntity.asExternalModel() = Deck(
    id = id,
    packId = packId,
    level = Level.valueOf(level),
    number = number,
    position = position,
)

fun Deck.asEntity() = DeckEntity(id = id, packId = packId, level = level.name, number = number, position = position)

fun Word.asEntity(packId: String) = WordEntity(
    id = id,
    packId = packId,
    deckId = deckId,
    headword = headword,
    position = position,
    synonyms = synonyms,
    antonyms = antonyms,
    note = note,
)

fun Word.senseEntities() = senses.mapIndexed { index, sense ->
    SenseEntity(
        wordId = id,
        senseIndex = index,
        partOfSpeech = sense.partOfSpeech.name,
        definition = sense.definition,
        example = sense.example,
    )
}

fun PopulatedWord.asExternalModel() = word.asExternalModel(senses)

fun PopulatedWordWithProgress.asExternalModel() = WordWithProgress(
    word = word.asExternalModel(senses),
    progress = progress?.asExternalModel() ?: WordProgress(word.id),
)

private fun WordEntity.asExternalModel(senses: List<SenseEntity>) = Word(
    id = id,
    deckId = deckId,
    headword = headword,
    senses = senses.sortedBy { it.senseIndex }.map {
        Sense(PartOfSpeech.valueOf(it.partOfSpeech), it.definition, it.example)
    },
    synonyms = synonyms,
    antonyms = antonyms,
    note = note,
    position = position,
)

fun WordSummaryRow.asExternalModel() = WordSummary(
    id = id,
    deckId = deckId,
    headword = headword,
    position = position,
    partOfSpeech = PartOfSpeech.valueOf(partOfSpeech),
    definition = definition,
    progress = progress?.asExternalModel() ?: WordProgress(id),
)

fun ProgressEntity.asExternalModel() = WordProgress(
    wordId = wordId,
    stage = stage,
    dueDay = dueDay,
    introducedDay = introducedDay,
    lastReviewed = lastReviewedAt,
    reviewCount = reviewCount,
    lapseCount = lapseCount,
    starred = starred,
    alreadyKnown = alreadyKnown,
)

fun WordProgress.asEntity() = ProgressEntity(
    wordId = wordId,
    stage = stage,
    dueDay = dueDay,
    introducedDay = introducedDay,
    lastReviewedAt = lastReviewed,
    reviewCount = reviewCount,
    lapseCount = lapseCount,
    starred = starred,
    alreadyKnown = alreadyKnown,
)

fun Review.asEntity() = ReviewEntity(
    id = id,
    wordId = wordId,
    reviewedAt = reviewedAt,
    knew = knew,
    mode = mode.name,
    stageBefore = stageBefore,
    stageAfter = stageAfter,
)
