package com.silentvoix.app.ui.speak

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silentvoix.app.R
import com.silentvoix.app.data.phrases.PhraseRepository
import com.silentvoix.app.ui.common.FullscreenCaptionDialog
import com.silentvoix.app.ui.common.ScreenHeader
import com.silentvoix.app.ui.common.rememberHapticTap

/**
 * Groups of ready-made phrases; [MINE] is whatever the user has saved. In display order:
 * [EMERGENCY] comes before [MINE] so that on a narrow phone it is never the pill scrolled out of view.
 */
internal enum class PhraseCategory(@StringRes val labelRes: Int, @ArrayRes val phrasesRes: Int?) {
    GREETING(R.string.speak_category_greeting, R.array.phrases_greeting),
    NEEDS(R.string.speak_category_needs, R.array.phrases_needs),
    EMERGENCY(R.string.speak_category_emergency, R.array.phrases_emergency),
    MINE(R.string.speak_category_mine, null),
}

/**
 * The other half of a conversation: the user types or picks a phrase, the phone says it aloud and
 * shows it full-screen for the other person to read. Needs no camera and no model.
 */
@Composable
fun SpeakScreen(
    phraseRepository: PhraseRepository,
    onSavePhrase: (String) -> Unit,
    onRemovePhrase: (String) -> Unit,
    hapticsEnabled: Boolean,
    onSpeak: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val saved by phraseRepository.phrases.collectAsStateWithLifecycle(initialValue = emptyList())
    var draft by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(PhraseCategory.GREETING) }
    var showing by rememberSaveable { mutableStateOf<String?>(null) }
    val tap = rememberHapticTap(hapticsEnabled)
    val keyboard = LocalSoftwareKeyboardController.current

    showing?.let { text ->
        FullscreenCaptionDialog(text = text, onDismiss = { showing = null }, onReplay = { onSpeak(text) })
    }

    SpeakContent(
        draft = draft,
        onDraftChange = { draft = it.take(PhraseRepository.MAX_LENGTH) },
        category = category,
        onCategoryChange = {
            tap()
            category = it
        },
        saved = saved,
        onSpeak = { text ->
            val phrase = PhraseRepository.normalize(text)
            if (phrase.isNotEmpty()) {
                tap()
                keyboard?.hide()
                onSpeak(phrase)
                showing = phrase
            }
        },
        onSave = {
            tap()
            onSavePhrase(draft)
            draft = ""
            category = PhraseCategory.MINE
        },
        onRemove = {
            tap()
            onRemovePhrase(it)
        },
        contentPadding = contentPadding,
    )
}

@Composable
internal fun SpeakContent(
    draft: String,
    onDraftChange: (String) -> Unit,
    category: PhraseCategory,
    onCategoryChange: (PhraseCategory) -> Unit,
    saved: List<String>,
    onSpeak: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val phrases = category.phrasesRes?.let { stringArrayResource(it).toList() } ?: saved
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        // One column of wide tiles on a phone (Vietnamese sentences need the width), more on a tablet.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScreenHeader(
                    title = stringResource(R.string.title_speak),
                    supporting = stringResource(R.string.speak_supporting),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Composer(draft = draft, onDraftChange = onDraftChange, onSpeak = { onSpeak(draft) }, onSave = onSave)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryPills(
                    selected = category,
                    onSelect = onCategoryChange,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
            }
            if (phrases.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyMine() }
            }
            items(phrases, key = { "${category.name}:$it" }) { phrase ->
                PhraseTile(
                    phrase = phrase,
                    urgent = category == PhraseCategory.EMERGENCY,
                    onSpeak = { onSpeak(phrase) },
                    onRemove = if (category == PhraseCategory.MINE) ({ onRemove(phrase) }) else null,
                )
            }
        }
    }
}

/** The text box with its two actions: say it now, or keep it for next time. */
@Composable
private fun Composer(draft: String, onDraftChange: (String) -> Unit, onSpeak: () -> Unit, onSave: () -> Unit) {
    val hasText = draft.isNotBlank()
    val label = stringResource(R.string.speak_input_label)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(bottom = 10.dp)) {
            TextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label },
                textStyle = MaterialTheme.typography.titleLarge,
                placeholder = {
                    Text(
                        text = stringResource(R.string.speak_input_placeholder),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                minLines = 2,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSave, enabled = hasText, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.speak_action_save))
                }
                Spacer(Modifier.weight(1f))
                if (hasText) {
                    IconButton(onClick = { onDraftChange("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.speak_action_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Button(
                    onClick = onSpeak,
                    enabled = hasText,
                    modifier = Modifier.heightIn(min = 52.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(start = 18.dp, end = 22.dp),
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_volume_up), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.speak_action_speak))
                }
            }
        }
    }
}

/** Category choice as a scrolling row of pills; each is a radio button for TalkBack. */
@Composable
private fun CategoryPills(selected: PhraseCategory, onSelect: (PhraseCategory) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PhraseCategory.entries.forEach { category ->
            val isSelected = category == selected
            val container by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "pill",
            )
            // The emergency group is marked in the error colour so it is found at a glance.
            val content = when {
                isSelected -> MaterialTheme.colorScheme.inverseOnSurface
                category == PhraseCategory.EMERGENCY -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(container)
                    .selectable(selected = isSelected, onClick = { onSelect(category) }, role = Role.RadioButton)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(category.labelRes), style = MaterialTheme.typography.labelLarge, color = content)
            }
        }
    }
}

/** One phrase: the whole tile speaks it. Saved phrases also carry a remove button. */
@Composable
private fun PhraseTile(phrase: String, urgent: Boolean, onSpeak: () -> Unit, onRemove: (() -> Unit)?) {
    val speakDescription = stringResource(R.string.speak_phrase_description, phrase)
    Surface(
        onClick = onSpeak,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = speakDescription },
        shape = RoundedCornerShape(22.dp),
        color = if (urgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (urgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 68.dp)
                .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = phrase, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.speak_remove_description, phrase),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_volume_up),
                    contentDescription = null,
                    tint = if (urgent) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyMine() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.speak_mine_empty_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.speak_mine_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
    }
}
