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
    private val settings = FakeUserSettingsRepository()
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
    fun shufflesDecksWhenAsked() = runTest {
        settings.set(UserSettings(shuffleCards = true))

        val ids = plan(StudyRequest(StudyMode.DECK, TestData.commonDeck.id))

        assertEquals(TestData.commonWords.map { it.id }.toSet(), ids.toSet())
    }

    @Test
    fun dailyReviewHasDueWordsThenNewOnesUpToTheDailyLimit() = runTest {
        settings.set(UserSettings(newWordsPerDay = 2))
        progress.setProgress(
            studied("vex", stage = 3, dueDay = TODAY),
            studied("laud", stage = 2, dueDay = TODAY.plusDays(1)),
        )

        assertEquals(listOf("gre:vex", "gre:abate", "gre:wane"), plan(StudyRequest(StudyMode.DAILY_REVIEW)))
    }

    @Test
    fun dailyReviewCountsWordsAlreadyIntroducedToday() = runTest {
        settings.set(UserSettings(newWordsPerDay = 2))
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
}
