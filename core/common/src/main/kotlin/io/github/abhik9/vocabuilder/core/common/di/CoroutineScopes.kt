package io.github.abhik9.vocabuilder.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** A scope that lives as long as the application process, for work that outlives a screen. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopesModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun providesApplicationScope(
        @Dispatcher(AppDispatcher.Default) dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher + backgroundErrorHandler)
}

/**
 * Background work failing, such as refreshing content, shouldn't take the whole app down: the
 * error is printed instead, and the rest of the work in the scope carries on.
 */
private val backgroundErrorHandler = CoroutineExceptionHandler { _, error -> error.printStackTrace() }
