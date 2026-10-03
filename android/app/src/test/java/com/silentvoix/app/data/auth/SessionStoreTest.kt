package com.silentvoix.app.data.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    private fun openStore() = CoroutineScope(Dispatchers.IO + SupervisorJob()).let { scope ->
        scopes += scope
        PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("session.preferences_pb") }
    }

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    private val admin = Session("tok-123", Account("id-1", "admin@silentvoix.local", "Quản trị viên", UserRole.ADMIN))

    @Test
    fun `nobody is signed in at first`() = runBlocking {
        assertNull(SessionStore(openStore()).session.first())
    }

    @Test
    fun `a session survives an app restart`() = runBlocking {
        SessionStore(openStore()).save(admin)
        scopes.forEach { it.coroutineContext.job.cancelAndJoin() }

        assertEquals(admin, SessionStore(openStore()).session.first())
    }

    @Test
    fun `an account without a name or e-mail is kept as such`() = runBlocking {
        val anonymous = Session("tok", Account("id-2", null, null, UserRole.USER))
        val store = SessionStore(openStore())

        store.save(anonymous)

        assertEquals(anonymous, store.session.first())
    }

    @Test
    fun `the account can be refreshed without signing in again`() = runBlocking {
        val store = SessionStore(openStore())
        store.save(admin)

        store.updateAccount(admin.account.copy(role = UserRole.USER))

        assertEquals(admin.copy(account = admin.account.copy(role = UserRole.USER)), store.session.first())
    }

    @Test
    fun `refreshing the account while signed out does not sign anyone in`() = runBlocking {
        val store = SessionStore(openStore())

        store.updateAccount(admin.account)

        assertNull(store.session.first())
    }

    @Test
    fun `signing out forgets the token`() = runBlocking {
        val store = SessionStore(openStore())
        store.save(admin)

        store.clear()

        assertNull(store.session.first())
    }

    @Test
    fun `an unknown stored role falls back to the least privilege`() = runBlocking {
        val dataStore = openStore()
        SessionStore(dataStore).save(admin)
        dataStore.edit { it[stringPreferencesKey("role")] = "superuser" }

        assertEquals(UserRole.USER, SessionStore(dataStore).session.first()?.account?.role)
    }
}
