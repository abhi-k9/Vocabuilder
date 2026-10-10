package io.github.abhik9.vocabuilder.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme

/** A part of a [SegmentedProgressBar]: [count] of the total, drawn in [color]. */
@Immutable
data class ProgressSegment(val count: Int, val color: Color)

/**
 * A rounded bar split into colored segments in proportion to their counts out of [total], on a
 * track for the rest. Decorative: describe the progress in text or semantics next to it.
 */
@Composable
fun SegmentedProgressBar(
    segments: List<ProgressSegment>,
    total: Int,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        if (total <= 0) return@Canvas
        val clip = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, radius))
        }
        clipPath(clip) {
            var start = 0f
            segments.forEach { segment ->
                val width = size.width * segment.count.coerceAtLeast(0) / total
                if (width > 0f) {
                    val x = if (rtl) size.width - start - width else start
                    drawRect(color = segment.color, topLeft = Offset(x, 0f), size = Size(width, size.height))
                    start += width
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedProgressBarPreview() {
    VocabuilderTheme(dynamicColor = false) {
        val colors = VocabuilderTheme.masteryColors
        SegmentedProgressBar(
            segments = listOf(
                ProgressSegment(12, colors.mastered.accent),
                ProgressSegment(9, colors.reviewing.accent),
                ProgressSegment(5, colors.learning.accent),
            ),
            total = 50,
        )
    }
}
