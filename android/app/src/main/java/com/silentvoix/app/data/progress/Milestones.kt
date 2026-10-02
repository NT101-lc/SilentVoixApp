package com.silentvoix.app.data.progress

import com.silentvoix.app.data.history.HistoryStats

/** Moments worth a small celebration on the home screen. Each is celebrated at most once. */
enum class Milestone(val kind: Kind, val threshold: Int) {
    STREAK_3(Kind.STREAK, 3),
    STREAK_7(Kind.STREAK, 7),
    STREAK_30(Kind.STREAK, 30),
    TOTAL_10(Kind.TOTAL, 10),
    TOTAL_50(Kind.TOTAL, 50),
    TOTAL_100(Kind.TOTAL, 100),
    ;

    enum class Kind { STREAK, TOTAL }
}

/** Rarest first: when several are new at once, the rarest is the one shown. */
private val ByRarity = listOf(
    Milestone.STREAK_30,
    Milestone.TOTAL_100,
    Milestone.STREAK_7,
    Milestone.TOTAL_50,
    Milestone.STREAK_3,
    Milestone.TOTAL_10,
)

fun reachedMilestones(stats: HistoryStats): Set<Milestone> = Milestone.entries.filter {
    when (it.kind) {
        Milestone.Kind.STREAK -> stats.streakDays >= it.threshold
        Milestone.Kind.TOTAL -> stats.total >= it.threshold
    }
}.toSet()

/**
 * The milestone to celebrate now, or null. Only one is shown even if several are new; the caller
 * then records every reached milestone as celebrated, so smaller ones do not follow in a queue.
 */
fun milestoneToCelebrate(stats: HistoryStats, celebrated: Set<Milestone>): Milestone? {
    val reached = reachedMilestones(stats)
    return ByRarity.firstOrNull { it in reached && it !in celebrated }
}
