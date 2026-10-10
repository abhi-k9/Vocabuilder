package io.github.abhik9.vocabuilder.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors for how well a word is known. They stay fixed under dynamic color, so
 * "mastered" always reads as green and "learning" as amber whatever the wallpaper.
 */
@Immutable
data class MasteryColors(
    val new: MasteryColor,
    val learning: MasteryColor,
    val reviewing: MasteryColor,
    val mastered: MasteryColor,
)

/**
 * @property accent for small marks such as dots and progress bar segments on the surface color.
 * @property container a tinted background, with [onContainer] for text on it.
 */
@Immutable
data class MasteryColor(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

// Tones 40/90/30 (light) and 80/30/90 (dark) of Material tonal palettes: a near-neutral one on the
// brand hue for new words, then amber, blue and green.

val LightMasteryColors = MasteryColors(
    new = MasteryColor(accent = Color(0xFF595F60), container = Color(0xFFDEE4E4), onContainer = Color(0xFF424849)),
    learning = MasteryColor(accent = Color(0xFF815600), container = Color(0xFFFFDDB1), onContainer = Color(0xFF624000)),
    reviewing = MasteryColor(
        accent = Color(0xFF0359C5),
        container = Color(0xFFD9E2FF),
        onContainer = Color(0xFF004397),
    ),
    mastered = MasteryColor(accent = Color(0xFF006E2E), container = Color(0xFFC9ECC8), onContainer = Color(0xFF005321)),
)

val DarkMasteryColors = MasteryColors(
    new = MasteryColor(accent = Color(0xFFC1C8C8), container = Color(0xFF424849), onContainer = Color(0xFFDEE4E4)),
    learning = MasteryColor(accent = Color(0xFFFFBA4A), container = Color(0xFF624000), onContainer = Color(0xFFFFDDB1)),
    reviewing = MasteryColor(
        accent = Color(0xFFAFC6FF),
        container = Color(0xFF004397),
        onContainer = Color(0xFFD9E2FF),
    ),
    mastered = MasteryColor(accent = Color(0xFF70DD85), container = Color(0xFF005321), onContainer = Color(0xFFC9ECC8)),
)

internal val LocalMasteryColors = staticCompositionLocalOf { LightMasteryColors }
