package com.silentvoix.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.silentvoix.app.ui.theme.EyebrowStyle
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
        if (status.database == DatabaseState.UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
}

/** Server reachability with a manual re-check; the dot is decorative, the text says it all. */
@Composable
fun BackendStatusRow(status: BackendStatus, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 18.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_about_backend_status).uppercase(),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(backendStatusColor(status)),
                )
                Text(text = backendStatusText(status), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (status is BackendStatus.Checking) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(14.dp)
                    .size(20.dp),
                strokeWidth = 2.5.dp,
            )
        } else {
            IconButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.status_backend_retry))
            }
        }
    }
}
