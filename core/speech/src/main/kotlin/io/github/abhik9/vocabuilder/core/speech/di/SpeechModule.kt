package io.github.abhik9.vocabuilder.core.speech.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.abhik9.vocabuilder.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.vocabuilder.core.domain.speech.Pronouncer
import io.github.abhik9.vocabuilder.core.speech.SpeechDiagnosticsSection
import io.github.abhik9.vocabuilder.core.speech.TextToSpeechPronouncer

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SpeechModule {
    @Binds
    abstract fun bindsPronouncer(pronouncer: TextToSpeechPronouncer): Pronouncer

    @Binds
    @IntoSet
    abstract fun bindsSpeechDiagnosticsSection(section: SpeechDiagnosticsSection): DiagnosticsSection
}
