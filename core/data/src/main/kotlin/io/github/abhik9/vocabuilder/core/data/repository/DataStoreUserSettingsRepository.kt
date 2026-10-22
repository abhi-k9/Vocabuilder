package io.github.abhik9.vocabuilder.core.data.repository

import io.github.abhik9.vocabuilder.core.common.diagnostics.EventLog
import io.github.abhik9.vocabuilder.core.datastore.UserSettingsDataSource
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class DataStoreUserSettingsRepository @Inject constructor(
    private val dataSource: UserSettingsDataSource,
    private val log: EventLog,
) : UserSettingsRepository {
    override val settings: Flow<UserSettings> = dataSource.settings

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataSource.setThemeMode(themeMode)
        log.record { "settings: theme $themeMode" }
    }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        dataSource.setUseDynamicColor(useDynamicColor)
        log.record { "settings: dynamic color $useDynamicColor" }
    }

    override suspend fun setNewWordsPerDay(count: Int) {
        dataSource.setNewWordsPerDay(count)
        log.record { "settings: new words per day $count" }
    }

    override suspend fun setCardFront(cardFront: CardFront) {
        dataSource.setCardFront(cardFront)
        log.record { "settings: card front $cardFront" }
    }

    override suspend fun setAutoPronounce(autoPronounce: Boolean) {
        dataSource.setAutoPronounce(autoPronounce)
        log.record { "settings: pronounce automatically $autoPronounce" }
    }

    override suspend fun setShuffleCards(shuffle: Boolean) {
        dataSource.setShuffleCards(shuffle)
        log.record { "settings: shuffle cards $shuffle" }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataSource.setReminderEnabled(enabled)
        log.record { "settings: daily reminder $enabled" }
    }

    override suspend fun setReminderTime(time: LocalTime) {
        dataSource.setReminderTime(time)
        log.record { "settings: reminder at $time" }
    }
}
