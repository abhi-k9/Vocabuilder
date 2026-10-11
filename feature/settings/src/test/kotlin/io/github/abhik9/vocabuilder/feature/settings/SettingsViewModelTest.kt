package io.github.abhik9.vocabuilder.feature.settings

import io.github.abhik9.vocabuilder.core.common.AppInfo
import io.github.abhik9.vocabuilder.core.domain.diagnostics.ExportDiagnosticsUseCase
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.MainDispatcherRule
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.diagnostics.FakeDiagnosticsRepository
import io.github.abhik9.vocabuilder.core.testing.diagnostics.FakeDocumentStore
import io.github.abhik9.vocabuilder.core.testing.diagnostics.RecordingEventLog
import io.github.abhik9.vocabuilder.core.testing.repository.FakeProgressRepository
import io.github.abhik9.vocabuilder.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeUserSettingsRepository()
    private val progress = FakeProgressRepository()
    private val diagnostics = FakeDiagnosticsRepository(report = "the report")
    private val documents = FakeDocumentStore()

    // Created in each test, once the rule has replaced the main dispatcher that viewModelScope uses.
    private fun viewModel() = SettingsViewModel(
        settingsRepository = settings,
        progressRepository = progress,
        diagnosticsRepository = diagnostics,
        exportDiagnostics = ExportDiagnosticsUseCase(diagnostics, documents, RecordingEventLog()),
        clock = TestData.clock,
        appInfo = AppInfo("1.0.0", "https://example.com"),
    )

    @Test
    fun changesSettings() = runTest {
        val viewModel = viewModel()
        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setUseDynamicColor(false)
        viewModel.setNewWordsPerDay(20)
        viewModel.setCardFront(CardFront.DEFINITION)
        viewModel.setAutoPronounce(true)
        viewModel.setShuffleCards(false)

        assertEquals(
            UserSettings(
                themeMode = ThemeMode.DARK,
                useDynamicColor = false,
                newWordsPerDay = 20,
                cardFront = CardFront.DEFINITION,
                autoPronounce = true,
                shuffleCards = false,
            ),
            settings.settings.value,
        )
    }

    @Test
    fun resetsAllProgressAndSaysSo() = runTest {
        progress.setProgress(WordProgress("gre:abate", stage = 3, reviewCount = 2))
        val viewModel = viewModel()

        viewModel.resetAllProgress()

        assertNull(progress.getProgress("gre:abate"))
        assertEquals(SettingsEvent.ProgressReset, viewModel.events.first())
    }

    @Test
    fun turnsDiagnosticsOnAndOffAndClearsThem() = runTest {
        val viewModel = viewModel()
        diagnostics.logSize.value = 1024

        viewModel.setDiagnosticsRecording(true)
        assertEquals(DiagnosticsState(isRecording = true, logBytes = 1024), viewModel.diagnostics.value)

        viewModel.clearDiagnostics()
        viewModel.setDiagnosticsRecording(false)
        assertEquals(DiagnosticsState(isRecording = false, logBytes = 0), viewModel.diagnostics.value)
    }

    @Test
    fun exportsDiagnosticsToADatedFileAndSaysWhetherItWorked() = runTest {
        val viewModel = viewModel()
        assertEquals("vocabuilder-diagnostics-2026-10-10.txt", viewModel.suggestedDiagnosticsFileName)

        viewModel.exportDiagnosticsTo("content://exported")
        assertEquals("the report", documents.documents["content://exported"])
        assertTrue(assertIs<SettingsEvent.DiagnosticsExported>(viewModel.events.first()).success)

        documents.failing = true
        viewModel.exportDiagnosticsTo("content://failing")
        assertFalse(assertIs<SettingsEvent.DiagnosticsExported>(viewModel.events.first()).success)
    }
}
