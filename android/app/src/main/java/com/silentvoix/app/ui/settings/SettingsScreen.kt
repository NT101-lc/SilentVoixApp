package com.silentvoix.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.silentvoix.app.BuildConfig
import com.silentvoix.app.R
import com.silentvoix.app.data.backend.BackendStatus
import com.silentvoix.app.ui.common.backendStatusText
import com.silentvoix.app.ui.theme.ThemeMode
import java.util.Locale

private val ContentMaxWidth = 720.dp
private val VietnameseLocale: Locale = Locale.forLanguageTag("vi-VN")

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    backendStatus: BackendStatus,
    contentPadding: PaddingValues,
) {
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
                .padding(bottom = 24.dp),
        ) {
            SectionHeader(R.string.settings_section_appearance)
            Text(
                text = stringResource(R.string.settings_theme_label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ThemeModeOptions(
                selected = settings.themeMode,
                onSelect = { onSettingsChange(settings.copy(themeMode = it)) },
            )
            SwitchRow(
                title = stringResource(R.string.settings_large_text_title),
                summary = stringResource(R.string.settings_large_text_summary),
                checked = settings.largeResultText,
                onCheckedChange = { onSettingsChange(settings.copy(largeResultText = it)) },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(R.string.settings_section_speech)
            Text(
                text = stringResource(R.string.settings_speech_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            SwitchRow(
                title = stringResource(R.string.settings_auto_speak_title),
                summary = stringResource(R.string.settings_auto_speak_summary),
                checked = settings.autoSpeak,
                onCheckedChange = { onSettingsChange(settings.copy(autoSpeak = it)) },
            )
            SpeechRateSlider(
                rate = settings.speechRate,
                onRateChange = { onSettingsChange(settings.copy(speechRate = it)) },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(R.string.settings_section_about)
            InfoRow(stringResource(R.string.settings_about_version), BuildConfig.VERSION_NAME)
            InfoRow(stringResource(R.string.settings_about_backend), BuildConfig.BACKEND_BASE_URL)
            InfoRow(stringResource(R.string.settings_about_backend_status), backendStatusText(backendStatus))
            InfoRow(
                stringResource(R.string.settings_about_recognition),
                stringResource(R.string.settings_about_recognition_value),
            )
        }
    }
}

@Composable
private fun SectionHeader(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun ThemeModeOptions(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(selected = isSelected, onClick = { onSelect(mode) }, role = Role.RadioButton)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.settings_theme_system
                            ThemeMode.LIGHT -> R.string.settings_theme_light
                            ThemeMode.DARK -> R.string.settings_theme_dark
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
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
            .heightIn(min = 64.dp)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
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
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(value) },
        modifier = Modifier.semantics(mergeDescendants = true) {},
    )
}
