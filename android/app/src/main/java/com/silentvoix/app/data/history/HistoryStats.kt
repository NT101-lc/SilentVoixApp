package com.silentvoix.app.data.history

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How many phrases were recognised on one calendar day. */
data class DayCount(val date: LocalDate, val count: Int)

/** The home dashboard's numbers, all derived from the history stored on this device. */
data class HistoryStats(
    val total: Int,
    val today: Int,
    val favourites: Int,
    /** The last [WEEK_DAYS] days, oldest first, ending today; days with nothing have a count of 0. */
    val week: List<DayCount>,
    /** Days in a row with at least one phrase, counting back from today (or from yesterday if today is still empty). */
    val streakDays: Int,
    /** The phrase recognised most often, or null with no history. Ties go to the most recent. */
    val topPhrase: String?,
) {
    val weekTotal: Int get() = week.sumOf { it.count }

    companion object {
        const val WEEK_DAYS = 7
    }
}

fun historyStats(entries: List<HistoryEntry>, nowMillis: Long, zone: ZoneId): HistoryStats {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val perDay = entries.groupingBy { Instant.ofEpochMilli(it.createdAtMillis).atZone(zone).toLocalDate() }.eachCount()

    // A streak is not lost until a whole day has gone by, so an empty today starts from yesterday.
    var day = if (perDay.containsKey(today)) today else today.minusDays(1)
    var streak = 0
    while (perDay.containsKey(day)) {
        streak++
        day = day.minusDays(1)
    }

    // Entries arrive newest first and maxByOrNull keeps the first maximum: ties go to the most recent.
    val topPhrase = entries.groupingBy { it.text }.eachCount().maxByOrNull { it.value }?.key

    return HistoryStats(
        total = entries.size,
        today = perDay[today] ?: 0,
        favourites = entries.count { it.isFavourite },
        week = (HistoryStats.WEEK_DAYS - 1 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            DayCount(date, perDay[date] ?: 0)
        },
        streakDays = streak,
        topPhrase = topPhrase,
    )
}
