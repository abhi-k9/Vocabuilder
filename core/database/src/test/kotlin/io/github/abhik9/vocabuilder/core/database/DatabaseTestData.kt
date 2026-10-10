package io.github.abhik9.vocabuilder.core.database

import io.github.abhik9.vocabuilder.core.database.model.DeckEntity
import io.github.abhik9.vocabuilder.core.database.model.PackEntity
import io.github.abhik9.vocabuilder.core.database.model.SenseEntity
import io.github.abhik9.vocabuilder.core.database.model.WordEntity

internal object DatabaseTestData {
    val pack = PackEntity(id = "gre", name = "GRE", position = 0, fingerprint = "v1")

    val decks = listOf(
        DeckEntity(id = "gre/common-01", packId = "gre", level = "COMMON", number = 1, position = 0),
        DeckEntity(id = "gre/advanced-01", packId = "gre", level = "ADVANCED", number = 1, position = 1),
    )

    fun word(headword: String, deckId: String = "gre/common-01", position: Int = 0) = WordEntity(
        id = "gre:$headword",
        packId = "gre",
        deckId = deckId,
        headword = headword,
        position = position,
        synonyms = listOf("one", "two words"),
        antonyms = emptyList(),
        note = null,
    )

    fun senses(word: WordEntity, count: Int = 1) = (0 until count).map { index ->
        SenseEntity(
            wordId = word.id,
            senseIndex = index,
            partOfSpeech = if (index == 0) "VERB" else "NOUN",
            definition = "meaning ${index + 1} of ${word.headword}",
            example = "An example of ${word.headword}.",
        )
    }

    val words = listOf(word("abate", position = 0), word("laud", position = 1), word("vex", "gre/advanced-01"))

    val senses = senses(words[0], count = 2) + senses(words[1]) + senses(words[2])
}
