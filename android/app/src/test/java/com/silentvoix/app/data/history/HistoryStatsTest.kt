package com.silentvoix.app.data.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HistoryStatsTest {

    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val today: LocalDate = LocalDate.of(2026, 9, 29)
    private val now = at(today, hour = 16)
    private var nextId = 1L

    private fun at(date: LocalDate, hour: Int): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli()

    private fun entry(daysAgo: Long, text: String = "Xin chào", favourite: Boolean = false, hour: Int = 9) =
        HistoryEntry(nextId++, text, 90, at(today.minusDays(daysAgo), hour), favourite)

    @Test
    fun `no history gives zeros, an empty week and no top phrase`() {
        val stats = historyStats(emptyList(), now, zone)
        assertEquals(0, stats.total)
        assertEquals(0, stats.today)
        assertEquals(0, stats.favourites)
        assertEquals(0, stats.streakDays)
        assertEquals(0, stats.weekTotal)
        assertNull(stats.topPhrase)
    }

    @Test
    fun `the week is seven days ending today, oldest first, with empty days kept`() {
        val stats = historyStats(listOf(entry(0), entry(0), entry(2), entry(6), entry(7)), now, zone)
        assertEquals((6L downTo 0L).map { today.minusDays(it) }, stats.week.map { it.date })
        assertEquals(listOf(1, 0, 0, 0, 1, 0, 2), stats.week.map { it.count })
        assertEquals(4, stats.weekTotal)
        assertEquals(5, stats.total)
        assertEquals(2, stats.today)
    }

    @Test
    fun `an entry just before midnight belongs to the day before`() {
        val stats = historyStats(listOf(entry(1, hour = 23)), now, zone)
        assertEquals(0, stats.today)
        assertEquals(1, stats.week[5].count)
    }

    @Test
    fun `a streak counts consecutive days back from today`() {
        val stats = historyStats(listOf(entry(0), entry(1), entry(2), entry(4)), now, zone)
        assertEquals(3, stats.streakDays)
    }

    @Test
    fun `a streak survives a today with nothing yet`() {
        val stats = historyStats(listOf(entry(1), entry(2)), now, zone)
        assertEquals(2, stats.streakDays)
    }

    @Test
    fun `a whole missed day ends the streak`() {
        val stats = historyStats(listOf(entry(2), entry(3)), now, zone)
        assertEquals(0, stats.streakDays)
    }

    @Test
    fun `favourites and the most used phrase are counted over all history`() {
        val entries = listOf(
            entry(0, "Đồng ý"),
            entry(1, "Xin chào", favourite = true),
            entry(20, "Xin chào"),
            entry(30, "Dừng lại", favourite = true),
        )
        val stats = historyStats(entries, now, zone)
        assertEquals(2, stats.favourites)
        assertEquals("Xin chào", stats.topPhrase)
    }

    @Test
    fun `a tie for most used goes to the most recent phrase`() {
        // Entries arrive newest first, as the repository delivers them.
        val stats = historyStats(listOf(entry(0, "Đồng ý"), entry(1, "Xin chào")), now, zone)
        assertEquals("Đồng ý", stats.topPhrase)
    }
}
