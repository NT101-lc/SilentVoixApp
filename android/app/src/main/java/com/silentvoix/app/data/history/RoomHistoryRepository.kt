package com.silentvoix.app.data.history

import com.silentvoix.app.domain.history.HistoryEntry

import com.silentvoix.app.domain.history.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Recognition history stored on this device (Room). Not synced to the backend. */
class RoomHistoryRepository(
    private val dao: HistoryDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : HistoryRepository {
    override val entries: Flow<List<HistoryEntry>> = dao.observeAll().map { rows ->
        rows.map { HistoryEntry(it.id, it.text, it.confidencePercent, it.createdAtMillis, it.isFavourite) }
    }

    override suspend fun add(text: String, confidencePercent: Int) {
        dao.insert(HistoryEntity(text = text, confidencePercent = confidencePercent, createdAtMillis = clock()))
    }

    override suspend fun setFavourite(id: Long, favourite: Boolean) = dao.setFavourite(id, favourite)

    override suspend fun clear() = dao.clear()
}
