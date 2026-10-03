package com.silentvoix.app.data.admin

import com.silentvoix.app.data.auth.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

class UserSearchTest {

    private fun user(id: String, email: String?, name: String?) =
        ManagedUser(id, email, name, UserRole.USER, locked = false, createdAtMillis = 0, lastSeenAtMillis = null)

    private val users = listOf(
        user("1", "lan@example.com", "Nguyễn Thị Lan"),
        user("2", "minh@example.com", "Đặng Minh"),
        user("3", null, null),
    )

    @Test
    fun `an empty query keeps everyone`() {
        assertEquals(users, users.matching("  "))
    }

    @Test
    fun `matches the e-mail or the name, ignoring case`() {
        assertEquals(listOf("1"), users.matching("LAN@").map { it.id })
        assertEquals(listOf("2"), users.matching("minh").map { it.id })
    }

    @Test
    fun `vietnamese marks are optional when searching`() {
        assertEquals(listOf("1"), users.matching("nguyen thi").map { it.id })
        assertEquals(listOf("2"), users.matching("dang").map { it.id })
    }
}
