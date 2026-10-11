package io.github.abhik9.vocabuilder.core.model

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    /** New words the daily review introduces each day, on top of the words due. */
    val newWordsPerDay: Int = DEFAULT_NEW_WORDS_PER_DAY,
    /** What the front of a flashcard shows. */
    val cardFront: CardFront = CardFront.WORD,
    /** Say the word aloud when the card is turned over. */
    val autoPronounce: Boolean = false,
    /** Show cards in random order, rather than in their deck's order, which is alphabetical. */
    val shuffleCards: Boolean = true,
) {
    companion object {
        const val DEFAULT_NEW_WORDS_PER_DAY = 10
        val NEW_WORDS_PER_DAY_RANGE = 0..50
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class CardFront {
    /** The word; recall its meaning. */
    WORD,

    /** The definition; recall the word. */
    DEFINITION,
}
