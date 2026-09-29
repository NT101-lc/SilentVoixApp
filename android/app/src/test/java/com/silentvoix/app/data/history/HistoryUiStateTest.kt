package com.silentvoix.app.data.history

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class HistoryUiStateTest {

    private val entry = HistoryEntry(1, "Xin chào", 92, createdAtMillis = 0L, isFavourite = false)

    @Test
    fun `shows loading until the store delivers entries`() = runBlocking {
        val states = flowOf(listOf(entry)).asHistoryUiState().toList()

        assertEquals(listOf(HistoryUiState.Loading, HistoryUiState.Loaded(listOf(entry))), states)
    }

    @Test
    fun `an empty store is loaded with no entries, not an error`() = runBlocking {
        val states = flowOf(emptyList<HistoryEntry>()).asHistoryUiState().toList()

        assertEquals(HistoryUiState.Loaded(emptyList()), states.last())
    }

    @Test
    fun `a failing store shows the error state`() = runBlocking {
        val failing = flow<List<HistoryEntry>> { throw IOException("disk") }

        assertEquals(HistoryUiState.Error, failing.asHistoryUiState().toList().last())
    }
}
