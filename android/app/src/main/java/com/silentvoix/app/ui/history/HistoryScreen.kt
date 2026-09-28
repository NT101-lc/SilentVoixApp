package com.silentvoix.app.ui.history

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silentvoix.app.R
import com.silentvoix.app.data.demo.DemoData
import com.silentvoix.app.data.demo.DemoHistoryEntry
import com.silentvoix.app.ui.common.DemoBadge

/** Lets the demo preview each screen state; real loading arrives with database-backed history. */
private enum class HistoryViewState(@StringRes val labelRes: Int) {
    CONTENT(R.string.history_state_content),
    EMPTY(R.string.history_state_empty),
    ERROR(R.string.history_state_error),
}

@Composable
fun HistoryScreen(
    onNavigateToTranslate: () -> Unit,
    onShowMessage: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var viewState by rememberSaveable { mutableStateOf(HistoryViewState.CONTENT) }
    val ttsUnavailable = stringResource(R.string.message_tts_unavailable)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        DemoStateSelector(selected = viewState, onSelect = { viewState = it })
        when (viewState) {
            HistoryViewState.CONTENT -> HistoryList(
                entries = DemoData.history,
                onReplay = { onShowMessage(ttsUnavailable) },
            )
            HistoryViewState.EMPTY -> MessageState(
                icon = ImageVector.vectorResource(R.drawable.ic_history),
                title = stringResource(R.string.history_empty_title),
                body = stringResource(R.string.history_empty_body),
                actionLabel = stringResource(R.string.history_empty_action),
                onAction = onNavigateToTranslate,
            )
            HistoryViewState.ERROR -> MessageState(
                icon = Icons.Filled.Warning,
                title = stringResource(R.string.history_error_title),
                body = stringResource(R.string.history_error_body),
                actionLabel = stringResource(R.string.history_error_action),
                onAction = { viewState = HistoryViewState.CONTENT },
                isError = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoStateSelector(selected: HistoryViewState, onSelect: (HistoryViewState) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.history_state_selector_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryViewState.entries.forEach { state ->
                val isSelected = state == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(state) },
                    label = { Text(stringResource(state.labelRes)) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryList(entries: List<DemoHistoryEntry>, onReplay: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DemoBadge()
                Text(
                    text = stringResource(R.string.history_demo_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        items(entries, key = { it.id }) { entry ->
            HistoryItem(entry = entry, onReplay = onReplay)
        }
    }
}

@Composable
private fun HistoryItem(entry: DemoHistoryEntry, onReplay: () -> Unit) {
    val replayDescription = stringResource(R.string.history_replay_description, entry.text)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)) {
            Column(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = entry.text, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.history_item_meta, entry.timeLabel, entry.confidencePercent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onReplay,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = replayDescription },
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_volume_up), contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.action_replay))
            }
        }
    }
}

@Composable
private fun MessageState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    isError: Boolean = false,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                    if (isError) liveRegion = LiveRegionMode.Assertive
                },
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onAction,
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}
