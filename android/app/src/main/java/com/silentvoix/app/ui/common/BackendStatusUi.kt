package com.silentvoix.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.silentvoix.app.R
import com.silentvoix.app.data.backend.BackendStatus
import com.silentvoix.app.data.backend.DatabaseState

@Composable
fun backendStatusText(status: BackendStatus): String = stringResource(
    when (status) {
        BackendStatus.Checking -> R.string.status_backend_checking
        BackendStatus.Unreachable -> R.string.status_backend_unreachable
        is BackendStatus.Online -> when (status.database) {
            DatabaseState.UP -> R.string.status_backend_db_up
            DatabaseState.DOWN -> R.string.status_backend_db_down
            DatabaseState.NOT_CONFIGURED -> R.string.status_backend_db_not_configured
            DatabaseState.UNKNOWN -> R.string.status_backend_db_unknown
        }
    },
)

/** Supplementary indicator colour; the status is always also given as text. */
@Composable
fun backendStatusColor(status: BackendStatus): Color = when (status) {
    BackendStatus.Checking -> MaterialTheme.colorScheme.outline
    BackendStatus.Unreachable -> MaterialTheme.colorScheme.error
    is BackendStatus.Online ->
        if (status.database == DatabaseState.UP) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
}
