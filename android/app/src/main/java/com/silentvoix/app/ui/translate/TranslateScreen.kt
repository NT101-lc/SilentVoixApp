package com.silentvoix.app.ui.translate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silentvoix.app.R
import com.silentvoix.app.data.backend.BackendStatus
import com.silentvoix.app.data.demo.DemoData
import com.silentvoix.app.data.demo.DemoRecognition
import com.silentvoix.app.ui.common.DemoBadge
import com.silentvoix.app.ui.common.backendStatusColor
import com.silentvoix.app.ui.common.backendStatusText
import kotlinx.coroutines.delay

private const val FIRST_RESULT_DELAY_MS = 1_500L
private const val NEXT_RESULT_DELAY_MS = 3_000L
private val TwoPaneMinWidth = 840.dp
private val SinglePaneMaxWidth = 640.dp

@Composable
fun TranslateScreen(
    backendStatus: BackendStatus,
    onRetryBackend: () -> Unit,
    largeResultText: Boolean,
    onShowMessage: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var resultIndex by rememberSaveable { mutableIntStateOf(-1) }

    // Demo only: cycles through canned phrases. No camera frames or model are involved.
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(if (resultIndex < 0) FIRST_RESULT_DELAY_MS else NEXT_RESULT_DELAY_MS)
            resultIndex = (resultIndex + 1) % DemoData.recognitions.size
        }
    }

    val recognition = DemoData.recognitions.getOrNull(resultIndex)
    val ttsUnavailable = stringResource(R.string.message_tts_unavailable)
    val onReplay = { onShowMessage(ttsUnavailable) }
    val onToggle = { isRunning = !isRunning }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        if (maxWidth >= TwoPaneMinWidth) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CameraPreviewPlaceholder(isSimulating = isRunning)
                    StartStopButton(isRunning = isRunning, onToggle = onToggle)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    DemoNotice()
                    ResultCard(recognition, largeResultText, onReplay)
                    StatusCard(backendStatus, onRetryBackend, isRunning)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = SinglePaneMaxWidth)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    DemoNotice()
                    CameraPreviewPlaceholder(isSimulating = isRunning)
                    StartStopButton(isRunning = isRunning, onToggle = onToggle)
                    ResultCard(recognition, largeResultText, onReplay)
                    StatusCard(backendStatus, onRetryBackend, isRunning)
                }
            }
        }
    }
}

@Composable
private fun DemoNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Info, contentDescription = null)
            Text(
                text = stringResource(R.string.translate_demo_notice),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Stand-in for the future CameraX preview. */
@Composable
private fun CameraPreviewPlaceholder(isSimulating: Boolean) {
    val onColor = MaterialTheme.colorScheme.inverseOnSurface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.inverseSurface)
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_videocam),
                contentDescription = null,
                tint = onColor,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.camera_placeholder_title),
                style = MaterialTheme.typography.titleMedium,
                color = onColor,
            )
            Text(
                text = stringResource(R.string.camera_placeholder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = onColor,
                textAlign = TextAlign.Center,
            )
        }
        if (isSimulating) {
            DemoBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                text = stringResource(R.string.camera_placeholder_simulating),
            )
        }
    }
}

@Composable
private fun StartStopButton(isRunning: Boolean, onToggle: () -> Unit) {
    val colors = if (isRunning) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )
    } else {
        ButtonDefaults.buttonColors()
    }
    Button(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        colors = colors,
    ) {
        Icon(
            imageVector = if (isRunning) ImageVector.vectorResource(R.drawable.ic_stop) else Icons.Filled.PlayArrow,
            contentDescription = null,
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = stringResource(if (isRunning) R.string.action_stop_demo else R.string.action_start_demo),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ResultCard(recognition: DemoRecognition?, largeResultText: Boolean, onReplay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.result_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                DemoBadge()
            }
            if (recognition == null) {
                Text(
                    text = stringResource(R.string.result_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                val description = stringResource(R.string.result_content_description, recognition.text)
                Text(
                    text = recognition.text,
                    style = if (largeResultText) {
                        MaterialTheme.typography.displaySmall
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    fontWeight = FontWeight.SemiBold,
                    // Announced by TalkBack whenever a new (sample) result appears.
                    modifier = Modifier.semantics {
                        contentDescription = description
                        liveRegion = LiveRegionMode.Polite
                    },
                )
                Text(
                    text = stringResource(R.string.result_confidence, recognition.confidencePercent),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = onReplay,
                enabled = recognition != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_volume_up), contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.action_replay))
            }
        }
    }
}

@Composable
private fun StatusCard(backendStatus: BackendStatus, onRetryBackend: () -> Unit, isRunning: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            StatusRow(
                label = stringResource(R.string.status_backend_label),
                value = backendStatusText(backendStatus),
                indicatorColor = backendStatusColor(backendStatus),
            ) {
                if (backendStatus is BackendStatus.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp),
                        strokeWidth = 3.dp,
                    )
                } else {
                    IconButton(onClick = onRetryBackend) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.status_backend_retry),
                        )
                    }
                }
            }
            HorizontalDivider()
            StatusRow(
                label = stringResource(R.string.status_recognition_label),
                value = stringResource(
                    if (isRunning) R.string.status_recognition_running else R.string.status_recognition_stopped,
                ),
                indicatorColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    indicatorColor: Color,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decorative: the value text carries the same information.
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(indicatorColor),
        )
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
        trailing?.invoke()
    }
}
