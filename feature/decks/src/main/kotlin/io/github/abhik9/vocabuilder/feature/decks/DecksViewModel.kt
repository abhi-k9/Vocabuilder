package io.github.abhik9.vocabuilder.feature.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveDeckOverviewsUseCase
import io.github.abhik9.vocabuilder.core.model.DeckOverview
import io.github.abhik9.vocabuilder.core.model.Level
import io.github.abhik9.vocabuilder.core.model.MasteryCounts
import io.github.abhik9.vocabuilder.core.model.Pack
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface DecksUiState {
    data object Loading : DecksUiState

    data class Success(val packs: List<PackSection>) : DecksUiState
}

/** A pack with its progress and its decks, grouped by level. */
data class PackSection(
    val pack: Pack,
    val counts: MasteryCounts,
    val levels: List<LevelSection>,
)

data class LevelSection(
    val level: Level,
    val decks: List<DeckOverview>,
)

@HiltViewModel
class DecksViewModel @Inject constructor(
    wordRepository: WordRepository,
    observeDeckOverviews: ObserveDeckOverviewsUseCase,
) : ViewModel() {
    val uiState: StateFlow<DecksUiState> =
        combine(wordRepository.observePacks(), observeDeckOverviews()) { packs, overviews ->
            DecksUiState.Success(packSections(packs, overviews))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DecksUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

internal fun packSections(packs: List<Pack>, overviews: List<DeckOverview>): List<PackSection> {
    val byPack = overviews.groupBy { it.deck.packId }
    return packs.mapNotNull { pack ->
        val decks = byPack[pack.id] ?: return@mapNotNull null
        PackSection(
            pack = pack,
            counts = decks.fold(MasteryCounts()) { total, deck -> total + deck.counts },
            levels = decks.groupBy { it.deck.level }.map { (level, levelDecks) -> LevelSection(level, levelDecks) },
        )
    }
}
