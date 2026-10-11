package io.github.abhik9.vocabuilder.feature.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.WordWithProgress
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WordUiState {
    data object Loading : WordUiState

    data object NotFound : WordUiState

    /**
     * @property relatedWordIds the synonyms and antonyms that are words of their own, by headword,
     * so they can be opened.
     */
    data class Success(
        val word: WordWithProgress,
        val deck: Deck?,
        val relatedWordIds: Map<String, String>,
        val today: LocalDate,
    ) : WordUiState
}

@HiltViewModel(assistedFactory = WordViewModel.Factory::class)
class WordViewModel @AssistedInject constructor(
    @Assisted private val wordId: String,
    wordRepository: WordRepository,
    private val progressRepository: ProgressRepository,
    private val clock: Clock,
) : ViewModel() {
    val uiState: StateFlow<WordUiState> = combine(
        wordRepository.observeWord(wordId),
        wordRepository.observeDecks(),
        wordRepository.observeWordSummaries(),
    ) { word, decks, summaries ->
        if (word == null) return@combine WordUiState.NotFound
        val related = (word.word.synonyms + word.word.antonyms).toSet()
        WordUiState.Success(
            word = word,
            deck = decks.firstOrNull { it.id == word.word.deckId },
            relatedWordIds = summaries.filter { it.headword in related }.associate { it.headword to it.id },
            today = LocalDate.now(clock),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WordUiState.Loading)

    fun toggleStar() {
        val state = uiState.value as? WordUiState.Success ?: return
        viewModelScope.launch { progressRepository.setStarred(wordId, !state.word.progress.starred) }
    }

    fun toggleKnown() {
        val state = uiState.value as? WordUiState.Success ?: return
        viewModelScope.launch { progressRepository.setAlreadyKnown(wordId, !state.word.progress.alreadyKnown) }
    }

    @AssistedFactory
    interface Factory {
        fun create(wordId: String): WordViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
