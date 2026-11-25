package com.silentvoix.app.domain.settings

import kotlinx.coroutines.flow.Flow

/** Where [AppSettings] are kept. Missing or unreadable values come back as defaults. */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}
