package io.github.abhik9.vocabuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.common.WhileUiSubscribed
import io.github.abhik9.vocabuilder.core.domain.repository.ContentRepository
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(val settings: UserSettings) : MainActivityUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settingsRepository: UserSettingsRepository,
    contentRepository: ContentRepository,
) : ViewModel() {
    /** Loading until the settings are read and the words are in the database. */
    val uiState: StateFlow<MainActivityUiState> =
        combine(settingsRepository.settings, contentRepository.isReady) { settings, contentReady ->
            if (contentReady) MainActivityUiState.Success(settings) else MainActivityUiState.Loading
        }.stateIn(viewModelScope, WhileUiSubscribed, MainActivityUiState.Loading)
}
