package com.silentvoix.app.domain.history

import kotlinx.coroutines.flow.Flow

/** Recognition history kept on this device. Not synced to the backend. */
interface HistoryRepository {
    /** Newest first. */
    val entries: Flow<List<HistoryEntry>>

    suspend fun add(text: String, confidencePercent: Int)

    suspend fun setFavourite(id: Long, favourite: Boolean)

    /** Deletes every entry, favourites included. */
    suspend fun clear()
}
