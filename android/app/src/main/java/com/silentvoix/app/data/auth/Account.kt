package com.silentvoix.app.data.auth

/** What a person may do in the app. The server decides; the app only follows. */
enum class UserRole {
    /** The everyday features: translate, speak, history, settings, feedback. */
    USER,

    /** Everything a user has, plus the admin tab: overview, accounts, feedback inbox, server status. */
    ADMIN;

    val apiValue: String get() = name.lowercase()

    companion object {
        /** Anything other than "admin" is an ordinary user: an unknown value never grants more. */
        fun fromApi(value: String?): UserRole = if (value == "admin") ADMIN else USER
    }
}

/** The signed-in person, as the server described them last. */
data class Account(
    val id: String,
    val email: String?,
    val displayName: String?,
    val role: UserRole,
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN

    /** The name to show: the display name, else the part of the e-mail before the @. */
    val shownName: String
        get() = displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@').orEmpty()
}

/** A signed-in session: the bearer token for the API and who it belongs to. */
data class Session(val token: String, val account: Account)
