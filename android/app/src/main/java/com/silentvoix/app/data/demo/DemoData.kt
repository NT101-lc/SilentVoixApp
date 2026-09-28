package com.silentvoix.app.data.demo

/** Canned recognition output. Not produced by any camera or model. */
data class DemoRecognition(val text: String, val confidencePercent: Int)

/** Canned local history entry. Not stored or synced anywhere. */
data class DemoHistoryEntry(
    val id: Int,
    val text: String,
    val timeLabel: String,
    val confidencePercent: Int,
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
        DemoHistoryEntry(1, "Xin chào, tôi tên là Lan", "Hôm nay · 09:12", 92),
        DemoHistoryEntry(2, "Cho tôi một cốc nước", "Hôm nay · 08:47", 89),
        DemoHistoryEntry(3, "Nhà vệ sinh ở đâu?", "Hôm qua · 17:30", 87),
        DemoHistoryEntry(4, "Cảm ơn bác sĩ", "Hôm qua · 10:05", 95),
        DemoHistoryEntry(5, "Tôi bị dị ứng với hải sản", "12/09 · 19:22", 84),
    )
}
