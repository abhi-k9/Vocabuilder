package io.github.abhik9.vocabuilder.feature.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveDeckOverviewsUseCase
import io.github.abhik9.vocabuilder.core.model.DeckOverview
import io.github.abhik9.vocabuilder.core.model.WordSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DeckUiState {
    data object Loading : DeckUiState

    data object NotFound : DeckUiState

    /** @property words the deck's words, in study order. */
    data class Success(
        val overview: DeckOverview,
        val words: List<WordSummary>,
    ) : DeckUiState
}

@HiltViewModel(assistedFactory = DeckViewModel.Factory::class)
class DeckViewModel @AssistedInject constructor(
    @Assisted private val deckId: String,
    wordRepository: WordRepository,
    observeDeckOverviews: ObserveDeckOverviewsUseCase,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    val uiState: StateFlow<DeckUiState> =
        combine(observeDeckOverviews(), wordRepository.observeWordSummaries()) { overviews, words ->
            val overview = overviews.firstOrNull { it.deck.id == deckId } ?: return@combine DeckUiState.NotFound
            DeckUiState.Success(
                overview = overview,
                words = words.filter { it.deckId == deckId }.sortedBy { it.position },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DeckUiState.Loading)

    fun resetProgress() {
        viewModelScope.launch { progressRepository.resetDeck(deckId) }
    }

    @AssistedFactory
    interface Factory {
        fun create(deckId: String): DeckViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
