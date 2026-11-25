package com.silentvoix.app

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.silentvoix.app.data.api.SilentVoixApi
import com.silentvoix.app.data.auth.SessionStore
import com.silentvoix.app.data.backend.BackendHealthClient
import com.silentvoix.app.data.history.HistoryDatabase
import com.silentvoix.app.data.history.RoomHistoryRepository
import com.silentvoix.app.data.phrases.DataStorePhraseRepository
import com.silentvoix.app.data.settings.DataStoreSettingsRepository
import com.silentvoix.app.domain.admin.AdminRepository
import com.silentvoix.app.domain.auth.AuthRepository
import com.silentvoix.app.domain.auth.SessionRepository
import com.silentvoix.app.domain.backend.BackendHealthRepository
import com.silentvoix.app.domain.feedback.FeedbackRepository
import com.silentvoix.app.domain.history.HistoryRepository
import com.silentvoix.app.domain.phrases.PhraseRepository
import com.silentvoix.app.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

private val android.content.Context.settingsDataStore by preferencesDataStore(name = "settings")
private val android.content.Context.phrasesDataStore by preferencesDataStore(name = "phrases")
private val android.content.Context.sessionDataStore by preferencesDataStore(name = "session")

/** Wires the repository layer once per process; the only place that names the implementations. */
class SilentVoixApplication : Application(), AppContainer {

    override val appScope = CoroutineScope(SupervisorJob())

    override val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(settingsDataStore) }

    override val phraseRepository: PhraseRepository by lazy { DataStorePhraseRepository(phrasesDataStore) }

    /** Who is signed in. Excluded from backups (res/xml/backup_rules.xml, data_extraction_rules.xml). */
    override val sessionRepository: SessionRepository by lazy { SessionStore(sessionDataStore) }

    private val api by lazy { SilentVoixApi(BuildConfig.BACKEND_BASE_URL, BuildConfig.VERSION_NAME) }

    override val authRepository: AuthRepository get() = api

    override val adminRepository: AdminRepository get() = api

    override val feedbackRepository: FeedbackRepository get() = api

    override val backendHealthRepository: BackendHealthRepository by lazy {
        BackendHealthClient(BuildConfig.BACKEND_BASE_URL)
    }

    override val historyRepository: HistoryRepository by lazy {
        val db = Room.databaseBuilder(this, HistoryDatabase::class.java, "history.db").build()
        RoomHistoryRepository(db.historyDao())
    }
}
