package io.github.abhik9.vocabuilder.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.vocabuilder.BuildConfig
import io.github.abhik9.vocabuilder.core.common.AppInfo
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providesAppInfo(): AppInfo = AppInfo(
        versionName = BuildConfig.VERSION_NAME,
        sourceCodeUrl = BuildConfig.SOURCE_CODE_URL,
    )
}
