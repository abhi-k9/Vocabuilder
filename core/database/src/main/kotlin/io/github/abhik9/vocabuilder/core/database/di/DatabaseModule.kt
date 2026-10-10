package io.github.abhik9.vocabuilder.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.vocabuilder.core.database.VocabuilderDatabase
import io.github.abhik9.vocabuilder.core.database.dao.ContentDao
import io.github.abhik9.vocabuilder.core.database.dao.ProgressDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): VocabuilderDatabase =
        Room.databaseBuilder(context, VocabuilderDatabase::class.java, VocabuilderDatabase.NAME).build()

    @Provides
    fun providesContentDao(database: VocabuilderDatabase): ContentDao = database.contentDao()

    @Provides
    fun providesProgressDao(database: VocabuilderDatabase): ProgressDao = database.progressDao()
}
