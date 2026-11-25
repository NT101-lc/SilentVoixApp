package com.silentvoix.app.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.silentvoix.app.AppContainer

/**
 * The activity's [VM] (one per [key]), made by [build] from the app's [AppContainer] the first
 * time it is asked for, and kept across recompositions and configuration changes.
 *
 * A stand-in for `lifecycle-viewmodel-compose`'s `viewModel {}`, which would pull the whole of
 * Compose up to a newer version than the BOM pins.
 */
@Composable
inline fun <reified VM : ViewModel> rememberViewModel(
    key: String? = null,
    noinline build: (AppContainer) -> VM,
): VM {
    val owner = LocalActivity.current as ViewModelStoreOwner
    val container = LocalContext.current.applicationContext as AppContainer
    return remember(owner, key) {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = build(container) as T
        }
        val provider = ViewModelProvider(owner, factory)
        if (key == null) provider[VM::class.java] else provider[key, VM::class.java]
    }
}
