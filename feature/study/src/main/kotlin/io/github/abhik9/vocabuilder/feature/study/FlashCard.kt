package io.github.abhik9.vocabuilder.feature.study

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.model.Word
import io.github.abhik9.vocabuilder.core.ui.colors
import io.github.abhik9.vocabuilder.core.ui.label
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * A flashcard that turns over when tapped. Once turned, it can be swiped right for "knew it" or
 * left for "didn't know"; the same answers are offered as accessibility actions.
 *
 * Its state belongs to one showing of a card: give each card its own `key`, so the next one starts
 * unturned and centered.
 */
@Composable
internal fun FlashCard(
    word: Word,
    cardFront: CardFront,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onAnswer: (knew: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = FLIP_MILLIS),
        label = "flip",
    )
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnAnswer by rememberUpdatedState(onAnswer)

    val flipLabel = stringResource(
        when {
            isFlipped -> R.string.feature_study_flip_back
            cardFront == CardFront.WORD -> R.string.feature_study_show_meaning
            else -> R.string.feature_study_show_word
        },
    )
    val knewLabel = stringResource(R.string.feature_study_knew)
    val didNotKnowLabel = stringResource(R.string.feature_study_did_not_know)

    // The swipe is handled, tilted and labeled outside the layer that turns the card over: a turned
    // card is mirrored, so inside it drags would go the wrong way and the label would read backwards.
    Box(
        modifier = modifier
            .absoluteOffset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(isFlipped) {
                if (!isFlipped) return@pointerInput
                val threshold = size.width * SWIPE_THRESHOLD
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val distance = offsetX.value
                            if (abs(distance) > threshold) {
                                val knew = distance > 0
                                offsetX.animateTo(size.width * if (knew) 1.5f else -1.5f, tween(SWIPE_OUT_MILLIS))
                                currentOnAnswer(knew)
                            } else {
                                offsetX.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                }
            }
            .graphicsLayer { rotationZ = offsetX.value / size.width.coerceAtLeast(1f) * MAX_TILT_DEGREES },
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = CAMERA_DISTANCE * density
                }
                .clip(CardDefaults.shape)
                .clickable(onClickLabel = flipLabel, role = Role.Button, onClick = onFlip)
                .semantics {
                    if (isFlipped) {
                        customActions = listOf(
                            CustomAccessibilityAction(knewLabel) {
                                onAnswer(true)
                                true
                            },
                            CustomAccessibilityAction(didNotKnowLabel) {
                                onAnswer(false)
                                true
                            },
                        )
                    }
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (rotation <= 90f) {
                    CardFrontFace(word, cardFront)
                } else {
                    // Counter-rotated, so the back reads the right way round.
                    Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) { CardBackFace(word) }
                }
            }
        }
        SwipeLabel(offset = offsetX.value, knewLabel = knewLabel, didNotKnowLabel = didNotKnowLabel)
    }
}

@Composable
private fun CardFrontFace(word: Word, cardFront: CardFront) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))
        when (cardFront) {
            CardFront.WORD -> {
                Text(
                    text = word.headword,
                    style = VocabuilderTheme.wordTypography.display,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = word.primarySense.partOfSpeech.label(),
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CardFront.DEFINITION -> {
                Text(
                    text = stringResource(R.string.feature_study_which_word),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = word.primarySense.definition,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = word.primarySense.partOfSpeech.label(),
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(
                if (cardFront == CardFront.WORD) {
                    R.string.feature_study_tap_to_reveal
                } else {
                    R.string.feature_study_tap_to_reveal_word
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CardBackFace(word: Word) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = word.headword, style = VocabuilderTheme.wordTypography.title)
        word.senses.forEachIndexed { index, sense ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (word.senses.size > 1) {
                    Text(
                        text = stringResource(R.string.feature_study_sense_number, index + 1),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = sense.partOfSpeech.label(),
                        style = MaterialTheme.typography.labelLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = sense.definition, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = sense.example,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        RelatedWords(stringResource(R.string.feature_study_synonyms), word.synonyms)
        RelatedWords(stringResource(R.string.feature_study_antonyms), word.antonyms)
        word.note?.let { note ->
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun RelatedWords(title: String, words: List<String>) {
    if (words.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(words.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
    }
}

/** "Knew it" or "Didn't know" in the corner the card is swiped away from, fading in as it goes. */
@Composable
private fun SwipeLabel(offset: Float, knewLabel: String, didNotKnowLabel: String) {
    if (offset == 0f) return
    val knew = offset > 0
    val colors = (if (knew) MasteryState.MASTERED else MasteryState.LEARNING).colors()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Surface(
            color = colors.container,
            contentColor = colors.onContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .align(if (knew) AbsoluteAlignment.TopLeft else AbsoluteAlignment.TopRight)
                .alpha((abs(offset) / SWIPE_LABEL_FULL_OPACITY_PX).coerceIn(0f, 1f)),
        ) {
            Text(
                text = if (knew) knewLabel else didNotKnowLabel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

private const val FLIP_MILLIS = 350
private const val SWIPE_OUT_MILLIS = 200
private const val SWIPE_THRESHOLD = 0.3f
private const val MAX_TILT_DEGREES = 8f
private const val CAMERA_DISTANCE = 12f
private const val SWIPE_LABEL_FULL_OPACITY_PX = 200f
