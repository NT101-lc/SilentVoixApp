package com.silentvoix.app.data.history

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A phrase recognised on the Translate screen and saved on this device. */
data class HistoryEntry(
    val id: Long,
    val text: String,
    val confidencePercent: Int,
    val createdAtMillis: Long,
    val isFavourite: Boolean,
)

/** Which day an entry belongs to, relative to now; drives the "Hôm nay" filter and day labels. */
sealed interface EntryDay {
    data object Today : EntryDay
    data object Yesterday : EntryDay
    data class Earlier(val date: LocalDate) : EntryDay
}

fun entryDay(createdAtMillis: Long, nowMillis: Long, zone: ZoneId): EntryDay {
    val date = Instant.ofEpochMilli(createdAtMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return when (date) {
        today -> EntryDay.Today
        today.minusDays(1) -> EntryDay.Yesterday
        else -> EntryDay.Earlier(date)
    }
}
