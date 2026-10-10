package io.github.abhik9.vocabuilder.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserSettingsDataSourceTest {
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private val dataSource = UserSettingsDataSource(
        PreferenceDataStoreFactory.create(scope = testScope.backgroundScope) {
            tmpFolder.newFile("user_settings.preferences_pb")
        },
    )

    @Test
    fun emptyStoreYieldsDefaults() = testScope.runTest {
        assertEquals(UserSettings(), dataSource.settings.first())
    }

    @Test
    fun writtenValuesAreReadBack() = testScope.runTest {
        dataSource.setThemeMode(ThemeMode.DARK)
        dataSource.setUseDynamicColor(false)
        dataSource.setNewWordsPerDay(25)
        dataSource.setCardFront(CardFront.DEFINITION)
        dataSource.setAutoPronounce(true)
        dataSource.setShuffleCards(true)

        assertEquals(
            UserSettings(
                themeMode = ThemeMode.DARK,
                useDynamicColor = false,
                newWordsPerDay = 25,
                cardFront = CardFront.DEFINITION,
                autoPronounce = true,
                shuffleCards = true,
            ),
            dataSource.settings.first(),
        )
    }

    @Test
    fun newWordsPerDayStaysInRange() = testScope.runTest {
        dataSource.setNewWordsPerDay(500)
        assertEquals(UserSettings.NEW_WORDS_PER_DAY_RANGE.last, dataSource.settings.first().newWordsPerDay)

        dataSource.setNewWordsPerDay(-3)
        assertEquals(0, dataSource.settings.first().newWordsPerDay)
    }
}
