package io.github.abhik9.vocabuilder.core.speech.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.vocabuilder.core.domain.speech.Pronouncer
import io.github.abhik9.vocabuilder.core.speech.TextToSpeechPronouncer

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SpeechModule {
    @Binds
    abstract fun bindsPronouncer(pronouncer: TextToSpeechPronouncer): Pronouncer
}
