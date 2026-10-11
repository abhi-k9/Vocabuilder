package io.github.abhik9.vocabuilder.feature.study

import io.github.abhik9.vocabuilder.core.domain.study.AnswerCardUseCase
import io.github.abhik9.vocabuilder.core.domain.study.BuildStudyPlanUseCase
import io.github.abhik9.vocabuilder.core.domain.study.MarkKnownUseCase
import io.github.abhik9.vocabuilder.core.domain.study.StudyRequest
import io.github.abhik9.vocabuilder.core.domain.study.UndoAnswerUseCase
import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveDeckOverviewsUseCase
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.FakePronouncer
import io.github.abhik9.vocabuilder.core.testing.MainDispatcherRule
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.diagnostics.RecordingEventLog
import io.github.abhik9.vocabuilder.core.testing.repository.FakeUserSettingsRepository
import io.github.abhik9.vocabuilder.core.testing.repository.FakeWordRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class StudyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val words = FakeWordRepository.withProgress()
    private val progress = words.progressRepository
    private val settings = FakeUserSettingsRepository(UserSettings(shuffleCards = false))
    private val pronouncer = FakePronouncer()
    private val log = RecordingEventLog()

    private fun viewModel(request: StudyRequest, words: FakeWordRepository = this.words) = StudyViewModel(
        request = request,
        buildPlan = BuildStudyPlanUseCase(words, settings, TestData.clock),
        answerCard = AnswerCardUseCase(words.progressRepository, TestData.clock),
        undoAnswer = UndoAnswerUseCase(words.progressRepository),
        markKnown = MarkKnownUseCase(words.progressRepository),
        progressRepository = words.progressRepository,
        wordRepository = words,
        observeDeckOverviews = ObserveDeckOverviewsUseCase(words, TestData.clock),
        pronouncer = pronouncer,
        settingsRepository = settings,
        log = log,
    )

    private suspend fun StudyViewModel.state() = uiState.first { it != StudyUiState.Loading }

    @Test
    fun goesThroughTheDeckUntilEveryWordIsKnown() = runTest {
        val viewModel = viewModel(StudyRequest(StudyMode.DECK, TestData.advancedDeck.id))
        backgroundScope.launch { viewModel.uiState.collect {} }

        var state = assertIs<StudyUiState.Studying>(viewModel.state())
        assertEquals("pique", state.word.headword)
        assertEquals(0, state.doneCount)
        assertEquals(2, state.wordCount)
        assertFalse(state.isFlipped)

        viewModel.flip()
        assertTrue(assertIs<StudyUiState.Studying>(viewModel.uiState.value).isFlipped)

        viewModel.answer(knew = false)
        state = assertIs<StudyUiState.Studying>(viewModel.uiState.value)
        assertEquals("vex", state.word.headword)
        assertFalse(state.isFlipped)

        viewModel.answer(knew = true)
        assertEquals("pique", assertIs<StudyUiState.Studying>(viewModel.uiState.value).word.headword)

        viewModel.answer(knew = true)
        val finished = assertIs<StudyUiState.Finished>(viewModel.uiState.value)
        assertEquals(2, finished.summary.studied)
        assertEquals(1, finished.summary.knewFirstTime)
        assertEquals(1, finished.summary.missed)
        // The advanced deck is the last one.
        assertNull(finished.nextDeck)

        assertEquals(1, progress.getProgress("gre:pique")?.stage)
        assertEquals(3, progress.getProgress("gre:vex")?.stage)
        assertEquals(
            listOf(
                "study: started StudyRequest(mode=DECK, deckId=gre/advanced-01, practice=false), 2 cards",
                "study: gre:pique missed, stage new to 0, due 2026-10-10",
                "study: gre:vex known, stage new to 3, due 2026-10-17",
                "study: gre:pique known, stage 0 to 1, due 2026-10-11",
                "study: finished, SessionSummary(studied=2, knewFirstTime=1, missed=1, newlyMastered=0)",
            ),
            log.messages,
        )
    }

    @Test
    fun offersTheNextDeckWithWordsToStudy() = runTest {
        val viewModel = viewModel(StudyRequest(StudyMode.DECK, TestData.commonDeck.id))
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.state()

        repeat(TestData.commonWords.size) { viewModel.answer(knew = true) }

        assertEquals(TestData.advancedDeck, assertIs<StudyUiState.Finished>(viewModel.uiState.value).nextDeck)
    }

    @Test
    fun undoBringsBackTheLastCardAndItsProgress() = runTest {
        val viewModel = viewModel(StudyRequest(StudyMode.DECK, TestData.advancedDeck.id))
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.state()

        viewModel.answer(knew = true)
        viewModel.undo()

        val state = assertIs<StudyUiState.Studying>(viewModel.uiState.value)
        assertEquals("pique", state.word.headword)
        assertFalse(state.canUndo)
        assertNull(progress.getProgress("gre:pique"))
    }

    @Test
    fun aMissedWordThatComesStraightBackIsANewCard() = runTest {
        val viewModel = viewModel(StudyRequest(StudyMode.DECK, TestData.advancedDeck.id))
        backgroundScope.launch { viewModel.uiState.collect {} }
        assertEquals(0, assertIs<StudyUiState.Studying>(viewModel.state()).cardIndex)

        viewModel.answer(knew = true)
        var state = assertIs<StudyUiState.Studying>(viewModel.uiState.value)
        assertEquals("vex", state.word.headword)
        assertEquals(1, state.cardIndex)

        // The last card left, so it's the next one too.
        viewModel.flip()
        viewModel.answer(knew = false)
        state = assertIs<StudyUiState.Studying>(viewModel.uiState.value)
        assertEquals("vex", state.word.headword)
        assertEquals(2, state.cardIndex)
        assertFalse(state.isFlipped)

        viewModel.undo()
        assertEquals(1, assertIs<StudyUiState.Studying>(viewModel.uiState.value).cardIndex)
    }

    @Test
    fun aCaughtUpDeckIsEmpty() = runTest {
        progress.setProgress(
            *TestData.advancedWords.map {
                WordProgress(it.id, stage = 4, dueDay = TestData.TODAY.plusDays(10), reviewCount = 4)
            }.toTypedArray(),
        )

        val viewModel = viewModel(StudyRequest(StudyMode.DECK, TestData.advancedDeck.id))

        assertEquals(StudyUiState.Empty, viewModel.state())
    }

    @Test
    fun theTitleNamesTheDeck() = runTest {
        val viewModel = viewModel(StudyRequest(StudyMode.DECK, TestData.advancedDeck.id, practice = true))

        val title = viewModel.title.first { it.deck != null }

        assertEquals(TestData.advancedDeck, title.deck)
        assertTrue(title.isPractice)
    }

    @Test
    fun aKnownWordLeavesTheSessionUntilUndone() = runTest {
        val viewModel = viewModel(StudyRequest(StudyMode.DECK, TestData.advancedDeck.id))
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.state()

        viewModel.markKnown()

        assertEquals("vex", assertIs<StudyUiState.Studying>(viewModel.uiState.value).word.headword)
        assertEquals(true, progress.getProgress("gre:pique")?.alreadyKnown)

        viewModel.undo()

        assertEquals("pique", assertIs<StudyUiState.Studying>(viewModel.uiState.value).word.headword)
        assertNull(progress.getProgress("gre:pique"))
    }

    @Test
    fun starsTheCurrentWord() = runTest {
        val viewModel = viewModel(StudyRequest(StudyMode.DECK, TestData.advancedDeck.id))
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.state()

        viewModel.toggleStar()

        assertTrue(assertIs<StudyUiState.Studying>(viewModel.uiState.value).isStarred)
        assertEquals(true, progress.getProgress("gre:pique")?.starred)
    }

    @Test
    fun looksUpSynonymsAndAntonymsWithTheMeaningsOfThoseThatAreWordsOfTheApp() = runTest {
        val (pique, vex) = TestData.advancedWords
        val withRelatedWords = pique.copy(synonyms = listOf("vex", "irk"), antonyms = listOf("soothe"))
        val words = FakeWordRepository.withProgress(TestData.words.map { if (it == pique) withRelatedWords else it })
        val viewModel = viewModel(StudyRequest(StudyMode.DECK, TestData.advancedDeck.id), words)
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertEquals(setOf("vex"), assertIs<StudyUiState.Studying>(viewModel.state()).relatedWordsWithMeanings)

        viewModel.lookUp("vex")
        assertEquals(RelatedWord("vex", vex), viewModel.relatedWord.value)
        viewModel.pronounceRelatedWord()

        viewModel.lookUp("irk")
        assertEquals(RelatedWord("irk", null), viewModel.relatedWord.value)
        viewModel.pronounceRelatedWord()
        assertEquals(listOf("vex", "irk"), pronouncer.spoken)

        viewModel.dismissRelatedWord()
        assertNull(viewModel.relatedWord.value)
    }

    @Test
    fun saysTheWordWhenAskedOrWhenTurningTheCardIfEnabled() = runTest {
        settings.set(UserSettings(autoPronounce = true, shuffleCards = false))
        val viewModel = viewModel(StudyRequest(StudyMode.DECK, TestData.advancedDeck.id))
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.state()

        viewModel.pronounce()
        viewModel.flip()
        viewModel.flip()

        assertEquals(listOf("pique", "pique"), pronouncer.spoken)
    }
}
