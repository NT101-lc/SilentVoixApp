package com.silentvoix.app.ui.history

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silentvoix.app.R
import com.silentvoix.app.data.history.EntryDay
import com.silentvoix.app.data.history.HistoryEntry
import com.silentvoix.app.data.history.HistoryRepository
import com.silentvoix.app.data.history.HistoryUiState
import com.silentvoix.app.data.history.asHistoryUiState
import com.silentvoix.app.data.history.entryDay
import com.silentvoix.app.ui.common.ScreenHeader
import com.silentvoix.app.ui.common.SectionLabel
import com.silentvoix.app.ui.common.SegmentedControl
import com.silentvoix.app.ui.common.rememberHapticTap
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal enum class HistoryFilter(@StringRes val labelRes: Int) {
    ALL(R.string.history_filter_all),
    TODAY(R.string.history_filter_today),
    FAVOURITES(R.string.history_filter_favourites),
}

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val ContentMaxWidth = 720.dp

@Composable
fun HistoryScreen(
    historyRepository: HistoryRepository,
    onToggleFavourite: (HistoryEntry) -> Unit,
    onClearAll: () -> Unit,
    onNavigateToTranslate: () -> Unit,
    hapticsEnabled: Boolean,
    onSpeak: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
    // Bumping this re-subscribes to the store after an error.
    var loadAttempt by remember { mutableIntStateOf(0) }
    val state by remember(historyRepository, loadAttempt) {
        historyRepository.entries.asHistoryUiState()
    }.collectAsStateWithLifecycle(initialValue = HistoryUiState.Loading)
    val tap = rememberHapticTap(hapticsEnabled)

    HistoryContent(
        state = state,
        filter = filter,
        onFilterChange = {
            tap()
            filter = it
        },
        nowMillis = System.currentTimeMillis(),
        zone = ZoneId.systemDefault(),
        onToggleFavourite = {
            tap()
            onToggleFavourite(it)
        },
        onReplay = {
            tap()
            onSpeak(it)
        },
        onClearAll = {
            tap()
            onClearAll()
        },
        onRetry = {
            tap()
            loadAttempt++
        },
        onNavigateToTranslate = {
            tap()
            onNavigateToTranslate()
        },
        contentPadding = contentPadding,
    )
}

/** Stateless History screen; [nowMillis] and [zone] decide "today" so screenshots are stable. */
@Composable
internal fun HistoryContent(
    state: HistoryUiState,
    filter: HistoryFilter,
    onFilterChange: (HistoryFilter) -> Unit,
    nowMillis: Long,
    zone: ZoneId,
    onToggleFavourite: (HistoryEntry) -> Unit,
    onReplay: (String) -> Unit,
    onClearAll: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToTranslate: () -> Unit,
    contentPadding: PaddingValues,
) {
    val total = (state as? HistoryUiState.Loaded)?.entries?.size
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    if (confirmClear && total != null) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_body, total)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.history_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.history_clear_cancel)) }
            },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.title_history),
                supporting = if (total != null && total > 0) {
                    stringResource(R.string.history_summary, total)
                } else {
                    stringResource(R.string.history_local_note)
                },
                action = if (total != null && total > 0) {
                    {
                        TextButton(
                            onClick = { confirmClear = true },
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.history_clear))
                        }
                    }
                } else {
                    null
                },
            )
            SegmentedControl(
                options = HistoryFilter.entries,
                selected = filter,
                label = { stringResource(it.labelRes) },
                onSelect = onFilterChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth(),
        ) {
            when (state) {
                HistoryUiState.Loading -> LoadingState()
                HistoryUiState.Error -> MessageState(
                    iconRes = R.drawable.ic_history,
                    title = stringResource(R.string.history_error_title),
                    body = stringResource(R.string.history_error_body),
                    action = stringResource(R.string.history_error_retry),
                    onAction = onRetry,
                )
                is HistoryUiState.Loaded -> {
                    val entries = when (filter) {
                        HistoryFilter.ALL -> state.entries
                        HistoryFilter.TODAY -> state.entries.filter {
                            entryDay(it.createdAtMillis, nowMillis, zone) == EntryDay.Today
                        }
                        HistoryFilter.FAVOURITES -> state.entries.filter { it.isFavourite }
                    }
                    if (entries.isEmpty()) {
                        EmptyState(
                            filter = filter,
                            onPrimaryAction = {
                                if (filter == HistoryFilter.ALL) onNavigateToTranslate() else onFilterChange(HistoryFilter.ALL)
                            },
                        )
                    } else {
                        HistoryList(entries, nowMillis, zone, onToggleFavourite, onReplay)
                    }
                }
            }
        }
    }
}

/** Entries grouped under one heading per day, each day one rounded card of rows. */
@Composable
private fun HistoryList(
    entries: List<HistoryEntry>,
    nowMillis: Long,
    zone: ZoneId,
    onToggleFavourite: (HistoryEntry) -> Unit,
    onReplay: (String) -> Unit,
) {
    // Entries arrive newest first, so grouping keeps days in order.
    val days = entries.groupBy { entryDay(it.createdAtMillis, nowMillis, zone) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
    ) {
        days.forEach { (day, dayEntries) ->
            item(key = "header-$day") {
                SectionLabel(
                    text = dayLabel(day),
                    modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 10.dp),
                )
            }
            itemsIndexed(dayEntries, key = { _, entry -> entry.id }) { index, entry ->
                HistoryRow(
                    entry = entry,
                    timeLabel = Instant.ofEpochMilli(entry.createdAtMillis).atZone(zone).format(TimeFormat),
                    shape = groupShape(index, dayEntries.size),
                    showDivider = index < dayEntries.lastIndex,
                    onToggleFavourite = { onToggleFavourite(entry) },
                    onReplay = { onReplay(entry.text) },
                )
            }
        }
    }
}

/** Rounds only the outer corners, so consecutive rows read as one card. */
private fun groupShape(index: Int, count: Int): Shape {
    val outer = 22.dp
    val inner = 4.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner,
    )
}

@Composable
private fun dayLabel(day: EntryDay): String = when (day) {
    EntryDay.Today -> stringResource(R.string.history_day_today)
    EntryDay.Yesterday -> stringResource(R.string.history_day_yesterday)
    is EntryDay.Earlier -> day.date.format(DateFormat)
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    timeLabel: String,
    shape: Shape,
    showDivider: Boolean,
    onToggleFavourite: () -> Unit,
    onReplay: () -> Unit,
) {
    val replayDescription = stringResource(R.string.history_replay_description, entry.text)
    val favouriteDescription = stringResource(
        if (entry.isFavourite) R.string.history_unfavourite_description else R.string.history_favourite_description,
        entry.text,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .heightIn(min = 76.dp)
                    .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = entry.text, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(R.string.history_item_meta, timeLabel, entry.confidencePercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onToggleFavourite,
                    modifier = Modifier.semantics { contentDescription = favouriteDescription },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            if (entry.isFavourite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                        ),
                        contentDescription = null,
                        // Favourites wear the tile-red accent, apart from the green of actions.
                        tint = if (entry.isFavourite) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                FilledTonalIconButton(
                    onClick = onReplay,
                    modifier = Modifier.semantics { contentDescription = replayDescription },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_volume_up),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 18.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val loading = stringResource(R.string.history_loading)
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loading })
    }
}

@Composable
private fun EmptyState(filter: HistoryFilter, onPrimaryAction: () -> Unit) {
    val (titleRes, bodyRes, actionRes) = when (filter) {
        HistoryFilter.ALL -> Triple(
            R.string.history_empty_title,
            R.string.history_empty_body,
            R.string.history_empty_action,
        )
        HistoryFilter.TODAY -> Triple(
            R.string.history_empty_today_title,
            R.string.history_empty_today_body,
            R.string.history_empty_show_all,
        )
        HistoryFilter.FAVOURITES -> Triple(
            R.string.history_empty_favourites_title,
            R.string.history_empty_favourites_body,
            R.string.history_empty_show_all,
        )
    }
    MessageState(
        iconRes = if (filter == HistoryFilter.FAVOURITES) R.drawable.ic_favorite_border else R.drawable.ic_history,
        title = stringResource(titleRes),
        body = stringResource(bodyRes),
        action = stringResource(actionRes),
        onAction = onPrimaryAction,
    )
}

/** Centred icon, heading, body and one action: shared by the empty and error states. */
@Composable
private fun MessageState(
    @DrawableRes iconRes: Int,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .padding(horizontal = 36.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
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
                shape = CircleShape,
            ) {
                Text(action)
            }
        }
    }
}
