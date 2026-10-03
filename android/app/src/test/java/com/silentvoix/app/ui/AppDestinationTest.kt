package com.silentvoix.app.ui

import com.silentvoix.app.data.auth.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {

    @Test
    fun `a user gets the five everyday tabs and no admin tab`() {
        assertEquals(
            listOf(AppDestination.HOME, AppDestination.TRANSLATE, AppDestination.SPEAK, AppDestination.HISTORY, AppDestination.SETTINGS),
            destinationsFor(UserRole.USER),
        )
    }

    @Test
    fun `an admin gets everything a user has plus the admin tab before settings`() {
        val tabs = destinationsFor(UserRole.ADMIN)

        assertTrue(tabs.containsAll(destinationsFor(UserRole.USER)))
        assertEquals(AppDestination.ADMIN, tabs[tabs.size - 2])
        assertEquals(AppDestination.SETTINGS, tabs.last())
    }

    @Test
    fun `losing the admin role while on the admin tab goes home`() {
        assertEquals(AppDestination.HOME, AppDestination.ADMIN.allowedFor(UserRole.USER))
        assertEquals(AppDestination.ADMIN, AppDestination.ADMIN.allowedFor(UserRole.ADMIN))
        assertEquals(AppDestination.HISTORY, AppDestination.HISTORY.allowedFor(UserRole.USER))
        assertFalse(AppDestination.ADMIN in destinationsFor(UserRole.USER))
    }
}
