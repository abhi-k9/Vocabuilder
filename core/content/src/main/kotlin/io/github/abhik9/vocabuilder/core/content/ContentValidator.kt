package io.github.abhik9.vocabuilder.core.content

import io.github.abhik9.vocabuilder.core.model.DictionaryEntry
import io.github.abhik9.vocabuilder.core.model.DictionarySource
import io.github.abhik9.vocabuilder.core.model.Meaning
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

    /**
     * Checks the dictionary against the packs: every synonym and antonym that isn't a word of the packs has an entry,
     * and every entry is one of those, once. Entries written for Vocabuilder follow the packs' style; those from
     * WordNet are kept as WordNet has them, so only their form is checked.
     */
    fun validateDictionary(entries: List<DictionaryEntry>, packs: List<BundledPack>): List<String> = buildList {
        val headwords = packs.flatMap { pack -> pack.words.map { normalize(it.headword) } }.toSet()
        val related = packs.flatMap { pack -> pack.words.flatMap { it.synonyms + it.antonyms } }
            .filter { normalize(it) !in headwords }
            .toSet()
        entries.groupBy { normalize(it.word) }.filterValues { it.size > 1 }.forEach { (word, duplicates) ->
            add("dictionary: \"$word\" has ${duplicates.size} entries")
        }
        entries.forEach { entry ->
            val where = "dictionary \"${entry.word}\""
            if (!entry.word.matches(RELATED_WORD)) add("$where: should be lowercase words")
            if (normalize(entry.word) in headwords) {
                add("$where: is a word of the packs, which shows its own meanings")
            } else if (entry.word !in related) {
                add("$where: isn't a synonym or antonym of any word of the packs")
            }
            checkDictionaryEntry(where, entry)
        }
        val defined = entries.map { it.word }.toSet()
        (related - defined).sorted().forEach { word ->
            add("\"$word\" is a synonym or antonym with no meaning in the dictionary")
        }
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
            checkWrittenSense("$where sense ${index + 1}", word.headword, sense)
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

    /** A sense written for Vocabuilder: in the packs' style, with an example using the word. */
    private fun MutableList<String>.checkWrittenSense(at: String, word: String, sense: Meaning) {
        val example = sense.example
        checkText(at, "definition", sense.definition)
        if (example == null) add("$at: has no example") else checkText(at, "example", example)
        if (!sense.definition.matches(DEFINITION)) {
            add("$at: the definition should start in lowercase and not end with a period")
        }
        if (normalizedTokens(sense.definition).any { it == normalize(word) }) {
            add("$at: the definition uses the word itself")
        }
        if (example == null) return
        if (!example.matches(SENTENCE)) add("$at: the example should be a sentence")
        if (!exampleUsesWord(word, example)) add("$at: the example doesn't use the word")
    }

    private fun MutableList<String>.checkDictionaryEntry(where: String, entry: DictionaryEntry) {
        if (entry.senses.isEmpty()) add("$where: has no senses")
        entry.senses.forEachIndexed { index, sense ->
            val at = "$where sense ${index + 1}"
            if (entry.source == DictionarySource.VOCABUILDER) {
                checkWrittenSense(at, entry.word, sense)
            } else {
                checkText(at, "definition", sense.definition)
                sense.example?.let { checkText(at, "example", it) }
            }
        }
        if (entry.senses.distinctBy { it.partOfSpeech to it.definition }.size < entry.senses.size) {
            add("$where: has the same sense twice")
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
     * Whether [example] uses [headword] or one of its forms ("abated" for "abate"). Single words need a word starting
     * with their stem; phrases must appear as they are, but for the forms of their first and last words ("abstained
     * from" for "abstain from") and any possessive for "one's" ("break his word" for "break one's word").
     */
    internal fun exampleUsesWord(headword: String, example: String): Boolean {
        val head = normalize(headword)
        if (head.any { !it.isLetter() }) return phrasePattern(head).containsMatchIn(normalize(example))
        val stemLength = when {
            head.length <= 3 -> head.length
            head.length == 4 -> 3
            else -> head.length - 2
        }
        val stem = head.take(stemLength)
        return normalizedTokens(example).any { it.startsWith(stem) }
    }

    private fun phrasePattern(phrase: String): Regex {
        val words = phrase.split(' ')
        val pattern = words.mapIndexed { index, word ->
            when {
                word == "one's" -> POSSESSIVE
                index == 0 && words.size > 1 -> Regex.escape(word) + "\\p{L}*"
                else -> Regex.escape(word)
            }
        }.joinToString(" ")
        return Regex("(?<!\\p{L})$pattern")
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
    private const val POSSESSIVE = "(?:one's|my|your|his|her|its|our|their)"
    private val SENTENCE = Regex("[\\p{Lu}\"“‘0-9].*[.!?][\"”’)]?")
}
