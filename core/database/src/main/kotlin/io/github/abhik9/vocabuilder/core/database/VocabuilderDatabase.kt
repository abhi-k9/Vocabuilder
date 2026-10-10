package io.github.abhik9.vocabuilder.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.abhik9.vocabuilder.core.database.dao.ContentDao
import io.github.abhik9.vocabuilder.core.database.dao.ProgressDao
import io.github.abhik9.vocabuilder.core.database.model.DeckEntity
import io.github.abhik9.vocabuilder.core.database.model.PackEntity
import io.github.abhik9.vocabuilder.core.database.model.ProgressEntity
import io.github.abhik9.vocabuilder.core.database.model.ReviewEntity
import io.github.abhik9.vocabuilder.core.database.model.SenseEntity
import io.github.abhik9.vocabuilder.core.database.model.WordEntity
import io.github.abhik9.vocabuilder.core.database.util.Converters

/**
 * The app's single database. When changing the schema, bump [version] and add an
 * [androidx.room.AutoMigration] (or a manual migration) so learners keep their progress.
 */
@Database(
    entities = [
        PackEntity::class,
        DeckEntity::class,
        WordEntity::class,
        SenseEntity::class,
        ProgressEntity::class,
        ReviewEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class VocabuilderDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao

    abstract fun progressDao(): ProgressDao

    companion object {
        const val NAME = "vocabuilder.db"
    }
}
