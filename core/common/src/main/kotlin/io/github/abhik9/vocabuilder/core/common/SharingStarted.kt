package io.github.abhik9.vocabuilder.core.common

import kotlinx.coroutines.flow.SharingStarted

/**
 * Shares a screen's state while the screen is shown, and for five seconds after, so that turning the phone, which
 * recreates the screen, doesn't start it over.
 */
val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)
