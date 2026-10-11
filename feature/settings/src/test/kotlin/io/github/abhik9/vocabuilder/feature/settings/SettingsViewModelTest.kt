package io.github.abhik9.vocabuilder.feature.settings

import io.github.abhik9.vocabuilder.core.common.AppInfo
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.MainDispatcherRule
import io.github.abhik9.vocabuilder.core.testing.repository.FakeProgressRepository
import io.github.abhik9.vocabuilder.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeUserSettingsRepository()
    private val progress = FakeProgressRepository()

    // Created in each test, once the rule has replaced the main dispatcher that viewModelScope uses.
    private fun viewModel() = SettingsViewModel(settings, progress, AppInfo("1.0.0", "https://example.com"))

    @Test
    fun changesSettings() = runTest {
        val viewModel = viewModel()
        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setUseDynamicColor(false)
        viewModel.setNewWordsPerDay(20)
        viewModel.setCardFront(CardFront.DEFINITION)
        viewModel.setAutoPronounce(true)
        viewModel.setShuffleCards(true)

        assertEquals(
            UserSettings(
                themeMode = ThemeMode.DARK,
                useDynamicColor = false,
                newWordsPerDay = 20,
                cardFront = CardFront.DEFINITION,
                autoPronounce = true,
                shuffleCards = true,
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
}
