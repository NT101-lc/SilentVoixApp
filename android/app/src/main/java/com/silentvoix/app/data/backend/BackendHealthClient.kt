package com.silentvoix.app.data.backend

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * Calls the SilentVoix backend health endpoint. The app only ever talks to the backend;
 * database access stays on the server.
 */
class BackendHealthClient(private val baseUrl: String) {

    suspend fun fetchStatus(): BackendStatus = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("${baseUrl.trimEnd('/')}/api/v1/health").openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                // The backend may wait for a Neon compute to wake up before answering.
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                BackendStatus.Unreachable
            } else {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                BackendStatus.Online(parseDatabaseState(body))
            }
        } catch (e: IOException) {
            BackendStatus.Unreachable
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseDatabaseState(body: String): DatabaseState = try {
        val status = JSONObject(body).optJSONObject("database")?.optString("status")
        DatabaseState.entries.firstOrNull { it.name == status } ?: DatabaseState.UNKNOWN
    } catch (e: JSONException) {
        DatabaseState.UNKNOWN
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000
        const val READ_TIMEOUT_MS = 20_000
    }
}
