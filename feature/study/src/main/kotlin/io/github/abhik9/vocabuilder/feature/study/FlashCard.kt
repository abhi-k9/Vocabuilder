package io.github.abhik9.vocabuilder.feature.study

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
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
import io.github.abhik9.vocabuilder.core.ui.Senses
import io.github.abhik9.vocabuilder.core.ui.colors
import io.github.abhik9.vocabuilder.core.ui.label
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * A flashcard that turns over when tapped. Once turned, it can be swiped right for "knew it" or
 * left for "didn't know", far enough or with a flick; the same answers are offered as accessibility
 * actions. Its back lists the
 * word's synonyms and antonyms, to look up.
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
    onLookUp: (headword: String) -> Unit,
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
                if (!isFlipped) {
                    // Not answerable before it's turned over: its drags are taken, so they don't count as taps
                    // that would turn it over.
                    detectHorizontalDragGestures { change, _ -> change.consume() }
                    return@pointerInput
                }
                val threshold = size.width * SWIPE_THRESHOLD
                val flingMinDistance = size.width * FLING_MIN_DISTANCE
                val flingVelocity = FLING_VELOCITY_DP_PER_SECOND.dp.toPx()
                val velocityTracker = VelocityTracker()
                // The drag as the finger moved, which the card's offset follows a little later.
                var distance = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                        distance = 0f
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity().x
                        // Far enough, or a quick flick the way it was dragged.
                        val flung = abs(distance) > flingMinDistance && abs(velocity) > flingVelocity &&
                            velocity * distance > 0f
                        val knew = distance > 0
                        val answered = abs(distance) > threshold || flung
                        scope.launch {
                            if (answered) {
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
                    distance += dragAmount
                    velocityTracker.addPosition(change.uptimeMillis, Offset(distance, 0f))
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
                    Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                        CardBackFace(word, onLookUp)
                    }
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
private fun CardBackFace(word: Word, onLookUp: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = word.headword, style = VocabuilderTheme.wordTypography.title)
        Senses(word.senses)
        RelatedWords(stringResource(R.string.feature_study_synonyms), word.synonyms, onLookUp)
        RelatedWords(stringResource(R.string.feature_study_antonyms), word.antonyms, onLookUp)
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

/** Synonyms or antonyms as chips that look the word up: its meanings, and a button to hear it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelatedWords(
    title: String,
    words: List<String>,
    onLookUp: (String) -> Unit,
) {
    if (words.isEmpty()) return
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            words.forEach { related ->
                AssistChip(
                    onClick = { onLookUp(related) },
                    label = { Text(related) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
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
private const val FLING_MIN_DISTANCE = 0.1f
private const val FLING_VELOCITY_DP_PER_SECOND = 600
private const val MAX_TILT_DEGREES = 8f
private const val CAMERA_DISTANCE = 12f
private const val SWIPE_LABEL_FULL_OPACITY_PX = 200f
