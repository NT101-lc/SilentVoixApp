package com.silentvoix.app.data.api

import com.silentvoix.app.data.admin.AdminOverview
import com.silentvoix.app.data.admin.FeedbackItem
import com.silentvoix.app.data.admin.FeedbackKind
import com.silentvoix.app.data.admin.ManagedUser
import com.silentvoix.app.data.auth.Account
import com.silentvoix.app.data.auth.Session
import com.silentvoix.app.data.auth.UserRole
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The SilentVoix backend's account, feedback and admin endpoints. The app only ever talks to the
 * backend; the database stays on the server. Admin calls are refused by the server (403) for
 * anyone but an admin, whatever the app shows.
 */
class SilentVoixApi(
    baseUrl: String,
    private val appVersion: String,
    private val transport: HttpTransport = UrlConnectionTransport(),
) {
    private val base = baseUrl.trimEnd('/')

    suspend fun login(email: String, password: String): ApiResult<Session> =
        call("POST", "/api/v1/auth/login", token = null, body = device().put("email", email).put("password", password)) {
            session(JSONObject(it))
        }

    suspend fun register(email: String, password: String, displayName: String?): ApiResult<Session> =
        call(
            "POST", "/api/v1/auth/register", token = null,
            body = device().put("email", email).put("password", password).put("displayName", displayName ?: JSONObject.NULL),
        ) { session(JSONObject(it)) }

    suspend fun me(token: String): ApiResult<Account> =
        call("GET", "/api/v1/auth/me", token) { account(JSONObject(it)) }

    suspend fun logout(token: String): ApiResult<Unit> =
        call("POST", "/api/v1/auth/logout", token, body = JSONObject()) { }

    suspend fun sendFeedback(token: String, kind: FeedbackKind, message: String): ApiResult<Unit> =
        call("POST", "/api/v1/feedback", token, body = JSONObject().put("kind", kind.apiValue).put("message", message)) { }

    suspend fun overview(token: String): ApiResult<AdminOverview> =
        call("GET", "/api/v1/admin/overview", token) {
            val json = JSONObject(it)
            AdminOverview(
                users = json.getInt("users"),
                admins = json.getInt("admins"),
                lockedUsers = json.getInt("lockedUsers"),
                newUsersThisWeek = json.getInt("newUsersThisWeek"),
                activeUsersThisWeek = json.getInt("activeUsersThisWeek"),
                openFeedback = json.getInt("openFeedback"),
                totalFeedback = json.getInt("totalFeedback"),
            )
        }

    suspend fun users(token: String): ApiResult<List<ManagedUser>> =
        call("GET", "/api/v1/admin/users", token) { body -> JSONArray(body).objects().map(::managedUser) }

    /** Changes only the fields given. */
    suspend fun updateUser(token: String, id: String, role: UserRole? = null, locked: Boolean? = null): ApiResult<ManagedUser> {
        val change = JSONObject()
        if (role != null) change.put("role", role.apiValue)
        if (locked != null) change.put("locked", locked)
        return call("PATCH", "/api/v1/admin/users/$id", token, body = change) { managedUser(JSONObject(it)) }
    }

    suspend fun feedback(token: String, openOnly: Boolean): ApiResult<List<FeedbackItem>> =
        call("GET", "/api/v1/admin/feedback?status=${if (openOnly) "open" else "all"}", token) { body ->
            JSONArray(body).objects().map(::feedbackItem)
        }

    suspend fun setResolved(token: String, id: String, resolved: Boolean): ApiResult<FeedbackItem> =
        call("PATCH", "/api/v1/admin/feedback/$id", token, body = JSONObject().put("resolved", resolved)) {
            feedbackItem(JSONObject(it))
        }

    private fun device() = JSONObject().put("platform", "android").put("appVersion", appVersion)

    private suspend fun <T> call(
        method: String,
        path: String,
        token: String?,
        body: JSONObject? = null,
        parse: (String) -> T,
    ): ApiResult<T> {
        val headers = buildMap {
            put("Accept", "application/json")
            if (body != null) put("Content-Type", "application/json; charset=utf-8")
            if (token != null) put("Authorization", "Bearer $token")
        }
        val response = try {
            transport.execute(HttpRequest(method, base + path, headers, body?.toString()))
        } catch (e: IOException) {
            return ApiResult.Failed(ApiError.NETWORK)
        }
        if (response.status !in 200..299) {
            return ApiResult.Failed(ApiError.from(response.status, errorCode(response.body)))
        }
        return try {
            ApiResult.Ok(parse(response.body))
        } catch (e: JSONException) {
            ApiResult.Failed(ApiError.UNKNOWN)
        } catch (e: DateTimeParseException) {
            ApiResult.Failed(ApiError.UNKNOWN)
        }
    }

    private fun errorCode(body: String): String? = try {
        JSONObject(body).optString("error").takeIf { it.isNotEmpty() }
    } catch (e: JSONException) {
        null
    }

    private fun session(json: JSONObject) = Session(json.getString("token"), account(json.getJSONObject("user")))

    private fun account(json: JSONObject) = Account(
        id = json.getString("id"),
        email = json.stringOrNull("email"),
        displayName = json.stringOrNull("displayName"),
        role = UserRole.fromApi(json.stringOrNull("role")),
    )

    private fun managedUser(json: JSONObject) = ManagedUser(
        id = json.getString("id"),
        email = json.stringOrNull("email"),
        displayName = json.stringOrNull("displayName"),
        role = UserRole.fromApi(json.stringOrNull("role")),
        locked = json.getBoolean("locked"),
        createdAtMillis = millis(json.getString("createdAt")),
        lastSeenAtMillis = json.stringOrNull("lastSeenAt")?.let(::millis),
    )

    private fun feedbackItem(json: JSONObject) = FeedbackItem(
        id = json.getString("id"),
        kind = FeedbackKind.fromApi(json.stringOrNull("kind")),
        message = json.getString("message"),
        createdAtMillis = millis(json.getString("createdAt")),
        resolvedAtMillis = json.stringOrNull("resolvedAt")?.let(::millis),
        authorEmail = json.stringOrNull("authorEmail"),
        authorName = json.stringOrNull("authorName"),
    )

    private fun millis(instant: String) = Instant.parse(instant).toEpochMilli()

    private fun JSONObject.stringOrNull(name: String): String? = if (isNull(name)) null else getString(name)

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
}
