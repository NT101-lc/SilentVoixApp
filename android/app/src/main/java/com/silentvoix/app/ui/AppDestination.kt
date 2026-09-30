package com.silentvoix.app.ui

import androidx.annotation.StringRes
import com.silentvoix.app.R

/** Top-level destinations, in navigation order. The app opens on [HOME]. */
enum class AppDestination(@StringRes val labelRes: Int, @StringRes val titleRes: Int) {
    HOME(R.string.nav_home, R.string.title_home),
    TRANSLATE(R.string.nav_translate, R.string.title_translate),
    SPEAK(R.string.nav_speak, R.string.title_speak),
    HISTORY(R.string.nav_history, R.string.title_history),
    SETTINGS(R.string.nav_settings, R.string.title_settings),
}
