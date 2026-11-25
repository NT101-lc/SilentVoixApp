package com.silentvoix.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silentvoix.app.AppContainer
import com.silentvoix.app.domain.history.HistoryEntry
import com.silentvoix.app.domain.history.HistoryRepository
import com.silentvoix.app.domain.history.HistoryStats
import com.silentvoix.app.domain.progress.Milestone
import com.silentvoix.app.domain.progress.milestoneToCelebrate
import com.silentvoix.app.domain.progress.reachedMilestones
import com.silentvoix.app.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The home dashboard: the device's history, and which milestone (if any) to celebrate. */
class HomeViewModel(
    history: HistoryRepository,
    private val settings: SettingsRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    constructor(container: AppContainer) :
        this(container.historyRepository, container.settingsRepository, container.appScope)

    /** Until the store answers, the dashboard shows zeros rather than a spinner. */
    val entries: StateFlow<List<HistoryEntry>> = history.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _celebration = MutableStateFlow<Milestone?>(null)
    val celebration: StateFlow<Milestone?> = _celebration.asStateFlow()

    /**
     * A new milestone is recorded as celebrated as soon as it shows, together with any smaller ones
     * reached at the same time, so each is celebrated exactly once.
     */
    fun onStats(stats: HistoryStats, celebrated: Set<Milestone>) {
        val next = milestoneToCelebrate(stats, celebrated) ?: return
        _celebration.value = next
        val recorded = celebrated + reachedMilestones(stats)
        appScope.launch { settings.update { it.copy(celebratedMilestones = recorded) } }
    }

    fun dismissCelebration() {
        _celebration.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
