package io.github.abhik9.vocabuilder.feature.words

import io.github.abhik9.vocabuilder.core.model.PartOfSpeech
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.model.WordSummary
import kotlin.test.assertEquals
import org.junit.Test

class WordSearchTest {
    private fun word(headword: String, definition: String, progress: WordProgress = WordProgress("gre:$headword")) =
        WordSummary(
            id = "gre:$headword",
            deckId = "gre/common-01",
            headword = headword,
            position = 0,
            partOfSpeech = PartOfSpeech.ADJECTIVE,
            definition = definition,
            progress = progress,
        )

    private val words = listOf(
        word("laud", "to praise highly"),
        word("extol", "to praise enthusiastically", WordProgress("gre:extol", starred = true)),
        word("abate", "to become less intense"),
        word("bête noire", "a thing one especially dislikes"),
        word("applaud", "to show approval of", WordProgress("gre:applaud", stage = 5, reviewCount = 6)),
    )

    private fun search(query: String, filter: WordFilter = WordFilter.ALL) =
        searchWords(words, query, filter).map { it.headword }

    @Test
    fun listsEverythingAlphabeticallyWithoutAQuery() {
        assertEquals(listOf("abate", "applaud", "bête noire", "extol", "laud"), search(""))
    }

    @Test
    fun ranksPrefixMatchesThenHeadwordsThenMeanings() {
        assertEquals(listOf("laud", "applaud"), search("laud"))
        assertEquals(listOf("extol", "laud"), search("praise"))
    }

    @Test
    fun ignoresCaseAndAccents() {
        assertEquals(listOf("bête noire"), search("BETE"))
    }

    @Test
    fun filtersByStateAndStars() {
        assertEquals(listOf("extol"), search("", WordFilter.STARRED))
        assertEquals(listOf("applaud"), search("", WordFilter.MASTERED))
        assertEquals(listOf("abate", "bête noire", "extol", "laud"), search("", WordFilter.NEW))
    }
}
