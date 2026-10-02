package com.silentvoix.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.silentvoix.app.data.progress.Milestone
import com.silentvoix.app.speech.SpeechRate
import com.silentvoix.app.ui.settings.AppSettings
import com.silentvoix.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [AppSettings] persisted in a Preferences DataStore, so they survive app restarts. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.THEME_MODE] = next.themeMode.name
            prefs[Keys.LARGE_RESULT_TEXT] = next.largeResultText
            prefs[Keys.AUTO_SPEAK] = next.autoSpeak
            prefs[Keys.SPEECH_RATE] = next.speechRate
            prefs[Keys.HAPTICS] = next.haptics
            prefs[Keys.CELEBRATED_MILESTONES] = next.celebratedMilestones.map { it.name }.toSet()
        }
    }

    /** Missing or unreadable values (an unknown theme name, an out-of-range rate) fall back safely. */
    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == this[Keys.THEME_MODE] }
                ?: defaults.themeMode,
            largeResultText = this[Keys.LARGE_RESULT_TEXT] ?: defaults.largeResultText,
            autoSpeak = this[Keys.AUTO_SPEAK] ?: defaults.autoSpeak,
            speechRate = (this[Keys.SPEECH_RATE] ?: defaults.speechRate)
                .coerceIn(SpeechRate.MIN, SpeechRate.MAX),
            haptics = this[Keys.HAPTICS] ?: defaults.haptics,
            // Names this version does not know (from a newer one, say) are dropped.
            celebratedMilestones = this[Keys.CELEBRATED_MILESTONES].orEmpty()
                .mapNotNull { name -> Milestone.entries.firstOrNull { it.name == name } }
                .toSet(),
        )
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LARGE_RESULT_TEXT = booleanPreferencesKey("large_result_text")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val HAPTICS = booleanPreferencesKey("haptics")
        val CELEBRATED_MILESTONES = stringSetPreferencesKey("celebrated_milestones")
    }
}
