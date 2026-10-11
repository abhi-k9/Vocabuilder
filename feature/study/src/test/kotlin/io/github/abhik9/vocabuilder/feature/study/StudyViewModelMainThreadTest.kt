package io.github.abhik9.vocabuilder.feature.study

import io.github.abhik9.vocabuilder.core.common.diagnostics.EventLog
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.domain.study.AnswerCardUseCase
import io.github.abhik9.vocabuilder.core.domain.study.BuildStudyPlanUseCase
import io.github.abhik9.vocabuilder.core.domain.study.MarkKnownUseCase
import io.github.abhik9.vocabuilder.core.domain.study.StudyRequest
import io.github.abhik9.vocabuilder.core.domain.study.UndoAnswerUseCase
import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveDeckOverviewsUseCase
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import io.github.abhik9.vocabuilder.core.model.Word
import io.github.abhik9.vocabuilder.core.testing.FakePronouncer
import io.github.abhik9.vocabuilder.core.testing.LooperMainDispatcher
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.repository.FakeDictionaryRepository
import io.github.abhik9.vocabuilder.core.testing.repository.FakeUserSettingsRepository
import io.github.abhik9.vocabuilder.core.testing.repository.FakeWordRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test

/** The study screen's state on Android's main thread, where flow updates can arrive out of order. */
@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelMainThreadTest {
    private val main = LooperMainDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        main.close()
    }

    @Test
    fun showsTheFirstCardWhenTheScreenWasWatchingWhileItLoaded() = runBlocking {
        val words = FakeWordRepository.withProgress()
        val progress = words.progressRepository
        val loaded = CompletableDeferred<Unit>()
        // Like the database's, the words take a while to load, so the screen is already watching.
        val slowWords = object : WordRepository by words {
            override suspend fun getWords(ids: List<String>): List<Word> {
                loaded.await()
                return words.getWords(ids)
            }
        }
        val settings = FakeUserSettingsRepository(UserSettings(shuffleCards = false))
        val viewModel = StudyViewModel(
            request = StudyRequest(StudyMode.DECK, TestData.advancedDeck.id),
            buildPlan = BuildStudyPlanUseCase(slowWords, settings, TestData.clock),
            answerCard = AnswerCardUseCase(progress, TestData.clock),
            undoAnswer = UndoAnswerUseCase(progress),
            markKnown = MarkKnownUseCase(progress),
            progressRepository = progress,
            wordRepository = slowWords,
            dictionaryRepository = FakeDictionaryRepository(),
            observeDeckOverviews = ObserveDeckOverviewsUseCase(slowWords, TestData.clock),
            pronouncer = FakePronouncer(),
            settingsRepository = settings,
            log = EventLog.NONE,
        )
        val screen = launch(main) { viewModel.uiState.collect {} }
        delay(WATCHING_MILLIS)

        loaded.complete(Unit)

        // Before the fix, the state saw the session before its words, and the app crashed.
        val state = withTimeout(TIMEOUT_MILLIS) { viewModel.uiState.first { it != StudyUiState.Loading } }
        assertEquals("pique", assertIs<StudyUiState.Studying>(state).word.headword)
        screen.cancel()
    }

    private companion object {
        const val WATCHING_MILLIS = 300L
        const val TIMEOUT_MILLIS = 5_000L
    }
}
