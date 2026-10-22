package io.github.abhik9.vocabuilder.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.common.WhileUiSubscribed
import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.vocabuilder.core.model.TodayOverview
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(val today: TodayOverview) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeToday: ObserveTodayUseCase,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = observeToday()
        .map<TodayOverview, HomeUiState>(HomeUiState::Success)
        .stateIn(viewModelScope, WhileUiSubscribed, HomeUiState.Loading)
}
