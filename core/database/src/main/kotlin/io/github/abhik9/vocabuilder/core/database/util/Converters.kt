package io.github.abhik9.vocabuilder.core.database.util

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Dates are stored as epoch days and instants as epoch milliseconds, so both sort natively. Lists
 * of words are stored one per line: words never contain line breaks.
 */
internal class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun wordsToText(words: List<String>): String = words.joinToString("\n")

    @TypeConverter
    fun textToWords(text: String): List<String> = if (text.isEmpty()) emptyList() else text.split("\n")
}
