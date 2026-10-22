package io.github.abhik9.vocabuilder.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors telling answers apart in charts: words known, and words missed. Blue and amber rather than the mastery
 * colors' green and amber, which red-green colorblind readers can't tell apart; each pair is checked for color
 * vision deficiency, lightness and contrast against its theme's surface.
 */
@Immutable
data class AnswerColors(
    val knew: Color,
    val missed: Color,
)

val LightAnswerColors = AnswerColors(knew = Color(0xFF0359C5), missed = Color(0xFF815600))

val DarkAnswerColors = AnswerColors(knew = Color(0xFF5D8DEB), missed = Color(0xFFBC8320))

internal val LocalAnswerColors = staticCompositionLocalOf { LightAnswerColors }
