package com.silentvoix.app.data.demo

/** Canned recognition output. Not produced by any camera or model. */
data class DemoRecognition(val text: String, val confidencePercent: Int)

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
    val recognitions = listOf(
        DemoRecognition("Xin chào", 94),
        DemoRecognition("Cảm ơn bạn", 91),
        DemoRecognition("Tôi cần giúp đỡ", 88),
        DemoRecognition("Rất vui được gặp bạn", 86),
        DemoRecognition("Bạn có khoẻ không?", 90),
        DemoRecognition("Hẹn gặp lại", 93),
    )

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
