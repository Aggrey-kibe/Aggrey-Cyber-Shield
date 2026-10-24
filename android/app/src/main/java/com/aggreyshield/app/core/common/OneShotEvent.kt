package com.aggreyshield.app.core.common

/** A single-fire UI event: shown once, then gone. Never re-delivered on recomposition or rotation. */
sealed interface OneShotEvent {
    data class ShowMessage(val message: String) : OneShotEvent
    data class Navigate(val route: String, val popUpToRoute: String? = null, val inclusive: Boolean = false) : OneShotEvent
    data object NavigateBack : OneShotEvent
}
