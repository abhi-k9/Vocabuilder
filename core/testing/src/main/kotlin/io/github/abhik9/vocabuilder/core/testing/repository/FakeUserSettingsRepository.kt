package io.github.abhik9.vocabuilder.core.testing.repository

import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FakeUserSettingsRepository(initial: UserSettings = UserSettings()) : UserSettingsRepository {
    private val _settings = MutableStateFlow(initial)
    override val settings: StateFlow<UserSettings> = _settings

    fun set(settings: UserSettings) {
        _settings.value = settings
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) = _settings.update { it.copy(themeMode = themeMode) }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) =
        _settings.update { it.copy(useDynamicColor = useDynamicColor) }

    override suspend fun setNewWordsPerDay(count: Int) = _settings.update {
        it.copy(newWordsPerDay = count.coerceIn(UserSettings.NEW_WORDS_PER_DAY_RANGE))
    }

    override suspend fun setCardFront(cardFront: CardFront) = _settings.update { it.copy(cardFront = cardFront) }

    override suspend fun setAutoPronounce(autoPronounce: Boolean) =
        _settings.update { it.copy(autoPronounce = autoPronounce) }

    override suspend fun setShuffleCards(shuffle: Boolean) = _settings.update { it.copy(shuffleCards = shuffle) }

    override suspend fun setReminderEnabled(enabled: Boolean) = _settings.update { it.copy(reminderEnabled = enabled) }

    override suspend fun setReminderTime(time: LocalTime) = _settings.update { it.copy(reminderTime = time) }
}
