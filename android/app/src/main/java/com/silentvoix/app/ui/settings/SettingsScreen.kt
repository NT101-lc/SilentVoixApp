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
import androidx.compose.runtime.Composable
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
import com.silentvoix.app.data.backend.BackendStatus
import com.silentvoix.app.speech.SpeechRate
import com.silentvoix.app.speech.SpeechStatus
import com.silentvoix.app.speech.SpeechUnavailableReason
import com.silentvoix.app.ui.common.ScreenHeader
import com.silentvoix.app.ui.common.SectionLabel
import com.silentvoix.app.ui.common.SegmentedControl
import com.silentvoix.app.ui.common.backendStatusColor
import com.silentvoix.app.ui.common.backendStatusText
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
    backendStatus: BackendStatus,
    onRetryBackend: () -> Unit,
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

            SettingsGroup(title = stringResource(R.string.settings_section_about)) {
                InfoRow(stringResource(R.string.settings_about_version), BuildConfig.VERSION_NAME)
                RowDivider()
                InfoRow(stringResource(R.string.settings_about_backend), BuildConfig.BACKEND_BASE_URL)
                RowDivider()
                BackendStatusRow(
                    status = backendStatus,
                    onRetry = {
                        tap()
                        onRetryBackend()
                    },
                )
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

/** Server reachability with a manual re-check; the dot is decorative, the text says it all. */
@Composable
private fun BackendStatusRow(status: BackendStatus, onRetry: () -> Unit) {
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
