package io.github.abhik9.vocabuilder.core.domain.repository

import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setUseDynamicColor(useDynamicColor: Boolean)

    suspend fun setNewWordsPerDay(count: Int)

    suspend fun setCardFront(cardFront: CardFront)

    suspend fun setAutoPronounce(autoPronounce: Boolean)

    suspend fun setShuffleCards(shuffle: Boolean)

    suspend fun setReminderEnabled(enabled: Boolean)

    suspend fun setReminderTime(time: LocalTime)
}
