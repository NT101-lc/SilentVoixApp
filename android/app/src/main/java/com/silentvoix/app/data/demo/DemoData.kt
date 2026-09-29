package com.silentvoix.app.data.demo

/** Canned local history entry. Not stored or synced anywhere. */
data class DemoHistoryEntry(
    val id: Int,
    val text: String,
    val dayLabel: String,
    val timeLabel: String,
    val confidencePercent: Int,
    val isToday: Boolean,
)

object DemoData {
    val history = listOf(
        DemoHistoryEntry(1, "Xin chào, tôi tên là Lan", "Hôm nay", "09:12", 92, isToday = true),
        DemoHistoryEntry(2, "Cho tôi một cốc nước", "Hôm nay", "08:47", 89, isToday = true),
        DemoHistoryEntry(3, "Nhà vệ sinh ở đâu?", "Hôm qua", "17:30", 87, isToday = false),
        DemoHistoryEntry(4, "Cảm ơn bác sĩ", "Hôm qua", "10:05", 95, isToday = false),
        DemoHistoryEntry(5, "Tôi bị dị ứng với hải sản", "12/09", "19:22", 84, isToday = false),
        DemoHistoryEntry(6, "Tôi muốn đặt lịch khám", "12/09", "14:08", 91, isToday = false),
    )

    /** Entries that start out marked as favourites in the demo. */
    val defaultFavoriteIds = setOf(4, 5)
}
