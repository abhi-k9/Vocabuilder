package io.github.abhik9.vocabuilder.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme
import io.github.abhik9.vocabuilder.core.model.WordSummary

/** A word in a list: its headword and first meaning, a star if it's starred, and its mastery. */
@Composable
fun WordSummaryRow(word: WordSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(word.headword, style = VocabuilderTheme.wordTypography.title) },
        supportingContent = {
            Text(
                text = "${word.partOfSpeech.label(short = true)} ${word.definition}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (word.progress.starred) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.core_ui_starred),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
                MasteryPill(word.progress)
            }
        },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}
