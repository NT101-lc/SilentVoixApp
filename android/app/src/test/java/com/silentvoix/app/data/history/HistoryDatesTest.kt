package com.silentvoix.app.data.history

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HistoryDatesTest {

    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val now = millis(2026, 9, 29, 0, 5) // just after midnight

    private fun millis(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `an entry from earlier today is today`() {
        assertEquals(EntryDay.Today, entryDay(millis(2026, 9, 29, 0, 1), now, zone))
    }

    @Test
    fun `an entry from late last night is yesterday, not today`() {
        assertEquals(EntryDay.Yesterday, entryDay(millis(2026, 9, 28, 23, 59), now, zone))
    }

    @Test
    fun `older entries carry their date`() {
        assertEquals(
            EntryDay.Earlier(LocalDate.of(2026, 9, 12)),
            entryDay(millis(2026, 9, 12, 19, 22), now, zone),
        )
    }
}
