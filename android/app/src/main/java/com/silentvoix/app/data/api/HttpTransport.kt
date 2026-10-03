package com.silentvoix.app.data.api

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HttpRequest(val method: String, val url: String, val headers: Map<String, String>, val body: String?)

data class HttpResponse(val status: Int, val body: String)

/** Sends one request; throws [IOException] when the server cannot be reached. */
fun interface HttpTransport {
    suspend fun execute(request: HttpRequest): HttpResponse
}

/** The platform's HttpURLConnection, off the main thread. */
class UrlConnectionTransport : HttpTransport {

    override suspend fun execute(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val connection = URL(request.url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            // The backend may wait for a Neon compute to wake up before answering.
            connection.readTimeout = READ_TIMEOUT_MS
            request.headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (request.body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(request.body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            HttpResponse(status, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 20_000
    }
}
