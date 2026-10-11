package io.github.abhik9.vocabuilder.feature.words

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.model.WordSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface WordsUiState {
    data object Loading : WordsUiState

    data class Success(
        val words: List<WordSummary>,
        val filter: WordFilter,
    ) : WordsUiState
}

@HiltViewModel
class WordsViewModel @Inject constructor(
    wordRepository: WordRepository,
) : ViewModel() {
    /** Held as Compose state so the search field updates synchronously while typing. */
    var query by mutableStateOf("")
        private set

    private val filter = MutableStateFlow(WordFilter.ALL)

    val uiState: StateFlow<WordsUiState> = combine(
        wordRepository.observeWordSummaries(),
        snapshotFlow { query },
        filter,
    ) { words, query, filter ->
        WordsUiState.Success(words = searchWords(words, query, filter), filter = filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WordsUiState.Loading)

    fun setQuery(query: String) {
        this.query = query
    }

    fun setFilter(filter: WordFilter) {
        this.filter.value = filter
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
