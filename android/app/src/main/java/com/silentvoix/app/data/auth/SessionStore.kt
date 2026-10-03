package com.silentvoix.app.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The signed-in session, kept in its own Preferences DataStore ("session") so the app opens signed
 * in, even offline. The file is left out of backups (res/xml/backup_rules.xml), so a token never
 * travels to another device.
 */
class SessionStore(private val dataStore: DataStore<Preferences>) {

    val session: Flow<Session?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val token = prefs[TOKEN]
            val id = prefs[USER_ID]
            if (token.isNullOrEmpty() || id.isNullOrEmpty()) {
                null
            } else {
                Session(token, Account(id, prefs[EMAIL], prefs[DISPLAY_NAME], UserRole.fromApi(prefs[ROLE])))
            }
        }

    suspend fun save(session: Session) {
        dataStore.edit { prefs ->
            prefs[TOKEN] = session.token
            prefs.putAccount(session.account)
        }
    }

    /** Refreshes who is signed in; does nothing when nobody is. */
    suspend fun updateAccount(account: Account) {
        dataStore.edit { prefs ->
            if (prefs[TOKEN] != null) prefs.putAccount(account)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.putAccount(account: Account) {
        this[USER_ID] = account.id
        this[ROLE] = account.role.apiValue
        if (account.email != null) this[EMAIL] = account.email else remove(EMAIL)
        if (account.displayName != null) this[DISPLAY_NAME] = account.displayName else remove(DISPLAY_NAME)
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val ROLE = stringPreferencesKey("role")
    }
}
