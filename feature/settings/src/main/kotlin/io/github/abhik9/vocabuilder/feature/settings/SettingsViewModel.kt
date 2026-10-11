package io.github.abhik9.vocabuilder.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.common.AppInfo
import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data object ProgressReset : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val progressRepository: ProgressRepository,
    val appInfo: AppInfo,
) : ViewModel() {
    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    fun setThemeMode(themeMode: ThemeMode) = launch { settingsRepository.setThemeMode(themeMode) }

    fun setUseDynamicColor(useDynamicColor: Boolean) = launch { settingsRepository.setUseDynamicColor(useDynamicColor) }

    fun setNewWordsPerDay(count: Int) = launch { settingsRepository.setNewWordsPerDay(count) }

    fun setCardFront(cardFront: CardFront) = launch { settingsRepository.setCardFront(cardFront) }

    fun setAutoPronounce(autoPronounce: Boolean) = launch { settingsRepository.setAutoPronounce(autoPronounce) }

    fun setShuffleCards(shuffle: Boolean) = launch { settingsRepository.setShuffleCards(shuffle) }

    fun resetAllProgress() = launch {
        progressRepository.resetAll()
        _events.send(SettingsEvent.ProgressReset)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
