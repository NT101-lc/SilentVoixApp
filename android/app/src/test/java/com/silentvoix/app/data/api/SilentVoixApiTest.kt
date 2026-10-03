package com.silentvoix.app.data.api

import com.silentvoix.app.data.admin.FeedbackKind
import com.silentvoix.app.data.auth.Account
import com.silentvoix.app.data.auth.UserRole
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The client against a scripted server: what it sends, and how it reads each answer. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SilentVoixApiTest {

    private val sent = mutableListOf<HttpRequest>()

    private fun api(status: Int, body: String) = SilentVoixApi("https://api.example.com/", "0.2.0") { request ->
        sent += request
        HttpResponse(status, body)
    }

    private val offline = SilentVoixApi("https://api.example.com", "0.2.0") { throw IOException("no route") }

    private val sessionBody = """
        {"token": "tok-1", "expiresAt": "2026-11-02T08:00:00.123456Z",
         "user": {"id": "u-1", "email": "lan@example.com", "displayName": "Lan", "role": "user"}}"""

    @Test
    fun `signing in posts the address, password and device, and reads the session`() = runBlocking {
        val result = api(200, sessionBody).login("lan@example.com", "mat-khau-dai")

        val request = sent.single()
        assertEquals("POST", request.method)
        assertEquals("https://api.example.com/api/v1/auth/login", request.url)
        val body = JSONObject(request.body!!)
        assertEquals("lan@example.com", body.getString("email"))
        assertEquals("mat-khau-dai", body.getString("password"))
        assertEquals("android", body.getString("platform"))
        assertEquals("0.2.0", body.getString("appVersion"))
        assertNull(request.headers["Authorization"])

        val session = (result as ApiResult.Ok).value
        assertEquals("tok-1", session.token)
        assertEquals(Account("u-1", "lan@example.com", "Lan", UserRole.USER), session.account)
    }

    @Test
    fun `registering sends the name, or null for none`() = runBlocking {
        api(201, sessionBody).register("lan@example.com", "mat-khau-dai", null)

        val body = JSONObject(sent.single().body!!)
        assertEquals("https://api.example.com/api/v1/auth/register", sent.single().url)
        assertTrue(body.isNull("displayName"))
    }

    @Test
    fun `a refusal carries the server's reason`() = runBlocking {
        val result = api(401, """{"error": "invalid_credentials"}""").login("lan@example.com", "wrong")

        assertEquals(ApiResult.Failed(ApiError.INVALID_CREDENTIALS), result)
    }

    @Test
    fun `an unreadable error body still gives an error from the status`() = runBlocking {
        assertEquals(ApiResult.Failed(ApiError.SERVER_UNAVAILABLE), api(502, "<html>Bad gateway</html>").me("tok"))
    }

    @Test
    fun `no network is its own error`() = runBlocking {
        assertEquals(ApiResult.Failed(ApiError.NETWORK), offline.login("lan@example.com", "x"))
    }

    @Test
    fun `signed-in calls send the token as a bearer header`() = runBlocking {
        val result = api(200, """{"id": "u-1", "email": null, "displayName": null, "role": "admin"}""").me("tok-9")

        assertEquals("Bearer tok-9", sent.single().headers["Authorization"])
        assertEquals(Account("u-1", null, null, UserRole.ADMIN), (result as ApiResult.Ok).value)
    }

    @Test
    fun `signing out is a post with an empty answer`() = runBlocking {
        assertEquals(ApiResult.Ok(Unit), api(204, "").logout("tok"))
        assertEquals("https://api.example.com/api/v1/auth/logout", sent.single().url)
    }

    @Test
    fun `feedback is sent with the server's kind name`() = runBlocking {
        api(201, """{"id": "f-1"}""").sendFeedback("tok", FeedbackKind.WRONG_RESULT, "Nhận nhầm")

        val body = JSONObject(sent.single().body!!)
        assertEquals("wrong_result", body.getString("kind"))
        assertEquals("Nhận nhầm", body.getString("message"))
    }

    @Test
    fun `the admin overview is read field by field`() = runBlocking {
        val result = api(200, """
            {"users": 12, "admins": 2, "lockedUsers": 1, "newUsersThisWeek": 4, "activeUsersThisWeek": 7,
             "openFeedback": 3, "totalFeedback": 9}""").overview("tok")

        val overview = (result as ApiResult.Ok).value
        assertEquals(12, overview.users)
        assertEquals(7, overview.activeUsersThisWeek)
        assertEquals(3, overview.openFeedback)
        assertEquals("https://api.example.com/api/v1/admin/overview", sent.single().url)
    }

    @Test
    fun `the user list keeps missing e-mails and never-seen users`() = runBlocking {
        val result = api(200, """[
            {"id": "u-1", "email": "lan@example.com", "displayName": "Lan", "role": "admin", "locked": false,
             "createdAt": "2026-10-01T08:00:00Z", "lastSeenAt": "2026-10-03T07:30:00.5Z"},
            {"id": "u-2", "email": null, "displayName": null, "role": "user", "locked": true,
             "createdAt": "2026-10-02T08:00:00Z", "lastSeenAt": null}]""").users("tok")

        val users = (result as ApiResult.Ok).value
        assertEquals(2, users.size)
        assertEquals(UserRole.ADMIN, users[0].role)
        assertEquals(Instant.parse("2026-10-03T07:30:00.5Z").toEpochMilli(), users[0].lastSeenAtMillis)
        assertNull(users[1].email)
        assertNull(users[1].lastSeenAtMillis)
        assertTrue(users[1].locked)
    }

    @Test
    fun `changing a user sends only what changes`() = runBlocking {
        val user = """{"id": "u-2", "email": "minh@example.com", "displayName": null, "role": "user", "locked": true,
            "createdAt": "2026-10-02T08:00:00Z", "lastSeenAt": null}"""

        api(200, user).updateUser("tok", "u-2", locked = true)

        val request = sent.single()
        assertEquals("PATCH", request.method)
        assertEquals("https://api.example.com/api/v1/admin/users/u-2", request.url)
        val body = JSONObject(request.body!!)
        assertTrue(body.getBoolean("locked"))
        assertTrue(!body.has("role"))
    }

    @Test
    fun `feedback can be fetched open only and is read with its author`() = runBlocking {
        val result = api(200, """[
            {"id": "f-1", "kind": "bug", "message": "Bị treo", "createdAt": "2026-10-03T08:00:00Z",
             "resolvedAt": null, "authorEmail": "lan@example.com", "authorName": "Lan"},
            {"id": "f-2", "kind": "something_new", "message": "?", "createdAt": "2026-10-03T08:00:00Z",
             "resolvedAt": "2026-10-03T09:00:00Z", "authorEmail": null, "authorName": null}]""")
            .feedback("tok", openOnly = true)

        assertEquals("https://api.example.com/api/v1/admin/feedback?status=open", sent.single().url)
        val items = (result as ApiResult.Ok).value
        assertEquals(FeedbackKind.BUG, items[0].kind)
        assertEquals("Lan", items[0].authorName)
        assertNull(items[0].resolvedAtMillis)
        // A kind this version does not know is still shown, just without a label of its own.
        assertNull(items[1].kind)
    }

    @Test
    fun `a malformed success body is an unknown error, not a crash`() = runBlocking {
        assertEquals(ApiResult.Failed(ApiError.UNKNOWN), api(200, "{\"users\": \"many\"}").overview("tok"))
    }
}
