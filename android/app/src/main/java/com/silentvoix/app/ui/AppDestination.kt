package com.silentvoix.app.ui

import androidx.annotation.StringRes
import com.silentvoix.app.R

enum class AppDestination(@StringRes val labelRes: Int, @StringRes val titleRes: Int) {
    TRANSLATE(R.string.nav_translate, R.string.title_translate),
    HISTORY(R.string.nav_history, R.string.title_history),
    SETTINGS(R.string.nav_settings, R.string.title_settings),
}
