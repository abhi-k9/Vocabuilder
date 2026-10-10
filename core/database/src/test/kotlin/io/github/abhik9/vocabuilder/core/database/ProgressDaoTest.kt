package io.github.abhik9.vocabuilder.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.vocabuilder.core.database.DatabaseTestData.decks
import io.github.abhik9.vocabuilder.core.database.DatabaseTestData.pack
import io.github.abhik9.vocabuilder.core.database.DatabaseTestData.senses
import io.github.abhik9.vocabuilder.core.database.DatabaseTestData.words
import io.github.abhik9.vocabuilder.core.database.model.ProgressEntity
import io.github.abhik9.vocabuilder.core.database.model.ReviewEntity
import java.time.Instant
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
class ProgressDaoTest {
    private lateinit var database: VocabuilderDatabase
    private val progressDao get() = database.progressDao()
    private val now = Instant.parse("2026-10-10T12:00:00Z")

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            VocabuilderDatabase::class.java,
        ).allowMainThreadQueries().build()
        database.contentDao().replacePack(pack, decks, words, senses)
    }

    @After
    fun tearDown() = database.close()

    private fun review(wordId: String, at: Instant = now) =
        ReviewEntity(wordId = wordId, reviewedAt = at, knew = true, mode = "DECK", stageBefore = 0, stageAfter = 3)

    private val progress = ProgressEntity(
        wordId = "gre:abate",
        stage = 3,
        dueDay = LocalDate.of(2026, 10, 17),
        introducedDay = LocalDate.of(2026, 10, 10),
        lastReviewedAt = now,
        reviewCount = 1,
    )

    @Test
    fun recordsAndUndoesAnAnswer() = runTest {
        val reviewId = progressDao.recordAnswer(progress, review("gre:abate"))

        assertEquals(progress, progressDao.get("gre:abate"))
        assertEquals(1, progressDao.observeReviewCountSince(now).first())

        progressDao.undoAnswer("gre:abate", previous = null, reviewId = reviewId)

        assertNull(progressDao.get("gre:abate"))
        assertEquals(0, progressDao.observeReviewCountSince(now).first())
    }

    @Test
    fun undoRestoresThePreviousProgress() = runTest {
        val previous = progress.copy(stage = 1)
        progressDao.upsert(previous)
        val reviewId = progressDao.recordAnswer(progress, review("gre:abate"))

        progressDao.undoAnswer("gre:abate", previous, reviewId)

        assertEquals(previous, progressDao.get("gre:abate"))
    }

    @Test
    fun starringAndMarkingKnownKeepTheRestOfTheProgress() = runTest {
        progressDao.setStarred("gre:laud", true)
        assertEquals(ProgressEntity("gre:laud", starred = true), progressDao.get("gre:laud"))

        progressDao.upsert(progress)
        progressDao.setAlreadyKnown("gre:abate", true)
        progressDao.setStarred("gre:abate", true)

        assertEquals(progress.copy(alreadyKnown = true, starred = true), progressDao.get("gre:abate"))
    }

    @Test
    fun resettingADeckForgetsOnlyItsWords() = runTest {
        progressDao.recordAnswer(progress, review("gre:abate"))
        progressDao.recordAnswer(progress.copy(wordId = "gre:vex"), review("gre:vex"))

        progressDao.resetDeck("gre/common-01")

        assertNull(progressDao.get("gre:abate"))
        assertEquals(3, progressDao.get("gre:vex")?.stage)
        assertEquals(1, progressDao.observeReviewCountSince(Instant.EPOCH).first())
    }

    @Test
    fun resettingAllForgetsEverything() = runTest {
        progressDao.recordAnswer(progress, review("gre:abate"))

        progressDao.resetAll()

        assertNull(progressDao.get("gre:abate"))
        assertEquals(0, progressDao.observeReviewCountSince(Instant.EPOCH).first())
    }

    @Test
    fun countsReviewsSinceAnInstant() = runTest {
        progressDao.insertReview(review("gre:abate", at = now.minusSeconds(3600)))
        progressDao.insertReview(review("gre:laud", at = now))

        assertEquals(1, progressDao.observeReviewCountSince(now.minusSeconds(60)).first())
        assertEquals(2, progressDao.observeReviewCountSince(Instant.EPOCH).first())
    }
}
