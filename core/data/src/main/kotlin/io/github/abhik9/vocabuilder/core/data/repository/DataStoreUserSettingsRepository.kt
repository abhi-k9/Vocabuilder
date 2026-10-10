package io.github.abhik9.vocabuilder.core.data.repository

import io.github.abhik9.vocabuilder.core.datastore.UserSettingsDataSource
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class DataStoreUserSettingsRepository @Inject constructor(
    private val dataSource: UserSettingsDataSource,
) : UserSettingsRepository {
    override val settings: Flow<UserSettings> = dataSource.settings

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataSource.setThemeMode(themeMode)
    }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        dataSource.setUseDynamicColor(useDynamicColor)
    }

    override suspend fun setNewWordsPerDay(count: Int) {
        dataSource.setNewWordsPerDay(count)
    }

    override suspend fun setCardFront(cardFront: CardFront) {
        dataSource.setCardFront(cardFront)
    }

    override suspend fun setAutoPronounce(autoPronounce: Boolean) {
        dataSource.setAutoPronounce(autoPronounce)
    }

    override suspend fun setShuffleCards(shuffle: Boolean) {
        dataSource.setShuffleCards(shuffle)
    }
}
