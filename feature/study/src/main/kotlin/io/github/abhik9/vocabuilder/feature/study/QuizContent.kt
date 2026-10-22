package io.github.abhik9.vocabuilder.feature.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.ui.colors
import io.github.abhik9.vocabuilder.core.ui.label

/**
 * A quiz card: the word, or its meaning, and the choices to pick the right one from. Once a choice is picked, the
 * right one is marked with a check and a wrong pick with a cross, and Continue answers the card.
 */
@Composable
internal fun QuizContent(
    state: StudyUiState.Studying,
    quiz: QuizCard,
    onChoose: (index: Int) -> Unit,
    onContinue: () -> Unit,
    onMarkKnown: () -> Unit,
) {
    val byWord = state.cardFront == CardFront.WORD
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = QUIZ_MAX_WIDTH),
    ) {
        LinearProgressIndicator(
            progress = { if (state.wordCount == 0) 0f else state.doneCount.toFloat() / state.wordCount },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuizPrompt(state, byWord)
            quiz.choices.forEachIndexed { index, choice ->
                val outcome = when {
                    quiz.chosen == null -> ChoiceOutcome.OPEN
                    choice.wordId == state.word.id -> ChoiceOutcome.RIGHT
                    index == quiz.chosen -> ChoiceOutcome.WRONG
                    else -> ChoiceOutcome.OTHER
                }
                QuizChoiceButton(
                    text = if (byWord) choice.definition else choice.headword,
                    outcome = outcome,
                    onClick = { onChoose(index) },
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            if (quiz.chosen == null) {
                OutlinedButton(
                    onClick = onMarkKnown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(stringResource(R.string.feature_study_already_know))
                }
            } else {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(stringResource(R.string.feature_study_continue))
                }
            }
        }
    }
}

@Composable
private fun QuizPrompt(state: StudyUiState.Studying, byWord: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    if (byWord) R.string.feature_study_quiz_which_meaning else R.string.feature_study_which_word,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val sense = state.word.primarySense
            if (byWord) {
                Text(
                    text = state.word.headword,
                    style = VocabuilderTheme.wordTypography.display,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = sense.definition,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = sense.partOfSpeech.label(),
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class ChoiceOutcome { OPEN, RIGHT, WRONG, OTHER }

/** A choice to pick; once picked, the right one in green with a check, a wrong pick in amber with a cross. */
@Composable
private fun QuizChoiceButton(text: String, outcome: ChoiceOutcome, onClick: () -> Unit) {
    val right = MasteryState.MASTERED.colors()
    val wrong = MasteryState.LEARNING.colors()
    val container = when (outcome) {
        ChoiceOutcome.RIGHT -> right.container
        ChoiceOutcome.WRONG -> wrong.container
        else -> MaterialTheme.colorScheme.surface
    }
    val content = when (outcome) {
        ChoiceOutcome.RIGHT -> right.onContainer
        ChoiceOutcome.WRONG -> wrong.onContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val state = when (outcome) {
        ChoiceOutcome.RIGHT -> stringResource(R.string.feature_study_quiz_right)
        ChoiceOutcome.WRONG -> stringResource(R.string.feature_study_quiz_wrong)
        else -> null
    }
    Surface(
        onClick = onClick,
        enabled = outcome == ChoiceOutcome.OPEN,
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        border = if (outcome == ChoiceOutcome.OPEN || outcome == ChoiceOutcome.OTHER) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (outcome == ChoiceOutcome.OTHER) 0.6f else 1f)
            .semantics {
                if (state != null) stateDescription = state
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            when (outcome) {
                ChoiceOutcome.RIGHT -> Icon(Icons.Filled.Check, contentDescription = null)
                ChoiceOutcome.WRONG -> Icon(Icons.Filled.Close, contentDescription = null)
                else -> Unit
            }
        }
    }
}

private val QUIZ_MAX_WIDTH = 640.dp
