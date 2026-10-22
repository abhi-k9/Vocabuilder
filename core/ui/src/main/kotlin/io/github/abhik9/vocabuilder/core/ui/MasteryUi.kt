package io.github.abhik9.vocabuilder.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.abhik9.vocabuilder.core.designsystem.component.Pill
import io.github.abhik9.vocabuilder.core.designsystem.component.ProgressSegment
import io.github.abhik9.vocabuilder.core.designsystem.component.SegmentedProgressBar
import io.github.abhik9.vocabuilder.core.designsystem.theme.MasteryColor
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme
import io.github.abhik9.vocabuilder.core.model.MasteryCounts
import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.model.WordProgress

@Composable
@ReadOnlyComposable
fun MasteryState.colors(): MasteryColor {
    val colors = VocabuilderTheme.masteryColors
    return when (this) {
        MasteryState.NEW -> colors.new
        MasteryState.LEARNING -> colors.learning
        MasteryState.REVIEWING -> colors.reviewing
        MasteryState.MASTERED -> colors.mastered
    }
}

@Composable
@ReadOnlyComposable
fun MasteryState.label(): String = stringResource(
    when (this) {
        MasteryState.NEW -> R.string.core_ui_mastery_new
        MasteryState.LEARNING -> R.string.core_ui_mastery_learning
        MasteryState.REVIEWING -> R.string.core_ui_mastery_reviewing
        MasteryState.MASTERED -> R.string.core_ui_mastery_mastered
    },
)

/** A word's mastery state as a tinted pill; words marked as known read "Known". */
@Composable
fun MasteryPill(progress: WordProgress, modifier: Modifier = Modifier) {
    val state = progress.masteryState
    val colors = state.colors()
    Pill(
        text = if (progress.alreadyKnown) stringResource(R.string.core_ui_mastery_known) else state.label(),
        containerColor = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier,
    )
}

/** "12 of 50 words mastered". */
@Composable
@ReadOnlyComposable
fun MasteryCounts.masteredText(): String =
    pluralStringResource(R.plurals.core_ui_progress_description, total, mastered, total)

/** Mastered, reviewing and learning words as segments of a bar, on a track for new words. */
@Composable
fun MasteryProgressBar(counts: MasteryCounts, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val description = counts.masteredText()
    SegmentedProgressBar(
        segments = listOf(
            ProgressSegment(counts.mastered, MasteryState.MASTERED.colors().accent),
            ProgressSegment(counts.reviewing, MasteryState.REVIEWING.colors().accent),
            ProgressSegment(counts.learning, MasteryState.LEARNING.colors().accent),
        ),
        total = counts.total,
        height = height,
        modifier = modifier.semantics { contentDescription = description },
    )
}

/** The number of words in each state, each next to a dot of its color. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasteryLegend(counts: MasteryCounts, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            MasteryState.MASTERED to counts.mastered,
            MasteryState.REVIEWING to counts.reviewing,
            MasteryState.LEARNING to counts.learning,
            MasteryState.NEW to counts.new,
        ).forEach { (state, count) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(state.colors().accent, CircleShape),
                )
                Text(
                    text = stringResource(R.string.core_ui_mastery_legend_item, state.label(), count),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
