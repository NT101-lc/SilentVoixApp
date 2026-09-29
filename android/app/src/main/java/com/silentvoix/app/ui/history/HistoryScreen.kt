package com.silentvoix.app.ui.history

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silentvoix.app.R
import com.silentvoix.app.data.demo.DemoData
import com.silentvoix.app.data.demo.DemoHistoryEntry
import com.silentvoix.app.ui.common.DemoBadge
import com.silentvoix.app.ui.common.ScreenHeader
import com.silentvoix.app.ui.common.rememberHapticTap
import com.silentvoix.app.ui.theme.EyebrowStyle

private enum class HistoryFilter(@StringRes val labelRes: Int) {
    ALL(R.string.history_filter_all),
    TODAY(R.string.history_filter_today),
    FAVOURITES(R.string.history_filter_favourites),
}

@Composable
fun HistoryScreen(
    onNavigateToTranslate: () -> Unit,
    hapticsEnabled: Boolean,
    onShowMessage: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
    var favouriteIds by rememberSaveable(
        stateSaver = listSaver<Set<Int>, Int>(
            save = { it.toList() },
            restore = { it.toSet() },
        ),
    ) { mutableStateOf(DemoData.defaultFavoriteIds) }

    val tap = rememberHapticTap(hapticsEnabled)
    val ttsUnavailable = stringResource(R.string.message_tts_unavailable)

    val entries = when (filter) {
        HistoryFilter.ALL -> DemoData.history
        HistoryFilter.TODAY -> DemoData.history.filter { it.isToday }
        HistoryFilter.FAVOURITES -> DemoData.history.filter { it.id in favouriteIds }
    }

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
                favouriteIds = favouriteIds,
                onToggleFavourite = { id ->
                    tap()
                    favouriteIds = if (id in favouriteIds) favouriteIds - id else favouriteIds + id
                },
                onReplay = {
                    tap()
                    onShowMessage(ttsUnavailable)
                },
            )
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
    entries: List<DemoHistoryEntry>,
    favouriteIds: Set<Int>,
    onToggleFavourite: (Int) -> Unit,
    onReplay: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
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
            HistoryItem(
                entry = entry,
                isFavourite = entry.id in favouriteIds,
                onToggleFavourite = { onToggleFavourite(entry.id) },
                onReplay = onReplay,
            )
        }
    }
}

@Composable
private fun HistoryItem(
    entry: DemoHistoryEntry,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    onReplay: () -> Unit,
) {
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
                            text = entry.dayLabel.uppercase(),
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
                            text = entry.timeLabel,
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
                    imageVector = ImageVector.vectorResource(
                        if (filter == HistoryFilter.FAVOURITES) {
                            R.drawable.ic_favorite_border
                        } else {
                            R.drawable.ic_history
                        },
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier.heightIn(min = 56.dp),
                shape = CircleShape,
            ) {
                Text(stringResource(actionRes))
            }
        }
    }
}
