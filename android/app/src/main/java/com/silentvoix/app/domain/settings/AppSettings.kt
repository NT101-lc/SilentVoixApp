package com.silentvoix.app.domain.settings

import com.silentvoix.app.domain.progress.Milestone
import com.silentvoix.app.speech.SpeechRate

/** User preferences, persisted by [SettingsRepository]. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val largeResultText: Boolean = true,
    val autoSpeak: Boolean = false,
    val speechRate: Float = SpeechRate.DEFAULT,
    val haptics: Boolean = true,
    /** Milestones the home screen has already celebrated, so each is shown once. */
    val celebratedMilestones: Set<Milestone> = emptySet(),
)
