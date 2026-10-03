package com.silentvoix.app.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The same rules the backend applies, checked before anything is sent. */
class AuthFormTest {

    @Test
    fun `a well-formed sign-in passes`() {
        assertTrue(validateSignIn(" Lan@Example.com ", "anything").isValid)
    }

    @Test
    fun `sign-in needs an address that looks like one and some password`() {
        val errors = validateSignIn("lan@", "")

        assertEquals(FormError.EMAIL_INVALID, errors.email)
        assertEquals(FormError.PASSWORD_MISSING, errors.password)
        assertFalse(errors.isValid)
    }

    @Test
    fun `sign-in does not judge an existing password's length`() {
        // Old or seeded passwords may follow other rules; the server decides.
        assertNull(validateSignIn("lan@example.com", "short").password)
    }

    @Test
    fun `registering needs at least eight characters`() {
        assertEquals(FormError.PASSWORD_TOO_SHORT, validateRegister("lan@example.com", "1234567", "1234567", "").password)
        assertNull(validateRegister("lan@example.com", "12345678", "12345678", "").password)
    }

    @Test
    fun `a password over 72 bytes is refused even if it has few characters`() {
        val vietnamese = "ấ".repeat(25) // 25 characters, 75 bytes

        assertEquals(FormError.PASSWORD_TOO_LONG, validateRegister("lan@example.com", vietnamese, vietnamese, "").password)
    }

    @Test
    fun `the two passwords must match`() {
        assertEquals(FormError.PASSWORDS_DIFFER, validateRegister("lan@example.com", "12345678", "12345679", "").confirm)
    }

    @Test
    fun `a name is optional but at most 80 characters`() {
        assertNull(validateRegister("lan@example.com", "12345678", "12345678", "").name)
        assertEquals(FormError.NAME_TOO_LONG, validateRegister("lan@example.com", "12345678", "12345678", "x".repeat(81)).name)
    }

    @Test
    fun `addresses are sent trimmed and in lower case`() {
        assertEquals("lan@example.com", normalizeEmail("  Lan@Example.COM "))
    }
}
