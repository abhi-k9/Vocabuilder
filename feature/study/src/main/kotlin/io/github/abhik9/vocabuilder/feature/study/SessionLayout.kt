package io.github.abhik9.vocabuilder.feature.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.abhik9.vocabuilder.core.designsystem.theme.MasteryColor
import io.github.abhik9.vocabuilder.core.designsystem.theme.MaxContentWidth
import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.ui.colors

/**
 * A card of a study session, flashcard or quiz: the session's progress at the top, the [card], which should take the
 * height left with `Modifier.weight(1f)`, and its [buttons] at the bottom.
 */
@Composable
internal fun SessionLayout(
    state: StudyUiState.Studying,
    card: @Composable ColumnScope.() -> Unit,
    buttons: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = MaxContentWidth)
            .fillMaxSize(),
    ) {
        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
        card()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = buttons,
        )
    }
}

/** The height of the buttons that answer a card, big enough to hit without looking. */
internal val AnswerButtonHeight = 56.dp

/** The colors of an answer, or of a quiz's choice once picked: green when known or right, amber when not. */
@Composable
internal fun feedbackColors(knew: Boolean): MasteryColor =
    (if (knew) MasteryState.MASTERED else MasteryState.LEARNING).colors()
