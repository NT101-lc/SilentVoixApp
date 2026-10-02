package com.silentvoix.app.data.progress

import com.silentvoix.app.data.history.HistoryStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MilestonesTest {

    private fun stats(total: Int = 0, streak: Int = 0) =
        HistoryStats(total = total, today = 0, favourites = 0, week = emptyList(), streakDays = streak, topPhrase = null)

    @Test
    fun `nothing to celebrate at the start`() {
        assertNull(milestoneToCelebrate(stats(), celebrated = emptySet()))
        assertEquals(emptySet<Milestone>(), reachedMilestones(stats(total = 9, streak = 2)))
    }

    @Test
    fun `a three-day streak is celebrated once`() {
        assertEquals(Milestone.STREAK_3, milestoneToCelebrate(stats(streak = 3), celebrated = emptySet()))
        assertNull(milestoneToCelebrate(stats(streak = 3), celebrated = setOf(Milestone.STREAK_3)))
    }

    @Test
    fun `when several are new, only the biggest is celebrated`() {
        // Say history is restored on a new phone: 55 phrases at once is one celebration, not two.
        assertEquals(Milestone.TOTAL_50, milestoneToCelebrate(stats(total = 55), celebrated = emptySet()))
        assertEquals(setOf(Milestone.TOTAL_10, Milestone.TOTAL_50), reachedMilestones(stats(total = 55)))
    }

    @Test
    fun `a longer streak outranks a phrase count`() {
        assertEquals(Milestone.STREAK_7, milestoneToCelebrate(stats(total = 12, streak = 7), celebrated = emptySet()))
    }

    @Test
    fun `an old milestone is not brought back after a bigger one`() {
        assertNull(
            milestoneToCelebrate(stats(total = 120, streak = 30), celebrated = Milestone.entries.toSet()),
        )
    }
}
