package com.silentvoix.app.domain.auth

import kotlinx.coroutines.flow.Flow

/** The signed-in session kept on this device, so the app opens signed in, even offline. */
interface SessionRepository {
    /** Null when nobody is signed in. */
    val session: Flow<Session?>

    suspend fun save(session: Session)

    /** Refreshes who is signed in; does nothing when nobody is. */
    suspend fun updateAccount(account: Account)

    suspend fun clear()
}
