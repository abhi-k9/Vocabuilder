package io.github.abhik9.vocabuilder.core.data.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.abhik9.vocabuilder.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.vocabuilder.core.content.AssetContentFiles
import io.github.abhik9.vocabuilder.core.content.ContentLoader
import io.github.abhik9.vocabuilder.core.data.DataDiagnosticsSection
import io.github.abhik9.vocabuilder.core.data.repository.BundledContentRepository
import io.github.abhik9.vocabuilder.core.data.repository.BundledDictionaryRepository
import io.github.abhik9.vocabuilder.core.data.repository.ContentResolverDocumentStore
import io.github.abhik9.vocabuilder.core.data.repository.DataStoreUserSettingsRepository
import io.github.abhik9.vocabuilder.core.data.repository.RoomProgressRepository
import io.github.abhik9.vocabuilder.core.data.repository.RoomWordRepository
import io.github.abhik9.vocabuilder.core.domain.repository.ContentRepository
import io.github.abhik9.vocabuilder.core.domain.repository.DictionaryRepository
import io.github.abhik9.vocabuilder.core.domain.repository.DocumentStore
import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindsWordRepository(repository: RoomWordRepository): WordRepository

    @Binds
    abstract fun bindsProgressRepository(repository: RoomProgressRepository): ProgressRepository

    @Binds
    abstract fun bindsUserSettingsRepository(repository: DataStoreUserSettingsRepository): UserSettingsRepository

    @Binds
    abstract fun bindsContentRepository(repository: BundledContentRepository): ContentRepository

    @Binds
    abstract fun bindsDictionaryRepository(repository: BundledDictionaryRepository): DictionaryRepository

    @Binds
    abstract fun bindsDocumentStore(store: ContentResolverDocumentStore): DocumentStore

    @Binds
    @IntoSet
    abstract fun bindsDataDiagnosticsSection(section: DataDiagnosticsSection): DiagnosticsSection

    companion object {
        @Provides
        fun providesContentLoader(@ApplicationContext context: Context): ContentLoader =
            ContentLoader(AssetContentFiles(context.assets))
    }
}
