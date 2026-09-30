package com.silentvoix.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silentvoix.app.R
import com.silentvoix.app.data.history.DayCount
import com.silentvoix.app.data.history.EntryDay
import com.silentvoix.app.data.history.HistoryEntry
import com.silentvoix.app.data.history.HistoryRepository
import com.silentvoix.app.data.history.HistoryStats
import com.silentvoix.app.data.history.entryDay
import com.silentvoix.app.data.history.historyStats
import com.silentvoix.app.recognition.HandPose
import com.silentvoix.app.recognition.SupportedGesture
import com.silentvoix.app.recognition.SupportedGestures
import com.silentvoix.app.ui.common.HandGlyph
import com.silentvoix.app.ui.common.SectionLabel
import com.silentvoix.app.ui.common.rememberHapticTap
import com.silentvoix.app.ui.common.warmBackdrop
import com.silentvoix.app.ui.theme.EyebrowStyle
import com.silentvoix.app.ui.theme.HeroPalette
import com.silentvoix.app.ui.theme.StagePalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TwoColumnMinWidth = 840.dp
private val PagePadding = 20.dp
private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val ShortDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
private const val RecentCount = 3

/**
 * Where the app opens: what you can do (translate, speak), how much you have used it, and the last
 * few phrases. Nothing here touches the camera; it starts only from the Translate action.
 */
@Composable
fun HomeScreen(
    historyRepository: HistoryRepository,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onStartTranslate: () -> Unit,
    onOpenSpeak: () -> Unit,
    onOpenHistory: () -> Unit,
    hapticsEnabled: Boolean,
    onSpeak: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    // Until the store answers, the dashboard shows zeros rather than a spinner: it settles in a
    // few milliseconds and the layout does not jump.
    val entries by historyRepository.entries.collectAsStateWithLifecycle(initialValue = emptyList())
    val tap = rememberHapticTap(hapticsEnabled)
    HomeContent(
        entries = entries,
        nowMillis = System.currentTimeMillis(),
        zone = ZoneId.systemDefault(),
        isDarkTheme = isDarkTheme,
        onToggleTheme = {
            tap()
            onToggleTheme()
        },
        onStartTranslate = {
            tap()
            onStartTranslate()
        },
        onOpenSpeak = {
            tap()
            onOpenSpeak()
        },
        onOpenHistory = {
            tap()
            onOpenHistory()
        },
        onSpeak = {
            tap()
            onSpeak(it)
        },
        contentPadding = contentPadding,
    )
}

/** Stateless dashboard; [nowMillis] and [zone] decide the greeting and "today" so screenshots are stable. */
@Composable
internal fun HomeContent(
    entries: List<HistoryEntry>,
    nowMillis: Long,
    zone: ZoneId,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onStartTranslate: () -> Unit,
    onOpenSpeak: () -> Unit,
    onOpenHistory: () -> Unit,
    onSpeak: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val stats = remember(entries, nowMillis, zone) { historyStats(entries, nowMillis, zone) }
    val now = remember(nowMillis, zone) { Instant.ofEpochMilli(nowMillis).atZone(zone) }

    val hero: @Composable () -> Unit = { HeroCard(onStart = onStartTranslate) }
    val speak: @Composable () -> Unit = { SpeakCard(onClick = onOpenSpeak) }
    val activity: @Composable () -> Unit = { ActivitySection(stats = stats, today = now.toLocalDate()) }
    val recent: @Composable () -> Unit = {
        RecentSection(
            entries = entries.take(RecentCount),
            nowMillis = nowMillis,
            zone = zone,
            onSpeak = onSpeak,
            onOpenHistory = onOpenHistory,
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .warmBackdrop()
            .padding(contentPadding),
    ) {
        val twoColumns = maxWidth >= TwoColumnMinWidth
        val gestures: @Composable () -> Unit = {
            GestureSection(bleed = if (twoColumns) 0.dp else PagePadding, onSpeak = onSpeak)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (twoColumns) 1120.dp else 640.dp)
                    .fillMaxWidth()
                    .padding(start = PagePadding, end = PagePadding, top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Header(
                    hour = now.hour,
                    date = now.toLocalDate(),
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                )
                if (twoColumns) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            hero()
                            speak()
                            gestures()
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            activity()
                            recent()
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        hero()
                        speak()
                    }
                    activity()
                    gestures()
                    recent()
                }
            }
        }
    }
}

/** A greeting for the time of day over today's date, with the light/dark switch beside it. */
@Composable
private fun Header(hour: Int, date: LocalDate, isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val greeting = when (hour) {
        in 5..10 -> R.string.home_greeting_morning
        in 11..13 -> R.string.home_greeting_noon
        in 14..17 -> R.string.home_greeting_afternoon
        else -> R.string.home_greeting_evening
    }
    val weekday = stringArrayResource(R.array.weekday_long)[date.dayOfWeek.value - 1]
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.home_date, weekday, date.dayOfMonth, date.monthValue).uppercase(),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(greeting),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
        }
        // The icon shows where a tap leads: the moon while the app is light, the sun while dark.
        FilledTonalIconButton(
            onClick = onToggleTheme,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(
                    if (isDarkTheme) R.drawable.ic_light_mode else R.drawable.ic_dark_mode,
                ),
                contentDescription = stringResource(
                    if (isDarkTheme) R.string.home_theme_to_light else R.string.home_theme_to_dark,
                ),
            )
        }
    }
}

/**
 * The screen's one loud moment: a fired-clay card that starts a translation. Rings of lantern
 * light and an open palm sit on the right; they are decoration and carry no meaning of their own.
 */
@Composable
private fun HeroCard(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    colors = listOf(HeroPalette.ClayTop, HeroPalette.ClayBottom),
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            )
            .drawBehind {
                // Centred on the palm, which sits in the lower right.
                val centre = Offset(size.width - 82.dp.toPx(), size.height - 84.dp.toPx())
                listOf(62, 96, 132, 170, 210).forEachIndexed { index, radius ->
                    drawCircle(
                        color = HeroPalette.Ring,
                        radius = radius.dp.toPx(),
                        center = centre,
                        alpha = 0.3f - index * 0.055f,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            },
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, top = 22.dp, end = 20.dp, bottom = 22.dp)) {
            Text(
                text = stringResource(R.string.home_hero_eyebrow).uppercase(),
                style = EyebrowStyle,
                color = HeroPalette.OnClayMuted,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.headlineMedium,
                color = HeroPalette.OnClay,
                modifier = Modifier.semantics { heading() },
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_hero_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = HeroPalette.OnClayMuted,
                    )
                    Button(
                        onClick = onStart,
                        modifier = Modifier.heightIn(min = 52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HeroPalette.OnClay,
                            contentColor = HeroPalette.ClayBottom,
                        ),
                        contentPadding = PaddingValues(start = 18.dp, end = 22.dp),
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_videocam), contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.home_hero_action))
                    }
                }
                HandGlyph(
                    pose = HandPose.OpenPalm,
                    bone = HeroPalette.Ring,
                    joint = HeroPalette.OnClay,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(124.dp),
                )
            }
        }
    }
}

/** The other way to be heard: type or pick a phrase. A quieter card than the hero, in honey. */
@Composable
private fun SpeakCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chat),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = stringResource(R.string.home_speak_title), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.home_speak_body), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

/** Three headline numbers and the week's bar chart. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivitySection(stats: HistoryStats, today: LocalDate) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = stringResource(R.string.home_section_activity), modifier = Modifier.padding(start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.home_stat_today),
                value = stats.today,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.home_stat_total),
                value = stats.total,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.home_stat_favourites),
                value = stats.favourites,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_week_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.home_week_total, stats.weekTotal),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (stats.weekTotal == 0) {
                    Text(
                        text = stringResource(R.string.home_week_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    WeekBars(week = stats.week, today = today)
                }
                if (stats.streakDays >= 2 || stats.topPhrase != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (stats.streakDays >= 2) {
                            Pill(
                                text = stringResource(R.string.home_streak, stats.streakDays),
                                container = MaterialTheme.colorScheme.tertiaryContainer,
                                content = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        stats.topPhrase?.let {
                            Pill(
                                text = stringResource(R.string.home_top_phrase, it),
                                container = MaterialTheme.colorScheme.surfaceContainerHighest,
                                content = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, container: Color, content: Color, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.home_stat_description, label, value)
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 14.dp)) {
            Text(text = value.toString(), style = MaterialTheme.typography.displaySmall, maxLines = 1)
            Text(text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

private val BarAreaHeight = 92.dp

/**
 * Phrases per day for the last week, oldest on the left. One series, so one colour and no legend;
 * only the busiest day and today carry a number, and each day reads out its own count to TalkBack.
 */
@Composable
private fun WeekBars(week: List<DayCount>, today: LocalDate) {
    val longNames = stringArrayResource(R.array.weekday_long)
    val shortNames = stringArrayResource(R.array.weekday_short)
    val max = week.maxOf { it.count }.coerceAtLeast(1)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        week.forEach { day ->
            val weekdayIndex = day.date.dayOfWeek.value - 1
            val isToday = day.date == today
            val description = stringResource(R.string.home_week_bar_description, longNames[weekdayIndex], day.count)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (day.count > 0 && (day.count == max || isToday)) day.count.toString() else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.height(BarAreaHeight), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            // An empty day keeps a sliver on the baseline so the week still reads as seven days.
                            .height(if (day.count == 0) 3.dp else maxOf(8.dp, BarAreaHeight * (day.count / max.toFloat())))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (day.count == 0) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                            ),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = shortNames[weekdayIndex],
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * What the model can read, as a row of hand shapes; tapping one speaks its phrase. On a phone the
 * row runs to both screen edges ([bleed] is the page's side padding), so a card cut off at the
 * edge reads as "there is more", not as a clipped box.
 */
@Composable
private fun GestureSection(bleed: Dp, onSpeak: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = stringResource(R.string.home_section_gestures), modifier = Modifier.padding(start = 4.dp))
        Row(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val extra = bleed.roundToPx()
                    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + 2 * extra))
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
                }
                .then(if (bleed == 0.dp) Modifier.clip(MaterialTheme.shapes.large) else Modifier)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = bleed),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SupportedGestures.forEach { gesture -> GestureCard(gesture = gesture, onSpeak = onSpeak) }
        }
        Text(
            text = stringResource(R.string.home_gestures_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun GestureCard(gesture: SupportedGesture, onSpeak: (String) -> Unit) {
    val phrase = stringResource(gesture.phraseRes)
    val name = stringResource(gesture.nameRes)
    val description = stringResource(R.string.home_gesture_description, name, phrase)
    Surface(
        onClick = { onSpeak(phrase) },
        modifier = Modifier
            .width(150.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(122.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(StagePalette.Ink),
            ) {
                HandGlyph(
                    pose = gesture.pose,
                    bone = StagePalette.Guide,
                    joint = StagePalette.OnInk,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 6.dp)) {
                Text(text = phrase, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The newest few phrases, each one tap from being spoken again. */
@Composable
private fun RecentSection(
    entries: List<HistoryEntry>,
    nowMillis: Long,
    zone: ZoneId,
    onSpeak: (String) -> Unit,
    onOpenHistory: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(
                text = stringResource(R.string.home_section_recent),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            if (entries.isNotEmpty()) {
                TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.home_recent_all)) }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_recent_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(18.dp),
                )
            } else {
                Column {
                    entries.forEachIndexed { index, entry ->
                        RecentRow(entry = entry, nowMillis = nowMillis, zone = zone, onSpeak = onSpeak)
                        if (index < entries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 18.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRow(entry: HistoryEntry, nowMillis: Long, zone: ZoneId, onSpeak: (String) -> Unit) {
    val time = Instant.ofEpochMilli(entry.createdAtMillis).atZone(zone)
    val day = when (val entryDay = entryDay(entry.createdAtMillis, nowMillis, zone)) {
        EntryDay.Today -> stringResource(R.string.history_day_today)
        EntryDay.Yesterday -> stringResource(R.string.history_day_yesterday)
        is EntryDay.Earlier -> entryDay.date.format(ShortDateFormat)
    }
    val replayDescription = stringResource(R.string.history_replay_description, entry.text)
    Row(
        modifier = Modifier
            .heightIn(min = 68.dp)
            .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = entry.text, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "$day · ${time.format(TimeFormat)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(
            onClick = { onSpeak(entry.text) },
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
}
