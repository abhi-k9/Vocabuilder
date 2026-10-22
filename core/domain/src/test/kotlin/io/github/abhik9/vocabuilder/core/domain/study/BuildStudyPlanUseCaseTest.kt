package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.TestData.TODAY
import io.github.abhik9.vocabuilder.core.testing.repository.FakeUserSettingsRepository
import io.github.abhik9.vocabuilder.core.testing.repository.FakeWordRepository
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class BuildStudyPlanUseCaseTest {
    private val words = FakeWordRepository.withProgress()
    private val progress = words.progressRepository
    private val inOrder = UserSettings(shuffleCards = false)
    private val settings = FakeUserSettingsRepository(inOrder)
    private val buildPlan = BuildStudyPlanUseCase(words, settings, TestData.clock)

    private suspend fun plan(request: StudyRequest) = buildPlan(request, Random(1)).wordIds

    private fun studied(id: String, stage: Int, dueDay: java.time.LocalDate) =
        WordProgress("gre:$id", stage = stage, dueDay = dueDay, reviewCount = 1, introducedDay = TODAY.minusDays(30))

    @Test
    fun aNewDeckHasAllItsWordsInOrder() = runTest {
        assertEquals(
            listOf("gre:abate", "gre:laud", "gre:wane", "gre:zeal"),
            plan(StudyRequest(StudyMode.DECK, TestData.commonDeck.id)),
        )
    }

    @Test
    fun aDeckPutsDueWordsFirstAndSkipsWordsNotDueOrKnown() = runTest {
        progress.setProgress(
            studied("laud", stage = 2, dueDay = TODAY.plusDays(3)),
            studied("wane", stage = 1, dueDay = TODAY),
            studied("zeal", stage = 0, dueDay = TODAY.minusDays(1)),
            WordProgress("gre:abate", alreadyKnown = true),
        )

        assertEquals(listOf("gre:zeal", "gre:wane"), plan(StudyRequest(StudyMode.DECK, TestData.commonDeck.id)))
    }

    @Test
    fun practiceGoesThroughEveryWordNotKnown() = runTest {
        progress.setProgress(
            studied("laud", stage = 2, dueDay = TODAY.plusDays(3)),
            WordProgress("gre:abate", alreadyKnown = true),
        )

        val plan = buildPlan(StudyRequest(StudyMode.DECK, TestData.commonDeck.id, practice = true))

        assertEquals(listOf("gre:laud", "gre:wane", "gre:zeal"), plan.wordIds)
        assertTrue(plan.isPractice)
    }

    @Test
    fun shufflesCardsByDefault() = runTest {
        settings.set(UserSettings())
        val inDeckOrder = TestData.commonWords.map { it.id }

        val plans = (1..20).map { seed ->
            buildPlan(StudyRequest(StudyMode.DECK, TestData.commonDeck.id), Random(seed)).wordIds
        }

        plans.forEach { assertEquals(inDeckOrder.toSet(), it.toSet()) }
        assertTrue(plans.any { it != inDeckOrder }, "Never shuffled")
    }

    @Test
    fun dailyReviewHasDueWordsThenNewOnesUpToTheDailyLimit() = runTest {
        settings.set(inOrder.copy(newWordsPerDay = 2))
        progress.setProgress(
            studied("vex", stage = 3, dueDay = TODAY),
            studied("laud", stage = 2, dueDay = TODAY.plusDays(1)),
        )

        assertEquals(listOf("gre:vex", "gre:abate", "gre:wane"), plan(StudyRequest(StudyMode.DAILY_REVIEW)))
    }

    @Test
    fun shuffledDailyReviewPicksNewWordsAtRandomFromTheDeckBeingLearned() = runTest {
        settings.set(UserSettings(newWordsPerDay = 2))
        progress.setProgress(studied("vex", stage = 3, dueDay = TODAY))
        val commonIds = TestData.commonWords.map { it.id }

        // Every seed picks two of the common deck's words, though the advanced deck has one left too.
        val plans = (1..20).map { seed -> buildPlan(StudyRequest(StudyMode.DAILY_REVIEW), Random(seed)).wordIds }
        plans.forEach { ids ->
            assertEquals(setOf("gre:vex"), ids.toSet() - commonIds.toSet())
            assertEquals(3, ids.size)
        }
        assertTrue(plans.map { it.toSet() }.distinct().size > 1, "Always the same new words")
        assertTrue(plans.any { it.first() != "gre:vex" }, "The review never mixed with the new words")
    }

    @Test
    fun dailyReviewCountsWordsAlreadyIntroducedToday() = runTest {
        settings.set(inOrder.copy(newWordsPerDay = 2))
        progress.setProgress(
            WordProgress("gre:abate", stage = 3, dueDay = TODAY.plusDays(7), reviewCount = 1, introducedDay = TODAY),
        )

        assertEquals(listOf("gre:laud"), plan(StudyRequest(StudyMode.DAILY_REVIEW)))
    }

    @Test
    fun starredWordsInDeckOrder() = runTest {
        progress.setProgress(
            WordProgress("gre:vex", starred = true),
            WordProgress("gre:laud", starred = true, alreadyKnown = true),
        )

        assertEquals(listOf("gre:laud", "gre:vex"), plan(StudyRequest(StudyMode.STARRED)))
    }

    @Test
    fun troubleWordsAreThoseMissedTwiceAndNotMasteredMostMissedFirst() = runTest {
        progress.setProgress(
            WordProgress("gre:vex", stage = 0, dueDay = TODAY, reviewCount = 4, lapseCount = 2),
            WordProgress("gre:laud", stage = 1, dueDay = TODAY, reviewCount = 6, lapseCount = 3),
            WordProgress("gre:wane", stage = 0, dueDay = TODAY, reviewCount = 2, lapseCount = 1),
            WordProgress("gre:zeal", stage = 4, dueDay = TODAY.plusDays(20), reviewCount = 9, lapseCount = 4),
        )

        assertEquals(listOf("gre:laud", "gre:vex"), plan(StudyRequest(StudyMode.TROUBLE)))
    }
}
