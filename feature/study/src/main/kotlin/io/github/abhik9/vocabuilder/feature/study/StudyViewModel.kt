package io.github.abhik9.vocabuilder.feature.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.domain.study.AnswerCardUseCase
import io.github.abhik9.vocabuilder.core.domain.study.BuildStudyPlanUseCase
import io.github.abhik9.vocabuilder.core.domain.study.SessionSummary
import io.github.abhik9.vocabuilder.core.domain.study.StudyRequest
import io.github.abhik9.vocabuilder.core.domain.study.StudySession
import io.github.abhik9.vocabuilder.core.domain.study.UndoAnswerUseCase
import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveDeckOverviewsUseCase
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface StudyUiState {
    data object Loading : StudyUiState

    /** Nothing to study; for a deck, practice is offered. */
    data object Empty : StudyUiState

    /**
     * @property doneCount words known in this session, out of [wordCount].
     * @property isFlipped the card shows its back: the meaning, or the word for definition-first cards.
     */
    data class Studying(
        val word: Word,
        val isFlipped: Boolean,
        val cardFront: CardFront,
        val doneCount: Int,
        val wordCount: Int,
        val canUndo: Boolean,
    ) : StudyUiState

    /** @property nextDeck the following deck with something to study, if this was a deck. */
    data class Finished(
        val summary: SessionSummary,
        val nextDeck: Deck?,
    ) : StudyUiState
}

/** What the session goes through, for the title: a deck, or the daily review or starred words. */
data class StudyTitle(
    val mode: StudyMode,
    val deck: Deck?,
    val isPractice: Boolean,
)

@HiltViewModel(assistedFactory = StudyViewModel.Factory::class)
class StudyViewModel @AssistedInject constructor(
    @Assisted private val request: StudyRequest,
    private val buildPlan: BuildStudyPlanUseCase,
    private val answerCard: AnswerCardUseCase,
    private val undoAnswer: UndoAnswerUseCase,
    private val wordRepository: WordRepository,
    private val observeDeckOverviews: ObserveDeckOverviewsUseCase,
    settingsRepository: UserSettingsRepository,
) : ViewModel() {
    private val session = MutableStateFlow<StudySession?>(null)
    private val words = MutableStateFlow<Map<String, Word>>(emptyMap())
    private val isFlipped = MutableStateFlow(false)
    private val nextDeck = MutableStateFlow<Deck?>(null)

    /** Set while an answer is being saved, so a double tap doesn't answer the next card too. */
    private var isAnswering = false

    val title: StateFlow<StudyTitle> = wordRepository.observeDecks()
        .map { decks -> StudyTitle(request.mode, decks.firstOrNull { it.id == request.deckId }, request.practice) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StudyTitle(request.mode, null, request.practice))

    val uiState: StateFlow<StudyUiState> = combine(
        session,
        words,
        isFlipped,
        settingsRepository.settings,
        nextDeck,
    ) { session, words, flipped, settings, nextDeck ->
        when {
            session == null -> StudyUiState.Loading

            session.isFinished && !session.canUndo -> StudyUiState.Empty

            session.isFinished -> StudyUiState.Finished(session.summary(), nextDeck)

            else -> StudyUiState.Studying(
                word = words.getValue(checkNotNull(session.currentWordId)),
                isFlipped = flipped,
                cardFront = settings.cardFront,
                doneCount = session.doneCount,
                wordCount = session.wordCount,
                canUndo = session.canUndo,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StudyUiState.Loading)

    init {
        viewModelScope.launch {
            val plan = buildPlan(request)
            val loaded = wordRepository.getWords(plan.wordIds).associateBy { it.id }
            words.value = loaded
            session.value = StudySession(plan.wordIds.filter { it in loaded })
        }
    }

    fun flip() = isFlipped.update { !it }

    fun answer(knew: Boolean) {
        val current = session.value ?: return
        val wordId = current.currentWordId ?: return
        if (isAnswering) return
        isAnswering = true
        viewModelScope.launch {
            try {
                val answer = answerCard(wordId, knew, request.mode)
                val next = current.answer(answer)
                if (next.isFinished) nextDeck.value = findNextDeck()
                isFlipped.value = false
                session.value = next
            } finally {
                isAnswering = false
            }
        }
    }

    fun undo() {
        val current = session.value ?: return
        val last = current.lastAnswer ?: return
        if (isAnswering) return
        isAnswering = true
        viewModelScope.launch {
            try {
                undoAnswer(last)
                isFlipped.value = false
                session.value = current.undo()
            } finally {
                isAnswering = false
            }
        }
    }

    /** The first deck after this one, in pack order, with words to study. */
    private suspend fun findNextDeck(): Deck? {
        if (request.mode != StudyMode.DECK) return null
        val overviews = observeDeckOverviews().first()
        val index = overviews.indexOfFirst { it.deck.id == request.deckId }
        if (index < 0) return null
        return overviews.drop(index + 1).firstOrNull { !it.isCaughtUp }?.deck
    }

    @AssistedFactory
    interface Factory {
        fun create(request: StudyRequest): StudyViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
