package com.silentvoix.app.ui.settings

import com.silentvoix.app.speech.SpeechRate
import com.silentvoix.app.ui.theme.ThemeMode

/** User preferences, persisted by [com.silentvoix.app.data.settings.SettingsRepository]. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val largeResultText: Boolean = true,
    val autoSpeak: Boolean = false,
    val speechRate: Float = SpeechRate.DEFAULT,
    val haptics: Boolean = true,
)
