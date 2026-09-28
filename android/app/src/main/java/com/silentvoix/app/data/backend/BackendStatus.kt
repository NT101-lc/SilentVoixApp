package com.silentvoix.app.data.backend

/** Mirrors `database.status` in the backend's `GET /api/v1/health` response. */
enum class DatabaseState { UP, DOWN, NOT_CONFIGURED, UNKNOWN }

sealed interface BackendStatus {
    data object Checking : BackendStatus

    data class Online(val database: DatabaseState) : BackendStatus

    data object Unreachable : BackendStatus
}
