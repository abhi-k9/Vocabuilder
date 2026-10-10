package io.github.abhik9.vocabuilder.core.content

import io.github.abhik9.vocabuilder.core.model.Word
import java.text.Normalizer

/**
 * Checks a pack against the content rules, so mistakes in the word files are caught by the tests
 * rather than by learners. Returns one message per problem; an empty list means the pack is fine.
 */
object ContentValidator {
    const val MAX_DECK_SIZE = 60

    fun validate(pack: BundledPack): List<String> = buildList {
        if (pack.decks.isEmpty()) add("${pack.pack.id}: the pack has no decks")
        checkDecks(pack)
        checkUniqueness(pack)
        pack.words.forEach { checkWord(it) }
    }

    private fun MutableList<String>.checkDecks(pack: BundledPack) {
        val wordsByDeck = pack.words.groupBy { it.deckId }
        pack.decks.zipWithNext().forEach { (previous, deck) ->
            if (deck.level < previous.level) add("${deck.id}: ${deck.level} deck after a ${previous.level} one")
        }
        pack.decks.groupBy { it.level }.forEach { (level, decks) ->
            val numbers = decks.map { it.number }
            if (numbers != (1..decks.size).toList()) add("${pack.pack.id}: $level decks are numbered $numbers")
        }
        pack.decks.forEach { deck ->
            val size = wordsByDeck[deck.id]?.size ?: 0
            if (size !in 1..MAX_DECK_SIZE) add("${deck.id}: has $size words, expected 1 to $MAX_DECK_SIZE")
        }
    }

    private fun MutableList<String>.checkUniqueness(pack: BundledPack) {
        pack.words.groupBy { it.id }.filterValues { it.size > 1 }.forEach { (id, words) ->
            add("$id: used by ${words.size} words, in ${words.map { it.deckId }.distinct()}")
        }
        pack.words.groupBy { normalize(it.headword) }.filterValues { it.size > 1 }.forEach { (headword, words) ->
            add("\"$headword\" appears ${words.size} times, in ${words.map { it.deckId }.distinct()}")
        }
    }

    private fun MutableList<String>.checkWord(word: Word) {
        val where = "${word.deckId} \"${word.headword}\""
        if (!word.headword.matches(HEADWORD)) add("$where: the headword has unexpected characters or spacing")
        if (word.senses.isEmpty()) add("$where: has no senses")
        word.senses.forEachIndexed { index, sense ->
            val at = "$where sense ${index + 1}"
            checkText(at, "definition", sense.definition)
            checkText(at, "example", sense.example)
            if (!sense.definition.matches(DEFINITION)) {
                add("$at: the definition should start in lowercase and not end with a period")
            }
            if (normalizedTokens(sense.definition).any { it == normalize(word.headword) }) {
                add("$at: the definition uses the word itself")
            }
            if (!sense.example.matches(SENTENCE)) add("$at: the example should be a sentence")
            if (!exampleUsesWord(word.headword, sense.example)) add("$at: the example doesn't use the word")
        }
        if (word.senses.distinctBy { it.partOfSpeech to it.definition }.size < word.senses.size) {
            add("$where: has the same sense twice")
        }
        checkRelatedWords(where, "synonyms", word.synonyms, word.headword)
        checkRelatedWords(where, "antonyms", word.antonyms, word.headword)
        val both = word.synonyms.intersect(word.antonyms.toSet())
        if (both.isNotEmpty()) add("$where: $both are both synonyms and antonyms")
        word.note?.let { note ->
            checkText(where, "note", note)
            if (!note.matches(SENTENCE)) add("$where: the note should be written as sentences")
        }
    }

    private fun MutableList<String>.checkText(where: String, field: String, text: String) {
        if (text.isBlank()) {
            add("$where: the $field is empty")
        } else if (text != text.trim() || "  " in text || '\n' in text) {
            add("$where: the $field has extra spaces or line breaks")
        }
    }

    private fun MutableList<String>.checkRelatedWords(
        where: String,
        field: String,
        words: List<String>,
        headword: String,
    ) {
        words.forEach { word ->
            if (!word.matches(RELATED_WORD)) add("$where: \"$word\" in $field should be lowercase words")
            if (normalize(word) == normalize(headword)) add("$where: lists itself in $field")
        }
        if (words.distinct().size < words.size) add("$where: repeats a word in $field")
    }

    /**
     * Whether [example] uses [headword] or one of its forms ("abated" for "abate"). Phrases must
     * appear as they are; single words need a word starting with their stem.
     */
    internal fun exampleUsesWord(headword: String, example: String): Boolean {
        val head = normalize(headword)
        if (head.any { !it.isLetter() }) return head in normalize(example)
        val stemLength = when {
            head.length <= 3 -> head.length
            head.length == 4 -> 3
            else -> head.length - 2
        }
        val stem = head.take(stemLength)
        return normalizedTokens(example).any { it.startsWith(stem) }
    }

    private fun normalizedTokens(text: String): List<String> = normalize(
        text,
    ).split(NON_LETTERS).filter { it.isNotEmpty() }

    private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(MARKS, "")
        .lowercase()

    private val MARKS = Regex("\\p{M}+")
    private val NON_LETTERS = Regex("[^\\p{L}]+")
    private val HEADWORD = Regex("\\p{L}+(?:[ '-]\\p{L}+)*")
    private val RELATED_WORD = Regex("\\p{Ll}+(?:[ '-]\\p{Ll}+)*")
    private val DEFINITION = Regex("[\\p{Ll}(].*[^.]")
    private val SENTENCE = Regex("[\\p{Lu}\"“‘0-9].*[.!?][\"”’)]?")
}
