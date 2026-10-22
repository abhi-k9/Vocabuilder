package io.github.abhik9.vocabuilder.feature.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.common.WhileUiSubscribed
import io.github.abhik9.vocabuilder.core.domain.repository.DictionaryRepository
import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.domain.speech.Pronouncer
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.DictionaryEntry
import io.github.abhik9.vocabuilder.core.model.WordWithProgress
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** A synonym or antonym that isn't a word of the app, looked up in the dictionary: its [entry], if it has one. */
data class LookedUpWord(
    val word: String,
    val entry: DictionaryEntry?,
)

@HiltViewModel(assistedFactory = WordViewModel.Factory::class)
class WordViewModel @AssistedInject constructor(
    @Assisted private val wordId: String,
    wordRepository: WordRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val progressRepository: ProgressRepository,
    private val pronouncer: Pronouncer,
    private val clock: Clock,
) : ViewModel() {
    /** Whether words can be said aloud on this device. */
    val canPronounce: StateFlow<Boolean> = pronouncer.isAvailable

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
    }.stateIn(viewModelScope, WhileUiSubscribed, WordUiState.Loading)

    private val _lookedUp = MutableStateFlow<LookedUpWord?>(null)

    /** The synonym or antonym looked up in the dictionary, while it's shown. */
    val lookedUp: StateFlow<LookedUpWord?> = _lookedUp.asStateFlow()

    init {
        viewModelScope.launch { dictionaryRepository.preload() }
    }

    fun pronounce() {
        val state = uiState.value as? WordUiState.Success ?: return
        pronouncer.speak(state.word.word.headword)
    }

    /** Shows the meanings of a synonym or antonym that isn't a word of the app, from the dictionary. */
    fun lookUp(word: String) {
        viewModelScope.launch { _lookedUp.value = LookedUpWord(word, dictionaryRepository.getEntry(word)) }
    }

    fun pronounceLookedUp() {
        _lookedUp.value?.let { pronouncer.speak(it.word) }
    }

    fun dismissLookedUp() {
        _lookedUp.value = null
    }

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
}
