package io.github.abhik9.vocabuilder.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import java.io.IOException
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Reads and writes [UserSettings] in a Preferences DataStore. Missing values use the defaults. */
class UserSettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<UserSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toUserSettings() }

    suspend fun setThemeMode(themeMode: ThemeMode) = dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }

    suspend fun setUseDynamicColor(useDynamicColor: Boolean) =
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = useDynamicColor }

    suspend fun setNewWordsPerDay(count: Int) = dataStore.edit {
        it[Keys.NEW_WORDS_PER_DAY] = count.coerceIn(UserSettings.NEW_WORDS_PER_DAY_RANGE)
    }

    suspend fun setCardFront(cardFront: CardFront) = dataStore.edit { it[Keys.CARD_FRONT] = cardFront.name }

    suspend fun setAutoPronounce(autoPronounce: Boolean) = dataStore.edit { it[Keys.AUTO_PRONOUNCE] = autoPronounce }

    suspend fun setShuffleCards(shuffle: Boolean) = dataStore.edit { it[Keys.SHUFFLE_CARDS] = shuffle }

    suspend fun setReminderEnabled(enabled: Boolean) = dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }

    suspend fun setReminderTime(time: LocalTime) = dataStore.edit {
        it[Keys.REMINDER_MINUTE_OF_DAY] = time.hour * MINUTES_PER_HOUR + time.minute
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            themeMode = enumOrDefault(this[Keys.THEME_MODE], defaults.themeMode),
            useDynamicColor = this[Keys.USE_DYNAMIC_COLOR] ?: defaults.useDynamicColor,
            newWordsPerDay = (this[Keys.NEW_WORDS_PER_DAY] ?: defaults.newWordsPerDay)
                .coerceIn(UserSettings.NEW_WORDS_PER_DAY_RANGE),
            cardFront = enumOrDefault(this[Keys.CARD_FRONT], defaults.cardFront),
            autoPronounce = this[Keys.AUTO_PRONOUNCE] ?: defaults.autoPronounce,
            shuffleCards = this[Keys.SHUFFLE_CARDS] ?: defaults.shuffleCards,
            reminderEnabled = this[Keys.REMINDER_ENABLED] ?: defaults.reminderEnabled,
            reminderTime = this[Keys.REMINDER_MINUTE_OF_DAY]
                ?.takeIf { it in 0 until MINUTES_PER_DAY }
                ?.let { LocalTime.of(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR) }
                ?: defaults.reminderTime,
        )
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val NEW_WORDS_PER_DAY = intPreferencesKey("new_words_per_day")
        val CARD_FRONT = stringPreferencesKey("card_front")
        val AUTO_PRONOUNCE = booleanPreferencesKey("auto_pronounce")
        val SHUFFLE_CARDS = booleanPreferencesKey("shuffle_cards")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTE_OF_DAY = intPreferencesKey("reminder_minute_of_day")
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    }
}
