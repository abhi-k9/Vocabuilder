package io.github.abhik9.vocabuilder.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Serif styles for headwords, so a word reads like a dictionary entry, set apart from the
 * interface text around it. They use the system serif font, so no font files are bundled.
 *
 * @property display the word on a flashcard.
 * @property title a word heading a page or a list row.
 */
@Immutable
data class WordTypography(
    val display: TextStyle,
    val title: TextStyle,
)

internal val DefaultWordTypography = WordTypography(
    display = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 48.sp,
    ),
    title = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
)

internal val LocalWordTypography = staticCompositionLocalOf { DefaultWordTypography }
