package com.silentvoix.app.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.silentvoix.app.data.progress.Milestone
import com.silentvoix.app.speech.SpeechRate
import com.silentvoix.app.ui.settings.AppSettings
import com.silentvoix.app.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    /** A fresh DataStore on the same file, as after an app restart. */
    private fun openStore() = CoroutineScope(Dispatchers.IO + SupervisorJob()).let { scope ->
        scopes += scope
        PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("settings.preferences_pb") }
    }

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    @Test
    fun `defaults are used when nothing is stored`() = runBlocking {
        assertEquals(AppSettings(), SettingsRepository(openStore()).settings.first())
    }

    @Test
    fun `settings survive an app restart`() = runBlocking {
        val changed = AppSettings(
            themeMode = ThemeMode.DARK,
            largeResultText = false,
            autoSpeak = true,
            speechRate = 1.5f,
            haptics = false,
        )
        SettingsRepository(openStore()).update { changed }
        scopes.forEach { it.cancel() } // "process death": the first store is gone.

        assertEquals(changed, SettingsRepository(openStore()).settings.first())
    }

    @Test
    fun `unreadable stored values fall back to safe defaults`() = runBlocking {
        val store = openStore()
        store.edit {
            it[stringPreferencesKey("theme_mode")] = "PURPLE"
            it[floatPreferencesKey("speech_rate")] = 9f
        }

        val settings = SettingsRepository(store).settings.first()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(SpeechRate.MAX, settings.speechRate)
    }

    @Test
    fun `celebrated milestones are remembered across restarts`() = runBlocking {
        SettingsRepository(openStore()).update {
            it.copy(celebratedMilestones = setOf(Milestone.STREAK_3, Milestone.TOTAL_10))
        }
        scopes.forEach { it.cancel() } // "process death": the first store is gone.

        assertEquals(
            setOf(Milestone.STREAK_3, Milestone.TOTAL_10),
            SettingsRepository(openStore()).settings.first().celebratedMilestones,
        )
    }

    @Test
    fun `an unknown stored milestone is ignored, not a crash`() = runBlocking {
        val store = openStore()
        store.edit { it[stringSetPreferencesKey("celebrated_milestones")] = setOf("STREAK_3", "STREAK_9000") }

        assertEquals(setOf(Milestone.STREAK_3), SettingsRepository(store).settings.first().celebratedMilestones)
    }
}
