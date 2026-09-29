package com.silentvoix.app.ui.history

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silentvoix.app.R
import com.silentvoix.app.data.history.EntryDay
import com.silentvoix.app.data.history.HistoryEntry
import com.silentvoix.app.data.history.HistoryRepository
import com.silentvoix.app.data.history.HistoryUiState
import com.silentvoix.app.data.history.asHistoryUiState
import com.silentvoix.app.data.history.entryDay
import com.silentvoix.app.ui.common.ScreenHeader
import com.silentvoix.app.ui.common.rememberHapticTap
import com.silentvoix.app.ui.theme.EyebrowStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class HistoryFilter(@StringRes val labelRes: Int) {
    ALL(R.string.history_filter_all),
    TODAY(R.string.history_filter_today),
    FAVOURITES(R.string.history_filter_favourites),
}

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun HistoryScreen(
    historyRepository: HistoryRepository,
    onToggleFavourite: (HistoryEntry) -> Unit,
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
    val nowMillis = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenHeader(
                eyebrow = stringResource(R.string.history_eyebrow),
                title = stringResource(R.string.title_history),
            )
            FilterRow(
                selected = filter,
                onSelect = {
                    tap()
                    filter = it
                },
            )
        }

        when (val current = state) {
            HistoryUiState.Loading -> LoadingState()
            HistoryUiState.Error -> ErrorState(
                onRetry = {
                    tap()
                    loadAttempt++
                },
            )
            is HistoryUiState.Loaded -> {
                val entries = when (filter) {
                    HistoryFilter.ALL -> current.entries
                    HistoryFilter.TODAY -> current.entries.filter {
                        entryDay(it.createdAtMillis, nowMillis, zone) == EntryDay.Today
                    }
                    HistoryFilter.FAVOURITES -> current.entries.filter { it.isFavourite }
                }
                if (entries.isEmpty()) {
                    EmptyState(
                        filter = filter,
                        onPrimaryAction = {
                            tap()
                            if (filter == HistoryFilter.ALL) onNavigateToTranslate() else filter = HistoryFilter.ALL
                        },
                    )
                } else {
                    HistoryList(
                        entries = entries,
                        nowMillis = nowMillis,
                        zone = zone,
                        onToggleFavourite = { entry ->
                            tap()
                            onToggleFavourite(entry)
                        },
                        onReplay = { text ->
                            tap()
                            onSpeak(text)
                        },
                    )
                }
            }
        }
    }
}

/** Segmented-looking filter row; each chip is a plain toggle with a 48 dp touch target. */
@Composable
private fun FilterRow(selected: HistoryFilter, onSelect: (HistoryFilter) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HistoryFilter.entries.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(option.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    entries: List<HistoryEntry>,
    nowMillis: Long,
    zone: ZoneId,
    onToggleFavourite: (HistoryEntry) -> Unit,
    onReplay: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.history_local_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(entries, key = { it.id }) { entry ->
            HistoryItem(
                entry = entry,
                dayLabel = dayLabel(entryDay(entry.createdAtMillis, nowMillis, zone)),
                timeLabel = Instant.ofEpochMilli(entry.createdAtMillis).atZone(zone).format(TimeFormat),
                onToggleFavourite = { onToggleFavourite(entry) },
                onReplay = { onReplay(entry.text) },
            )
        }
    }
}

@Composable
private fun dayLabel(day: EntryDay): String = when (day) {
    EntryDay.Today -> stringResource(R.string.history_day_today)
    EntryDay.Yesterday -> stringResource(R.string.history_day_yesterday)
    is EntryDay.Earlier -> day.date.format(DateFormat)
}

@Composable
private fun HistoryItem(
    entry: HistoryEntry,
    dayLabel: String,
    timeLabel: String,
    onToggleFavourite: () -> Unit,
    onReplay: () -> Unit,
) {
    val isFavourite = entry.isFavourite
    val replayDescription = stringResource(R.string.history_replay_description, entry.text)
    val favouriteDescription = stringResource(
        if (isFavourite) R.string.history_unfavourite_description else R.string.history_favourite_description,
        entry.text,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp, bottom = 4.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = dayLabel.uppercase(),
                            style = EyebrowStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box(
                            Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                        Text(
                            text = timeLabel,
                            style = EyebrowStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(text = entry.text, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(R.string.history_item_confidence, entry.confidencePercent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onToggleFavourite,
                    modifier = Modifier.semantics { contentDescription = favouriteDescription },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            if (isFavourite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                        ),
                        contentDescription = null,
                        tint = if (isFavourite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onReplay,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = replayDescription },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_volume_up),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_replay))
                }
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
private fun ErrorState(onRetry: () -> Unit) {
    MessageState(
        iconRes = R.drawable.ic_history,
        title = stringResource(R.string.history_error_title),
        body = stringResource(R.string.history_error_body),
        action = stringResource(R.string.history_error_retry),
        onAction = onRetry,
    )
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
