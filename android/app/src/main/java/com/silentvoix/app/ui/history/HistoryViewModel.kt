package com.silentvoix.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silentvoix.app.AppContainer
import com.silentvoix.app.domain.history.HistoryEntry
import com.silentvoix.app.domain.history.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The History screen: loading/error/loaded state, favourites, clearing, and retrying a failed read. */
class HistoryViewModel(
    private val history: HistoryRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    constructor(container: AppContainer) : this(container.historyRepository, container.appScope)

    // Bumping this re-subscribes to the store after an error.
    private val loadAttempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HistoryUiState> = loadAttempt
        .flatMapLatest { history.entries.asHistoryUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState.Loading)

    fun retry() {
        loadAttempt.value++
    }

    fun toggleFavourite(entry: HistoryEntry) {
        appScope.launch { history.setFavourite(entry.id, !entry.isFavourite) }
    }

    fun clearAll() {
        appScope.launch { history.clear() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
