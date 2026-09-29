package com.silentvoix.app.ui.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silentvoix.app.BuildConfig
import com.silentvoix.app.R
import com.silentvoix.app.data.backend.BackendStatus
import com.silentvoix.app.ui.common.ScreenHeader
import com.silentvoix.app.ui.common.SectionLabel
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
    contentPadding: PaddingValues,
) {
    // Feedback follows the current setting: switching haptics off stops buzzing immediately.
    val tap = rememberHapticTap(settings.haptics)

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            ScreenHeader(
                eyebrow = stringResource(R.string.settings_eyebrow),
                title = stringResource(R.string.title_settings),
            )

            SettingsGroup(title = stringResource(R.string.settings_section_appearance)) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme_label),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    ThemeModeSelector(
                        selected = settings.themeMode,
                        onSelect = {
                            tap()
                            onSettingsChange(settings.copy(themeMode = it))
                        },
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
                Text(
                    text = stringResource(R.string.settings_speech_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp),
                )
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
                InfoRow(
                    stringResource(R.string.settings_about_backend_status),
                    backendStatusText(backendStatus),
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

/** Three-way segmented control. Each segment is a radio in the accessibility tree. */
@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(mode) },
                        role = Role.RadioButton,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.settings_theme_system
                            ThemeMode.LIGHT -> R.string.settings_theme_light
                            ThemeMode.DARK -> R.string.settings_theme_dark
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
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
    val label = stringResource(R.string.settings_speech_rate, String.format(VietnameseLocale, "%.2f", rate))
    Column(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        // 0.5× to 2.0× in steps of 0.25.
        Slider(
            value = rate,
            onValueChange = onRateChange,
            valueRange = 0.5f..2f,
            steps = 5,
            modifier = Modifier.semantics { contentDescription = label },
        )
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
