package io.github.abhik9.vocabuilder.core.content

import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.Level
import io.github.abhik9.vocabuilder.core.model.Pack
import io.github.abhik9.vocabuilder.core.model.PartOfSpeech
import io.github.abhik9.vocabuilder.core.model.Sense
import io.github.abhik9.vocabuilder.core.model.Word
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ContentValidatorTest {
    private val deck = Deck(id = "gre/common-01", packId = "gre", level = Level.COMMON, number = 1, position = 0)

    private fun word(
        headword: String = "abate",
        definition: String = "to become less intense",
        example: String = "The storm abated by morning.",
        synonyms: List<String> = listOf("subside"),
        antonyms: List<String> = emptyList(),
        note: String? = null,
        id: String = ContentLoader.wordId("gre", headword),
    ) = Word(
        id = id,
        deckId = deck.id,
        headword = headword,
        senses = listOf(Sense(PartOfSpeech.VERB, definition, example)),
        synonyms = synonyms,
        antonyms = antonyms,
        note = note,
    )

    private fun validate(vararg words: Word, decks: List<Deck> = listOf(deck)) =
        ContentValidator.validate(BundledPack(Pack("gre", "GRE"), decks, words.toList(), fingerprint = ""))

    @Test
    fun acceptsWellFormedWords() {
        assertEquals(emptyList(), validate(word(), word(headword = "bête noire", example = "Rain is my bête noire.")))
    }

    @Test
    fun flagsDuplicateWordsAndIds() {
        val problems =
            validate(word(), word(headword = "Abate", id = "gre:other"), word(headword = "wane", id = "gre:abate"))

        assertTrue(problems.any { "gre:abate: used by 2 words" in it }, problems.toString())
        assertTrue(problems.any { "\"abate\" appears 2 times" in it }, problems.toString())
    }

    @Test
    fun flagsDefinitionStyle() {
        assertTrue(validate(word(definition = "To become less intense")).any { "start in lowercase" in it })
        assertTrue(validate(word(definition = "to become less intense.")).any { "not end with a period" in it })
        assertTrue(validate(word(definition = "to abate")).any { "uses the word itself" in it })
        assertTrue(validate(word(definition = "to become  less intense")).any { "extra spaces" in it })
    }

    @Test
    fun flagsExamplesThatAreNotSentencesOrMissTheWord() {
        assertTrue(validate(word(example = "the storm abated.")).any { "should be a sentence" in it })
        assertTrue(validate(word(example = "The storm passed.")).any { "doesn't use the word" in it })
    }

    @Test
    fun flagsRelatedWords() {
        assertTrue(validate(word(synonyms = listOf("Subside"))).any { "should be lowercase" in it })
        assertTrue(validate(word(synonyms = listOf("abate"))).any { "lists itself" in it })
        assertTrue(validate(word(synonyms = listOf("wane", "wane"))).any { "repeats a word" in it })
        assertTrue(validate(word(antonyms = listOf("subside"))).any { "both synonyms and antonyms" in it })
    }

    @Test
    fun flagsNotesThatAreNotSentences() {
        assertTrue(validate(word(note = "from Latin")).any { "the note should be written as sentences" in it })
    }

    @Test
    fun flagsDeckNumberingAndOrder() {
        val advanced = Deck(id = "gre/advanced-02", packId = "gre", level = Level.ADVANCED, number = 2, position = 0)
        val problems = validate(word(), decks = listOf(advanced, deck))

        assertTrue(problems.any { "ADVANCED decks are numbered [2]" in it }, problems.toString())
        assertTrue(problems.any { "COMMON deck after a ADVANCED one" in it }, problems.toString())
        assertTrue(problems.any { "gre/advanced-02: has 0 words" in it }, problems.toString())
    }

    @Test
    fun recognizesFormsOfTheWord() {
        assertTrue(ContentValidator.exampleUsesWord("abate", "The noise abated."))
        assertTrue(ContentValidator.exampleUsesWord("deny", "She denied it."))
        assertTrue(ContentValidator.exampleUsesWord("irk", "It irked him."))
        assertTrue(ContentValidator.exampleUsesWord("naïve", "A naive plan."))
        assertTrue(ContentValidator.exampleUsesWord("ad hoc", "An ad hoc committee met."))
        assertFalse(ContentValidator.exampleUsesWord("ad hoc", "An ad-hoc committee met."))
        assertFalse(ContentValidator.exampleUsesWord("abate", "The noise faded."))
    }
}
