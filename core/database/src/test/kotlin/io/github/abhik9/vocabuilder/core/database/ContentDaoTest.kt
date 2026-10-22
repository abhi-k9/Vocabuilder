package io.github.abhik9.vocabuilder.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.vocabuilder.core.database.DatabaseTestData.decks
import io.github.abhik9.vocabuilder.core.database.DatabaseTestData.pack
import io.github.abhik9.vocabuilder.core.database.DatabaseTestData.senses
import io.github.abhik9.vocabuilder.core.database.DatabaseTestData.words
import io.github.abhik9.vocabuilder.core.database.model.ProgressEntity
import io.github.abhik9.vocabuilder.core.database.model.asExternalModel
import io.github.abhik9.vocabuilder.core.model.Level
import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.model.PartOfSpeech
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentDaoTest {
    private lateinit var database: VocabuilderDatabase
    private val contentDao get() = database.contentDao()
    private val progressDao get() = database.progressDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            VocabuilderDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun storesAPackWithItsDecksAndWords() = runTest {
        contentDao.replacePack(pack, decks, words, senses)

        assertEquals("v1", contentDao.fingerprint("gre"))
        assertEquals(listOf("GRE"), contentDao.observePacks().first().map { it.name })
        val storedDecks = contentDao.observeDecks().first().map { it.asExternalModel() }
        assertEquals(listOf("gre/common-01", "gre/advanced-01"), storedDecks.map { it.id })
        assertEquals(Level.ADVANCED, storedDecks[1].level)
    }

    @Test
    fun wordsComeWithTheirSensesInOrder() = runTest {
        contentDao.replacePack(pack, decks, words, senses)

        val abate = contentDao.getWords(listOf("gre:abate")).single().asExternalModel()

        assertEquals(listOf(PartOfSpeech.VERB, PartOfSpeech.NOUN), abate.senses.map { it.partOfSpeech })
        assertEquals("meaning 1 of abate", abate.primarySense.definition)
        assertEquals(listOf("one", "two words"), abate.synonyms)
        assertEquals(emptyList(), abate.antonyms)
    }

    @Test
    fun summariesHaveTheFirstSenseAndAnyProgress() = runTest {
        contentDao.replacePack(pack, decks, words, senses)
        progressDao.upsert(ProgressEntity("gre:laud", stage = 5, dueDay = LocalDate.of(2026, 11, 1), reviewCount = 6))

        val summaries = contentDao.observeWordSummaries().first().map { it.asExternalModel() }.associateBy { it.id }

        assertEquals(3, summaries.size)
        assertEquals("meaning 1 of abate", summaries.getValue("gre:abate").definition)
        assertEquals(MasteryState.NEW, summaries.getValue("gre:abate").progress.masteryState)
        assertEquals(MasteryState.MASTERED, summaries.getValue("gre:laud").progress.masteryState)
        assertEquals(LocalDate.of(2026, 11, 1), summaries.getValue("gre:laud").progress.dueDay)
    }

    @Test
    fun replacingAPackKeepsProgress() = runTest {
        contentDao.replacePack(pack, decks, words, senses)
        progressDao.upsert(ProgressEntity("gre:abate", stage = 2, reviewCount = 3))

        val renamed = words[0].copy(headword = "abate", position = 5)
        contentDao.replacePack(pack.copy(fingerprint = "v2"), decks, listOf(renamed), senses(renamed))

        assertEquals("v2", contentDao.fingerprint("gre"))
        val summaries = contentDao.observeWordSummaries().first()
        assertEquals(listOf("gre:abate"), summaries.map { it.id })
        assertEquals(2, summaries.single().progress?.stage)
        assertEquals(2, progressDao.get("gre:abate")?.stage)
    }

    @Test
    fun observesAWordWithItsProgress() = runTest {
        contentDao.replacePack(pack, decks, words, senses)
        progressDao.setStarred("gre:vex", true)

        val vex = contentDao.observeWord("gre:vex").first()?.asExternalModel()

        assertEquals("vex", vex?.word?.headword)
        assertEquals(true, vex?.progress?.starred)
        assertNull(contentDao.observeWord("gre:missing").first())
    }
}
