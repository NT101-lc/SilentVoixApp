package com.silentvoix.app.data.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object Error : HistoryUiState

    /** Loaded from the store; an empty list is a real "no history yet", not a failure. */
    data class Loaded(val entries: List<HistoryEntry>) : HistoryUiState
}

/** Maps the store's entries to screen state: loading first, then content, or an error if reading fails. */
fun Flow<List<HistoryEntry>>.asHistoryUiState(): Flow<HistoryUiState> =
    map<List<HistoryEntry>, HistoryUiState> { HistoryUiState.Loaded(it) }
        .onStart { emit(HistoryUiState.Loading) }
        .catch { emit(HistoryUiState.Error) }
