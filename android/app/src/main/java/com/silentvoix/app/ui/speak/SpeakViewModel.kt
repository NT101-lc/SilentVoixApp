package com.silentvoix.app.ui.speak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silentvoix.app.AppContainer
import com.silentvoix.app.domain.phrases.PhraseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Speak screen's saved phrases. Writes outlive the screen ([appScope]). */
class SpeakViewModel(
    private val phrases: PhraseRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    constructor(container: AppContainer) : this(container.phraseRepository, container.appScope)

    val saved: StateFlow<List<String>> = phrases.phrases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun save(text: String) {
        appScope.launch { phrases.add(text) }
    }

    fun remove(phrase: String) {
        appScope.launch { phrases.remove(phrase) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
