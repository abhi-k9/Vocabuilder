package io.github.abhik9.vocabuilder.feature.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.common.diagnostics.EventLog
import io.github.abhik9.vocabuilder.core.domain.repository.DictionaryRepository
import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.domain.speech.Pronouncer
import io.github.abhik9.vocabuilder.core.domain.study.AnswerCardUseCase
import io.github.abhik9.vocabuilder.core.domain.study.BuildStudyPlanUseCase
import io.github.abhik9.vocabuilder.core.domain.study.MarkKnownUseCase
import io.github.abhik9.vocabuilder.core.domain.study.QuizChoice
import io.github.abhik9.vocabuilder.core.domain.study.QuizChoices
import io.github.abhik9.vocabuilder.core.domain.study.SessionSummary
import io.github.abhik9.vocabuilder.core.domain.study.StudyRequest
import io.github.abhik9.vocabuilder.core.domain.study.StudySession
import io.github.abhik9.vocabuilder.core.domain.study.UndoAnswerUseCase
import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveDeckOverviewsUseCase
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.DictionaryEntry
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import io.github.abhik9.vocabuilder.core.model.Word
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.model.WordSummary
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
     * @property cardIndex how many cards were answered before this one. It tells cards apart even when
     *   a missed word comes straight back, as the last card left.
     * @property quiz the card's choices, in a quiz.
     */
    data class Studying(
        val word: Word,
        val cardIndex: Int,
        val quiz: QuizCard? = null,
        val isFlipped: Boolean,
        val cardFront: CardFront,
        val doneCount: Int,
        val wordCount: Int,
        val canUndo: Boolean,
        val isStarred: Boolean,
    ) : StudyUiState

    /** @property nextDeck the following deck with something to study, if this was a deck. */
    data class Finished(
        val summary: SessionSummary,
        val nextDeck: Deck?,
    ) : StudyUiState
}

/**
 * A synonym or antonym looked up from a card, with its meanings: the [word] of the app it is, or else its [entry] in
 * the dictionary. Without either, there's no meaning to show.
 */
data class RelatedWord(
    val headword: String,
    val word: Word? = null,
    val entry: DictionaryEntry? = null,
)

/**
 * A quiz card's [choices], and the one the learner picked once they have; then the card shows which is right, and
 * answering it counts it as known if they picked right.
 */
data class QuizCard(
    val choices: List<QuizChoice>,
    val chosen: Int? = null,
)

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
    private val markKnown: MarkKnownUseCase,
    private val progressRepository: ProgressRepository,
    private val wordRepository: WordRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val observeDeckOverviews: ObserveDeckOverviewsUseCase,
    private val pronouncer: Pronouncer,
    settingsRepository: UserSettingsRepository,
    private val log: EventLog,
) : ViewModel() {
    private val settings: StateFlow<UserSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    /** Whether words can be said aloud on this device. */
    val canPronounce: StateFlow<Boolean> = pronouncer.isAvailable

    private val session = MutableStateFlow<StudySession?>(null)

    // Loaded before the session starts, and fixed from then on: the session's words, and the ids of
    // all the app's words by lowercase headword, to look up synonyms and antonyms. Not flows: on
    // Android's main thread, combine() can pass on a new session before the words that came with it.
    private var words: Map<String, Word> = emptyMap()
    private var wordIdsByHeadword: Map<String, String> = emptyMap()

    // A quiz's other choices come from all of the app's words, picked anew for each card, but the same while it shows.
    private var quizPool: List<WordSummary> = emptyList()
    private val quizSeed = Random.nextLong()

    /** The choice picked on a quiz card, and which card it was, so it never shows on another. */
    private val chosen = MutableStateFlow<Pair<Int, Int>?>(null)

    private val _relatedWord = MutableStateFlow<RelatedWord?>(null)

    /** The synonym or antonym looked up from the card, while it's shown. */
    val relatedWord: StateFlow<RelatedWord?> = _relatedWord.asStateFlow()
    private val isFlipped = MutableStateFlow(false)
    private val nextDeck = MutableStateFlow<Deck?>(null)

    /** Set while an answer is being saved, so a double tap doesn't answer the next card too. */
    private var isAnswering = false

    val title: StateFlow<StudyTitle> = wordRepository.observeDecks()
        .map { decks -> StudyTitle(request.mode, decks.firstOrNull { it.id == request.deckId }, request.practice) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StudyTitle(request.mode, null, request.practice))

    /** The current card's progress, kept up to date as it's starred or answered. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentProgress: Flow<WordProgress?> = session
        .map { it?.currentWordId }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else wordRepository.observeWord(id).map { it?.progress } }

    val uiState: StateFlow<StudyUiState> = combine(
        session,
        isFlipped,
        combine(settings, currentProgress, ::Pair),
        nextDeck,
        chosen,
    ) { session, flipped, (settings, progress), nextDeck, chosen ->
        when {
            session == null -> StudyUiState.Loading

            session.isFinished && !session.canUndo -> StudyUiState.Empty

            session.isFinished -> StudyUiState.Finished(session.summary(), nextDeck)

            else -> {
                val word = words.getValue(checkNotNull(session.currentWordId))
                val cardIndex = session.answers.size
                StudyUiState.Studying(
                    word = word,
                    cardIndex = cardIndex,
                    quiz = if (request.quiz) {
                        QuizCard(
                            choices = QuizChoices.of(word, quizPool, Random(quizSeed + cardIndex)),
                            chosen = chosen?.takeIf { it.first == cardIndex }?.second,
                        )
                    } else {
                        null
                    },
                    isFlipped = flipped,
                    cardFront = settings.cardFront,
                    doneCount = session.doneCount,
                    wordCount = session.wordCount,
                    canUndo = session.canUndo,
                    // Until the new card's progress arrives, the previous card's must not show.
                    isStarred = progress?.takeIf { it.wordId == session.currentWordId }?.starred == true,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StudyUiState.Loading)

    init {
        viewModelScope.launch {
            val plan = buildPlan(request)
            val loaded = wordRepository.getWords(plan.wordIds).associateBy { it.id }
            val summaries = wordRepository.observeWordSummaries().first()
            wordIdsByHeadword = summaries.associate { it.headword.lowercase() to it.id }
            quizPool = summaries
            words = loaded
            val started = StudySession(plan.wordIds.filter { it in loaded })
            log.record { "study: started $request, ${started.queue.size} cards" }
            session.value = started
            // For the synonyms and antonyms on the cards.
            dictionaryRepository.preload()
        }
    }

    fun flip() {
        val turningOver = !isFlipped.value
        isFlipped.update { !it }
        if (turningOver && settings.value.autoPronounce) pronounce()
    }

    /** Says the current card's word aloud. */
    fun pronounce() {
        val wordId = session.value?.currentWordId ?: return
        words[wordId]?.let { pronouncer.speak(it.headword) }
    }

    /** Shows one of the card's synonyms or antonyms with its meanings: its own if it's a word of the app. */
    fun lookUp(headword: String) {
        viewModelScope.launch {
            val id = wordIdsByHeadword[headword.lowercase()]
            val word = id?.let { wordRepository.getWords(listOf(it)).firstOrNull() }
            val entry = if (word == null) dictionaryRepository.getEntry(headword) else null
            _relatedWord.value = RelatedWord(headword, word, entry)
            log.record {
                val found = word?.id ?: if (entry != null) "from the dictionary" else "with no meaning"
                "study: looked up \"$headword\", $found"
            }
        }
    }

    fun pronounceRelatedWord() {
        _relatedWord.value?.let { pronouncer.speak(it.headword) }
    }

    fun dismissRelatedWord() {
        _relatedWord.value = null
    }

    fun answer(knew: Boolean) {
        val current = session.value ?: return
        val wordId = current.currentWordId ?: return
        if (isAnswering) return
        isAnswering = true
        viewModelScope.launch {
            try {
                val answer = answerCard(wordId, knew, request.mode)
                log.record {
                    "study: $wordId ${if (knew) "known" else "missed"}, " +
                        "stage ${answer.before?.stage ?: "new"} to ${answer.after.stage}, due ${answer.after.dueDay}"
                }
                val next = current.answer(answer)
                if (next.isFinished) {
                    log.record { "study: finished, ${next.summary()}" }
                    nextDeck.value = findNextDeck()
                }
                isFlipped.value = false
                session.value = next
            } finally {
                isAnswering = false
            }
        }
    }

    /** Picks a choice of the current quiz card, which then shows which is right. */
    fun choose(index: Int) {
        val state = uiState.value as? StudyUiState.Studying ?: return
        val quiz = state.quiz ?: return
        if (quiz.chosen != null || index !in quiz.choices.indices) return
        chosen.value = state.cardIndex to index
        if (settings.value.autoPronounce) pronounce()
    }

    /** Answers the current quiz card as its choice was right or wrong, and moves on. */
    fun continueQuiz() {
        val state = uiState.value as? StudyUiState.Studying ?: return
        val chosenIndex = state.quiz?.chosen ?: return
        answer(knew = state.quiz.choices[chosenIndex].wordId == state.word.id)
    }

    /** Takes the current card out of this session and future ones; undo brings it back. */
    fun markKnown() {
        val current = session.value ?: return
        val wordId = current.currentWordId ?: return
        if (isAnswering) return
        isAnswering = true
        viewModelScope.launch {
            try {
                val next = current.answer(markKnown(wordId))
                log.record { "study: $wordId marked as known" }
                if (next.isFinished) {
                    log.record { "study: finished, ${next.summary()}" }
                    nextDeck.value = findNextDeck()
                }
                isFlipped.value = false
                session.value = next
            } finally {
                isAnswering = false
            }
        }
    }

    fun toggleStar() {
        val wordId = session.value?.currentWordId ?: return
        val starred = (uiState.value as? StudyUiState.Studying)?.isStarred ?: return
        viewModelScope.launch { progressRepository.setStarred(wordId, !starred) }
    }

    fun undo() {
        val current = session.value ?: return
        val last = current.lastAnswer ?: return
        if (isAnswering) return
        isAnswering = true
        viewModelScope.launch {
            try {
                undoAnswer(last)
                log.record { "study: undid the answer to ${last.wordId}" }
                isFlipped.value = false
                chosen.value = null
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
