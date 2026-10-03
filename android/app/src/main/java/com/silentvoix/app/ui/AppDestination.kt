package com.silentvoix.app.ui

import androidx.annotation.StringRes
import com.silentvoix.app.R
import com.silentvoix.app.data.auth.UserRole

/** Top-level destinations, in navigation order. The app opens on [HOME]. */
enum class AppDestination(@StringRes val labelRes: Int, @StringRes val titleRes: Int) {
    HOME(R.string.nav_home, R.string.title_home),
    TRANSLATE(R.string.nav_translate, R.string.title_translate),
    SPEAK(R.string.nav_speak, R.string.title_speak),
    HISTORY(R.string.nav_history, R.string.title_history),

    /** Admins only: overview, accounts, feedback inbox and server status. */
    ADMIN(R.string.nav_admin, R.string.title_admin),
    SETTINGS(R.string.nav_settings, R.string.title_settings);

    /** This destination if [role] may open it, else home. */
    fun allowedFor(role: UserRole): AppDestination = if (this in destinationsFor(role)) this else HOME
}

/** The tabs [role] sees. Hiding a tab is only presentation: the server refuses admin calls itself. */
fun destinationsFor(role: UserRole): List<AppDestination> =
    AppDestination.entries.filter { it != AppDestination.ADMIN || role == UserRole.ADMIN }
