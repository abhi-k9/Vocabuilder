package io.github.abhik9.vocabuilder.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Goes back, from a top app bar. */
@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.core_ui_back))
    }
}

/** Stars a word, or takes its star away. */
@Composable
fun StarButton(starred: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = stringResource(if (starred) R.string.core_ui_unstar else R.string.core_ui_star),
        )
    }
}

/** Says a word aloud. */
@Composable
fun PronounceButton(onPronounce: () -> Unit) {
    IconButton(onClick = onPronounce) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.core_ui_pronounce))
    }
}
