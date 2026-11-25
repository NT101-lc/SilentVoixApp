package com.silentvoix.app.domain.backend

/** The backend's health, as admins see it on the server-status row. */
interface BackendHealthRepository {
    suspend fun fetchStatus(): BackendStatus
}
