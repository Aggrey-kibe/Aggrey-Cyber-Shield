package com.aggreyshield.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

/** Collects a one-shot event flow for exactly as long as this composable is in the composition. */
@Composable
fun <T> ObserveEvents(events: Flow<T>, onEvent: suspend (T) -> Unit) {
    LaunchedEffect(Unit) {
        events.collect { onEvent(it) }
    }
}
