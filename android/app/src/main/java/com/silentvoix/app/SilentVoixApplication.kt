package com.silentvoix.app

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.silentvoix.app.data.api.SilentVoixApi
import com.silentvoix.app.data.auth.SessionStore
import com.silentvoix.app.data.history.HistoryDatabase
import com.silentvoix.app.data.history.HistoryRepository
import com.silentvoix.app.data.phrases.PhraseRepository
import com.silentvoix.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

private val android.content.Context.settingsDataStore by preferencesDataStore(name = "settings")
private val android.content.Context.phrasesDataStore by preferencesDataStore(name = "phrases")
private val android.content.Context.sessionDataStore by preferencesDataStore(name = "session")

/** Holds the app-wide stores. Created once per process. */
class SilentVoixApplication : Application() {

    /** For writes that must finish even if the screen that started them goes away. */
    val appScope = CoroutineScope(SupervisorJob())

    val settingsRepository by lazy { SettingsRepository(settingsDataStore) }

    val phraseRepository by lazy { PhraseRepository(phrasesDataStore) }

    /** Who is signed in. Excluded from backups (res/xml/backup_rules.xml, data_extraction_rules.xml). */
    val sessionStore by lazy { SessionStore(sessionDataStore) }

    val api by lazy { SilentVoixApi(BuildConfig.BACKEND_BASE_URL, BuildConfig.VERSION_NAME) }

    val historyRepository by lazy {
        val db = Room.databaseBuilder(this, HistoryDatabase::class.java, "history.db").build()
        HistoryRepository(db.historyDao())
    }
}
