package com.silentvoix.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silentvoix.app.BuildConfig
import com.silentvoix.app.R
import com.silentvoix.app.ui.scene.skyWash
import com.silentvoix.app.data.admin.FeedbackKind
import com.silentvoix.app.data.auth.Account
import com.silentvoix.app.ui.admin.RolePill
import com.silentvoix.app.speech.SpeechRate
import com.silentvoix.app.speech.SpeechStatus
import com.silentvoix.app.speech.SpeechUnavailableReason
import com.silentvoix.app.ui.common.ScreenHeader
import com.silentvoix.app.ui.common.SectionLabel
import com.silentvoix.app.ui.common.SegmentedControl
import com.silentvoix.app.ui.common.rememberHapticTap
import com.silentvoix.app.ui.theme.EyebrowStyle
import com.silentvoix.app.ui.theme.ThemeMode
import java.util.Locale

private val ContentMaxWidth = 640.dp
private val VietnameseLocale: Locale = Locale.forLanguageTag("vi-VN")

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    account: Account,
    onSignOut: () -> Unit,
    sendingFeedback: Boolean,
    /** Goes up each time feedback was delivered; the form then clears. */
    feedbackSentCount: Int,
    onSendFeedback: (FeedbackKind, String) -> Unit,
    speechStatus: SpeechStatus,
    onPreviewSpeech: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    // Feedback follows the current setting: switching haptics off stops buzzing immediately.
    val tap = rememberHapticTap(settings.haptics)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .skyWash()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ScreenHeader(title = stringResource(R.string.title_settings))

            SettingsGroup(title = stringResource(R.string.settings_section_account)) {
                AccountRow(
                    account = account,
                    onSignOut = {
                        tap()
                        onSignOut()
                    },
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_section_appearance)) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme_label),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    SegmentedControl(
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        label = { mode ->
                            stringResource(
                                when (mode) {
                                    ThemeMode.SYSTEM -> R.string.settings_theme_system
                                    ThemeMode.LIGHT -> R.string.settings_theme_light
                                    ThemeMode.DARK -> R.string.settings_theme_dark
                                },
                            )
                        },
                        onSelect = {
                            tap()
                            onSettingsChange(settings.copy(themeMode = it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                RowDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_large_text_title),
                    summary = stringResource(R.string.settings_large_text_summary),
                    checked = settings.largeResultText,
                    onCheckedChange = {
                        tap()
                        onSettingsChange(settings.copy(largeResultText = it))
                    },
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_section_speech)) {
                val previewText = stringResource(R.string.settings_speech_preview_text)
                SpeechStatusRow(
                    status = speechStatus,
                    onPreview = {
                        tap()
                        onPreviewSpeech(previewText)
                    },
                )
                RowDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_auto_speak_title),
                    summary = stringResource(R.string.settings_auto_speak_summary),
                    checked = settings.autoSpeak,
                    onCheckedChange = {
                        tap()
                        onSettingsChange(settings.copy(autoSpeak = it))
                    },
                )
                RowDivider()
                SpeechRateSlider(
                    rate = settings.speechRate,
                    onRateChange = { onSettingsChange(settings.copy(speechRate = it)) },
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_section_feedback)) {
                SwitchRow(
                    title = stringResource(R.string.settings_haptics_title),
                    summary = stringResource(R.string.settings_haptics_summary),
                    checked = settings.haptics,
                    onCheckedChange = { enabled ->
                        // Buzz on the way off too, so the change is felt once.
                        tap()
                        onSettingsChange(settings.copy(haptics = enabled))
                    },
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_section_send_feedback)) {
                FeedbackForm(
                    sending = sendingFeedback,
                    sentCount = feedbackSentCount,
                    onSend = { kind, message ->
                        tap()
                        onSendFeedback(kind, message)
                    },
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_section_about)) {
                InfoRow(stringResource(R.string.settings_about_version), BuildConfig.VERSION_NAME)
                RowDivider()
                InfoRow(
                    stringResource(R.string.settings_about_recognition),
                    stringResource(R.string.settings_about_recognition_value),
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = title, modifier = Modifier.padding(start = 4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun SwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SpeechRateSlider(rate: Float, onRateChange: (Float) -> Unit) {
    val value = String.format(VietnameseLocale, "%.2f", rate).trimEnd('0').trimEnd(',')
    val description = stringResource(R.string.settings_speech_rate, value)
    Column(
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_speech_rate_title),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    text = "$value×",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        // 0.5× to 2.0× in steps of 0.25.
        Slider(
            value = rate,
            onValueChange = onRateChange,
            valueRange = SpeechRate.MIN..SpeechRate.MAX,
            steps = 5,
            colors = SliderDefaults.colors(
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.semantics { contentDescription = description },
        )
        Row {
            Text(
                text = stringResource(R.string.settings_speech_rate_slow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.settings_speech_rate_fast),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Whether the device can speak Vietnamese, with a preview button once it can and a shortcut to
 * the system's voice-data installer when the Vietnamese voice is missing.
 */
@Composable
private fun SpeechStatusRow(status: SpeechStatus, onPreview: () -> Unit) {
    val context = LocalContext.current
    val statusText = stringResource(
        when (status) {
            SpeechStatus.Initializing -> R.string.settings_speech_status_initializing
            SpeechStatus.Ready -> R.string.settings_speech_status_ready
            is SpeechStatus.Unavailable -> when (status.reason) {
                SpeechUnavailableReason.NO_ENGINE -> R.string.speech_unavailable_no_engine
                SpeechUnavailableReason.LANGUAGE_MISSING -> R.string.speech_unavailable_language
            }
        },
    )
    val dot = when (status) {
        SpeechStatus.Ready -> MaterialTheme.colorScheme.primary
        SpeechStatus.Initializing -> MaterialTheme.colorScheme.outline
        is SpeechStatus.Unavailable -> MaterialTheme.colorScheme.error
    }
    Column(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (status is SpeechStatus.Unavailable) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        when {
            status == SpeechStatus.Ready -> FilledTonalButton(
                onClick = onPreview,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_volume_up), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_speech_preview))
            }
            status == SpeechStatus.Unavailable(SpeechUnavailableReason.LANGUAGE_MISSING) -> OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                    } catch (_: ActivityNotFoundException) {
                        // No installer on this device; the status text already explains the problem.
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.settings_speech_install_voice))
            }
        }
    }
}

/** Who is signed in, their role, and the way out (asked twice). */
@Composable
private fun AccountRow(account: Account, onSignOut: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (account.isAdmin) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = account.shownName.take(1).uppercase().ifEmpty { "?" },
                style = MaterialTheme.typography.titleLarge,
                color = if (account.isAdmin) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(account.shownName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            account.email?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(Modifier.padding(top = 4.dp)) { RolePill(account.role) }
        }
    }
    RowDivider()
    TextButton(
        onClick = { confirming = true },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 6.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.settings_sign_out), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.weight(1f))
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.sign_out_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onSignOut()
                }) { Text(stringResource(R.string.sign_out_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/** Kind and message; the message stays until the server confirms it arrived. */
@Composable
private fun FeedbackForm(sending: Boolean, sentCount: Int, onSend: (FeedbackKind, String) -> Unit) {
    var kind by rememberSaveable { mutableStateOf(FeedbackKind.IDEA) }
    var message by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(sentCount) { if (sentCount > 0) message = "" }
    Column(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SegmentedControl(
            options = FeedbackKind.entries,
            selected = kind,
            label = {
                stringResource(
                    when (it) {
                        FeedbackKind.WRONG_RESULT -> R.string.feedback_kind_wrong_result
                        FeedbackKind.BUG -> R.string.feedback_kind_bug
                        FeedbackKind.IDEA -> R.string.feedback_kind_idea
                    },
                )
            },
            onSelect = { kind = it },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = message,
            onValueChange = { if (it.length <= FeedbackMaxLength) message = it },
            placeholder = { Text(stringResource(R.string.feedback_hint)) },
            minLines = 3,
            maxLines = 6,
            supportingText = { Text("${message.length}/$FeedbackMaxLength") },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSend(kind, message.trim()) },
            enabled = message.isNotBlank() && !sending,
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = 48.dp),
        ) {
            if (sending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.feedback_send))
        }
    }
}

private const val FeedbackMaxLength = 2000

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
