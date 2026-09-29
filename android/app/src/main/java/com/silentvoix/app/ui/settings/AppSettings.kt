package com.silentvoix.app.ui.settings

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.silentvoix.app.speech.SpeechRate
import com.silentvoix.app.ui.theme.ThemeMode

/**
 * In-memory user preferences. Survives configuration changes but not app restarts;
 * persistent storage comes in a later phase.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val largeResultText: Boolean = true,
    val autoSpeak: Boolean = false,
    val speechRate: Float = SpeechRate.DEFAULT,
    val haptics: Boolean = true,
) {
    companion object {
        val Saver: Saver<AppSettings, Any> = listSaver(
            save = {
                listOf(it.themeMode.name, it.largeResultText, it.autoSpeak, it.speechRate, it.haptics)
            },
            restore = {
                AppSettings(
                    themeMode = ThemeMode.valueOf(it[0] as String),
                    largeResultText = it[1] as Boolean,
                    autoSpeak = it[2] as Boolean,
                    speechRate = it[3] as Float,
                    haptics = it[4] as Boolean,
                )
            },
        )
    }
}
