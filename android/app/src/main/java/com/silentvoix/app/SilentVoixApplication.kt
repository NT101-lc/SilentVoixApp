package com.silentvoix.app

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.silentvoix.app.data.history.HistoryDatabase
import com.silentvoix.app.data.history.HistoryRepository
import com.silentvoix.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

private val android.content.Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Holds the app-wide stores. Created once per process. */
class SilentVoixApplication : Application() {

    /** For writes that must finish even if the screen that started them goes away. */
    val appScope = CoroutineScope(SupervisorJob())

    val settingsRepository by lazy { SettingsRepository(settingsDataStore) }

    val historyRepository by lazy {
        val db = Room.databaseBuilder(this, HistoryDatabase::class.java, "history.db").build()
        HistoryRepository(db.historyDao())
    }
}
