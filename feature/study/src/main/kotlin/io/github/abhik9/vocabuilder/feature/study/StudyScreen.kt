package io.github.abhik9.vocabuilder.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.vocabuilder.core.designsystem.component.EmptyState
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme
import io.github.abhik9.vocabuilder.core.domain.study.SessionSummary
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.DictionarySource
import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.ui.WordMeaningSheet
import io.github.abhik9.vocabuilder.core.ui.colors
import io.github.abhik9.vocabuilder.core.ui.title

@Composable
internal fun StudyRoute(
    onClose: () -> Unit,
    onPractice: () -> Unit,
    onNextDeck: (Deck) -> Unit,
    viewModel: StudyViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val canPronounce by viewModel.canPronounce.collectAsStateWithLifecycle()
    val relatedWord by viewModel.relatedWord.collectAsStateWithLifecycle()
    StudyScreen(
        uiState = uiState,
        title = title,
        canPronounce = canPronounce,
        relatedWord = relatedWord,
        onPronounce = viewModel::pronounce,
        onLookUp = viewModel::lookUp,
        onPronounceRelatedWord = viewModel::pronounceRelatedWord,
        onDismissRelatedWord = viewModel::dismissRelatedWord,
        onClose = onClose,
        onFlip = viewModel::flip,
        onAnswer = viewModel::answer,
        onChoose = viewModel::choose,
        onContinueQuiz = viewModel::continueQuiz,
        onUndo = viewModel::undo,
        onToggleStar = viewModel::toggleStar,
        onMarkKnown = viewModel::markKnown,
        onPractice = onPractice,
        onNextDeck = onNextDeck,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudyScreen(
    uiState: StudyUiState,
    title: StudyTitle,
    canPronounce: Boolean,
    relatedWord: RelatedWord?,
    onPronounce: () -> Unit,
    onLookUp: (headword: String) -> Unit,
    onPronounceRelatedWord: () -> Unit,
    onDismissRelatedWord: () -> Unit,
    onClose: () -> Unit,
    onFlip: () -> Unit,
    onAnswer: (knew: Boolean) -> Unit,
    onChoose: (index: Int) -> Unit,
    onContinueQuiz: () -> Unit,
    onUndo: () -> Unit,
    onToggleStar: () -> Unit,
    onMarkKnown: () -> Unit,
    onPractice: () -> Unit,
    onNextDeck: (Deck) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title.text(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (uiState is StudyUiState.Studying) {
                            Text(
                                text = stringResource(
                                    R.string.feature_study_count,
                                    uiState.doneCount,
                                    uiState.wordCount,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_study_close))
                    }
                },
                actions = {
                    if (uiState is StudyUiState.Studying) {
                        if (canPronounce) {
                            IconButton(onClick = onPronounce) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = stringResource(R.string.feature_study_pronounce),
                                )
                            }
                        }
                        IconButton(onClick = onToggleStar) {
                            Icon(
                                imageVector = if (uiState.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = stringResource(
                                    if (uiState.isStarred) {
                                        R.string.feature_study_unstar
                                    } else {
                                        R.string.feature_study_star
                                    },
                                ),
                            )
                        }
                        IconButton(onClick = onUndo, enabled = uiState.canUndo) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = stringResource(R.string.feature_study_undo),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                StudyUiState.Loading -> CircularProgressIndicator()

                StudyUiState.Empty -> EmptySession(title, onPractice = onPractice, onDone = onClose)

                is StudyUiState.Studying -> {
                    val quiz = uiState.quiz
                    if (quiz != null) {
                        // Keyed like the cards, so each quiz card starts scrolled to the top.
                        key(uiState.cardIndex) {
                            QuizContent(
                                state = uiState,
                                quiz = quiz,
                                onChoose = onChoose,
                                onContinue = onContinueQuiz,
                                onMarkKnown = onMarkKnown,
                            )
                        }
                    } else {
                        StudyingContent(
                            state = uiState,
                            onFlip = onFlip,
                            onAnswer = onAnswer,
                            onLookUp = onLookUp,
                            onMarkKnown = onMarkKnown,
                        )
                    }
                }

                is StudyUiState.Finished -> FinishedContent(
                    summary = uiState.summary,
                    title = title,
                    nextDeck = uiState.nextDeck,
                    onNextDeck = onNextDeck,
                    onDone = onClose,
                )
            }
        }
    }
    relatedWord?.let {
        WordMeaningSheet(
            word = it.headword,
            meanings = it.word?.senses ?: it.entry?.senses.orEmpty(),
            fromWordNet = it.entry?.source == DictionarySource.WORDNET,
            canPronounce = canPronounce,
            onPronounce = onPronounceRelatedWord,
            onDismiss = onDismissRelatedWord,
        )
    }
}

@Composable
private fun StudyingContent(
    state: StudyUiState.Studying,
    onFlip: () -> Unit,
    onAnswer: (knew: Boolean) -> Unit,
    onLookUp: (headword: String) -> Unit,
    onMarkKnown: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = MAX_CONTENT_WIDTH),
    ) {
        LinearProgressIndicator(
            progress = { if (state.wordCount == 0) 0f else state.doneCount.toFloat() / state.wordCount },
            modifier = Modifier.fillMaxWidth(),
        )
        // Each card starts afresh: unturned, rather than turning back over with the next word's
        // meaning showing, and centered, even when a word swiped away comes straight back.
        key(state.cardIndex) {
            FlashCard(
                word = state.word,
                cardFront = state.cardFront,
                isFlipped = state.isFlipped,
                onFlip = onFlip,
                onAnswer = onAnswer,
                onLookUp = onLookUp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isFlipped) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onAnswer(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.feature_study_did_not_know))
                    }
                    Button(
                        onClick = { onAnswer(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MasteryState.MASTERED.colors().accent,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.feature_study_knew))
                    }
                }
                Text(
                    text = stringResource(R.string.feature_study_swipe_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onMarkKnown,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Text(stringResource(R.string.feature_study_already_know), textAlign = TextAlign.Center)
                    }
                    FilledTonalButton(
                        onClick = onFlip,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (state.cardFront == CardFront.WORD) {
                                    R.string.feature_study_show_meaning
                                } else {
                                    R.string.feature_study_show_word
                                },
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySession(title: StudyTitle, onPractice: () -> Unit, onDone: () -> Unit) {
    val canPractice = title.mode == StudyMode.DECK && !title.isPractice
    EmptyState(
        emoji = when (title.mode) {
            StudyMode.STARRED -> "⭐"
            StudyMode.TROUBLE -> "💪"
            else -> "✨"
        },
        title = stringResource(R.string.feature_study_empty_title),
        message = stringResource(
            when (title.mode) {
                StudyMode.DECK -> R.string.feature_study_empty_deck
                StudyMode.DAILY_REVIEW -> R.string.feature_study_empty_daily
                StudyMode.STARRED -> R.string.feature_study_empty_starred
                StudyMode.TROUBLE -> R.string.feature_study_empty_trouble
            },
        ),
        action = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (canPractice) {
                    Button(onClick = onPractice) { Text(stringResource(R.string.feature_study_practice_anyway)) }
                    TextButton(onClick = onDone) { Text(stringResource(R.string.feature_study_done)) }
                } else {
                    Button(onClick = onDone) { Text(stringResource(R.string.feature_study_done)) }
                }
            }
        },
    )
}

@Composable
private fun FinishedContent(
    summary: SessionSummary,
    title: StudyTitle,
    nextDeck: Deck?,
    onNextDeck: (Deck) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = MAX_CONTENT_WIDTH)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = "🎉", fontSize = 56.sp)
        Text(
            text = stringResource(R.string.feature_study_finished_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        title.deck?.let { deck ->
            Text(
                text = stringResource(R.string.feature_study_finished_deck, deck.title()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(summary.studied, stringResource(R.string.feature_study_stat_studied), Modifier.weight(1f))
            StatTile(summary.knewFirstTime, stringResource(R.string.feature_study_stat_knew), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(summary.missed, stringResource(R.string.feature_study_stat_missed), Modifier.weight(1f))
            StatTile(summary.newlyMastered, stringResource(R.string.feature_study_stat_mastered), Modifier.weight(1f))
        }
        nextDeck?.let { deck ->
            Button(onClick = { onNextDeck(deck) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_study_next_deck, deck.title()))
            }
        }
        if (nextDeck == null) {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_study_done))
            }
        } else {
            TextButton(onClick = onDone) { Text(stringResource(R.string.feature_study_done)) }
        }
    }
}

@Composable
private fun StatTile(value: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** "Common · Deck 3", "Daily review", "Starred words" or "Trouble words", marked when practicing. */
@Composable
private fun StudyTitle.text(): String {
    val base = when (mode) {
        StudyMode.DECK -> deck?.title().orEmpty()
        StudyMode.DAILY_REVIEW -> stringResource(R.string.feature_study_daily_review)
        StudyMode.STARRED -> stringResource(R.string.feature_study_starred)
        StudyMode.TROUBLE -> stringResource(R.string.feature_study_trouble)
    }
    return if (isPractice) stringResource(R.string.feature_study_practice, base) else base
}

private val MAX_CONTENT_WIDTH = 640.dp
