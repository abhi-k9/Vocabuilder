package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.PartOfSpeech
import io.github.abhik9.vocabuilder.core.model.Sense
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.model.WordSummary
import io.github.abhik9.vocabuilder.core.testing.TestData
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuizChoicesTest {
    private fun summary(
        headword: String,
        pos: PartOfSpeech,
        deckId: String = "deck-1",
        definition: String = "to $headword",
    ) =
        WordSummary("gre:$headword", deckId, headword, 0, pos, definition, WordProgress("gre:$headword"))

    private val abate = TestData.word("abate").copy(
        deckId = "deck-1",
        senses = listOf(Sense(PartOfSpeech.VERB, "to lessen", "The storm abated.")),
        synonyms = listOf("wane"),
    )

    @Test
    fun hasTheWordAndThreeOthersOfItsDeckAndPartOfSpeechFirst() {
        val pool = listOf(
            summary("abate", PartOfSpeech.VERB, definition = "to lessen"),
            summary("laud", PartOfSpeech.VERB),
            summary("vex", PartOfSpeech.VERB),
            summary("goad", PartOfSpeech.VERB),
            summary("irk", PartOfSpeech.VERB, deckId = "deck-2"),
            summary("zeal", PartOfSpeech.NOUN),
        )

        repeat(20) { seed ->
            val choices = QuizChoices.of(abate, pool, Random(seed))

            assertEquals(QuizChoices.COUNT, choices.size)
            assertEquals(1, choices.count { it.wordId == abate.id })
            assertEquals(setOf("abate", "laud", "vex", "goad"), choices.map { it.headword }.toSet())
        }
    }

    @Test
    fun leavesOutSynonymsAndWordsWithTheSameMeaning() {
        val pool = listOf(
            summary("wane", PartOfSpeech.VERB),
            summary("ebb", PartOfSpeech.VERB, definition = "to lessen"),
            summary("laud", PartOfSpeech.VERB, definition = "to praise"),
            summary("extol", PartOfSpeech.VERB, definition = "to praise"),
            summary("zeal", PartOfSpeech.NOUN),
        )

        val choices = QuizChoices.of(abate, pool, Random(1))

        assertTrue(choices.none { it.headword == "wane" || it.headword == "ebb" }, choices.toString())
        // Two words with one meaning would leave no single right answer, so only one of them is a choice.
        assertEquals(1, choices.count { it.definition == "to praise" }, choices.toString())
        assertEquals(3, choices.size)
    }
}
