package com.silentvoix.app.ui.translate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silentvoix.app.R
import com.silentvoix.app.data.backend.BackendStatus
import com.silentvoix.app.recognition.Recognition
import com.silentvoix.app.recognition.RecognitionFailure
import com.silentvoix.app.recognition.SessionStatus
import com.silentvoix.app.recognition.TranslateSession
import com.silentvoix.app.ui.common.ScreenHeader
import com.silentvoix.app.ui.common.backendStatusColor
import com.silentvoix.app.ui.common.backendStatusText
import com.silentvoix.app.ui.common.rememberHapticTap
import com.silentvoix.app.ui.theme.EyebrowStyle
import com.silentvoix.app.ui.theme.StagePalette

private val TwoPaneMinWidth = 840.dp
private val SinglePaneMaxWidth = 560.dp

@Composable
fun TranslateScreen(
    backendStatus: BackendStatus,
    onRetryBackend: () -> Unit,
    largeResultText: Boolean,
    hapticsEnabled: Boolean,
    /** Called once per recognised phrase (auto-speak and history live in the app shell). */
    onNewResult: (Recognition) -> Unit,
    onReplay: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    // Not saved across tab switches on purpose: leaving the screen releases the camera.
    var session by remember { mutableStateOf(TranslateSession()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> session = session.onPermissionResult(granted) }

    // Keyed on the count, not the text, so the same phrase recognised twice is reported twice.
    val currentOnNewResult by rememberUpdatedState(onNewResult)
    LaunchedEffect(session.resultCount) {
        val latest = session.latest
        if (session.resultCount > 0 && latest != null) currentOnNewResult(latest)
    }

    val tap = rememberHapticTap(hapticsEnabled)
    val replayLatest: () -> Unit = {
        tap()
        session.latest?.let { onReplay(it.text) }
    }
    val isActive = session.isRunning || session.status == SessionStatus.AwaitingPermission
    val onToggle = {
        tap()
        if (isActive) {
            session = session.onStopRequested()
        } else {
            val hasPermission = context.checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            session = session.onStartRequested(hasPermission)
            if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    val onOpenSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null)),
        )
    }
    val stage: @Composable () -> Unit = {
        CaptureStage(
            status = session.status,
            onReady = { session = session.onEngineReady() },
            onRecognized = { session = session.onRecognized(it) },
            onFailure = { session = session.onFailure(it) },
            onOpenSettings = onOpenSettings,
        )
    }
    val action: @Composable () -> Unit = {
        PrimaryAction(isActive = isActive, isRetry = session.status is SessionStatus.Failed, onToggle = onToggle)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        if (maxWidth >= TwoPaneMinWidth) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    ScreenHeader(
                        eyebrow = stringResource(R.string.app_name),
                        title = stringResource(R.string.title_translate),
                    )
                    stage()
                    action()
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Spacer(Modifier.height(4.dp))
                    ResultPanel(session, largeResultText, replayLatest)
                    BackendRow(backendStatus, onRetryBackend)
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
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    ScreenHeader(
                        eyebrow = stringResource(R.string.app_name),
                        title = stringResource(R.string.title_translate),
                    )
                    stage()
                    action()
                    ResultPanel(session, largeResultText, replayLatest)
                    BackendRow(backendStatus, onRetryBackend)
                }
            }
        }
    }
}

/**
 * The gesture capture area: an ink panel that shows the live camera while a session runs, and
 * otherwise the framing guide with copy explaining the current state (idle, waiting for camera
 * permission, permission denied, or a failure).
 */
@Composable
private fun CaptureStage(
    status: SessionStatus,
    onReady: () -> Unit,
    onRecognized: (Recognition) -> Unit,
    onFailure: (RecognitionFailure) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val isLive = status == SessionStatus.Listening
    val guideColor by animateColorAsState(
        targetValue = if (isLive) StagePalette.Guide else StagePalette.GuideIdle,
        animationSpec = tween(durationMillis = 450),
        label = "guideColor",
    )

    // One slow breath: enough to read as live, not enough to distract.
    val transition = rememberInfiniteTransition(label = "stage")
    val breath by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(30.dp))
            .background(StagePalette.Ink),
    ) {
        val isRunning = status == SessionStatus.Starting || status == SessionStatus.Listening
        if (isRunning) {
            GestureCamera(
                onReady = onReady,
                onRecognized = onRecognized,
                onFailure = onFailure,
                modifier = Modifier.fillMaxSize(),
            )
        }

        HandFramingGuide(
            color = guideColor,
            alpha = if (isLive) breath else 0.7f,
            modifier = Modifier.fillMaxSize(),
        )

        if (status == SessionStatus.Starting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp),
                color = StagePalette.OnInk,
                strokeWidth = 3.dp,
            )
        }

        StageMessage(
            status = status,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.align(if (isRunning) Alignment.BottomCenter else Alignment.Center),
        )

        if (isLive) {
            LivePill(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                alpha = breath,
            )
        }
    }
}

/** The stage copy for [status]. Announced politely so TalkBack users hear state changes. */
@Composable
private fun StageMessage(
    status: SessionStatus,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, body) = when (status) {
        SessionStatus.Idle -> R.string.stage_idle_title to R.string.stage_idle_body
        SessionStatus.AwaitingPermission ->
            R.string.stage_permission_title to R.string.stage_permission_body
        SessionStatus.PermissionDenied ->
            R.string.stage_permission_denied_title to R.string.stage_permission_denied_body
        SessionStatus.Starting -> R.string.stage_starting to null
        SessionStatus.Listening -> R.string.stage_active_title to null
        is SessionStatus.Failed -> when (status.failure) {
            RecognitionFailure.CAMERA_UNAVAILABLE ->
                R.string.stage_error_camera_title to R.string.stage_error_camera_body
            RecognitionFailure.MODEL_UNAVAILABLE ->
                R.string.stage_error_model_title to R.string.stage_error_model_body
            RecognitionFailure.INFERENCE ->
                R.string.stage_error_inference_title to R.string.stage_error_inference_body
        }
    }
    val showIcon = status == SessionStatus.Idle || status == SessionStatus.PermissionDenied ||
        status is SessionStatus.Failed

    Column(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Crossfade(targetState = Triple(title, body, showIcon), label = "stageCopy") { (t, b, icon) ->
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (icon) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_videocam),
                        contentDescription = null,
                        tint = StagePalette.OnInkMuted,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Text(
                    text = stringResource(t),
                    style = MaterialTheme.typography.titleMedium,
                    color = StagePalette.OnInk,
                    textAlign = TextAlign.Center,
                )
                if (b != null) {
                    Text(
                        text = stringResource(b),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StagePalette.OnInkMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (status == SessionStatus.PermissionDenied) {
            OutlinedButton(
                onClick = onOpenSettings,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StagePalette.OnInk),
            ) {
                Text(stringResource(R.string.action_open_settings))
            }
        }
    }
}

/** Four corner brackets marking where a hand should sit. Decorative; the copy carries the meaning. */
@Composable
private fun HandFramingGuide(color: Color, alpha: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val insetX = size.width * 0.14f
        val insetY = size.height * 0.13f
        val left = insetX
        val top = insetY
        val right = size.width - insetX
        val bottom = size.height - insetY
        val arm = minOf(right - left, bottom - top) * 0.22f
        val radius = 22.dp.toPx()
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)

        fun corner(x: Float, y: Float, dx: Float, dy: Float): Path = Path().apply {
            moveTo(x + dx * arm, y)
            lineTo(x + dx * radius, y)
            quadraticBezierTo(x, y, x, y + dy * radius)
            lineTo(x, y + dy * arm)
        }

        listOf(
            corner(left, top, 1f, 1f),
            corner(right, top, -1f, 1f),
            corner(left, bottom, 1f, -1f),
            corner(right, bottom, -1f, -1f),
        ).forEach { path ->
            drawPath(path = path, color = color, alpha = alpha, style = stroke)
        }
    }
}

@Composable
private fun LivePill(modifier: Modifier = Modifier, alpha: Float) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = StagePalette.Guide.copy(alpha = 0.16f),
        contentColor = StagePalette.Guide,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(StagePalette.Guide.copy(alpha = alpha)),
            )
            Text(text = stringResource(R.string.stage_live), style = EyebrowStyle)
        }
    }
}

@Composable
private fun PrimaryAction(isActive: Boolean, isRetry: Boolean, onToggle: () -> Unit) {
    val colors = if (isActive) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
    Button(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp),
        shape = CircleShape,
        colors = colors,
    ) {
        Icon(
            imageVector = if (isActive) {
                ImageVector.vectorResource(R.drawable.ic_stop)
            } else {
                Icons.Filled.PlayArrow
            },
            contentDescription = null,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(
                when {
                    isActive -> R.string.action_stop
                    isRetry -> R.string.action_retry
                    else -> R.string.action_start
                },
            ),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ResultPanel(
    session: TranslateSession,
    largeResultText: Boolean,
    onReplay: () -> Unit,
) {
    val recognition = session.latest
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(R.string.result_title),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )

            when {
                recognition != null -> ResultContent(recognition, largeResultText, onReplay)
                session.isRunning -> WaitingContent()
                session.hasRunOnce -> PlaceholderContent(
                    title = stringResource(R.string.result_none_title),
                    body = stringResource(R.string.result_none_body),
                )
                else -> PlaceholderContent(
                    title = stringResource(R.string.result_idle_title),
                    body = stringResource(R.string.result_idle_body),
                )
            }

            // The stock model knows a few common gestures, not VSL; say so next to every result.
            Text(
                text = stringResource(R.string.result_model_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultContent(
    recognition: Recognition,
    largeResultText: Boolean,
    onReplay: () -> Unit,
) {
    val description = stringResource(R.string.result_content_description, recognition.text)
    Text(
        text = recognition.text,
        style = if (largeResultText) {
            MaterialTheme.typography.displaySmall
        } else {
            MaterialTheme.typography.headlineSmall
        },
        color = MaterialTheme.colorScheme.onSurface,
        // Announced by TalkBack whenever a new result appears.
        modifier = Modifier.semantics {
            contentDescription = description
            liveRegion = LiveRegionMode.Polite
        },
    )
    ConfidenceBar(recognition.confidencePercent)
    Button(
        onClick = onReplay,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Icon(ImageVector.vectorResource(R.drawable.ic_volume_up), contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.action_replay))
    }
}

/** Confidence as a bar plus the number; the text alone is enough for screen readers. */
@Composable
private fun ConfidenceBar(percent: Int) {
    val label = stringResource(R.string.result_confidence, percent)
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun WaitingContent() {
    Row(
        modifier = Modifier
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.result_waiting),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaceholderContent(title: String, body: String) {
    Column(
        modifier = Modifier
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Backend reachability. The only part of this screen that is not local sample data. */
@Composable
private fun BackendRow(backendStatus: BackendStatus, onRetryBackend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decorative: the value text carries the same information.
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(backendStatusColor(backendStatus)),
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                text = stringResource(R.string.status_backend_label),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = backendStatusText(backendStatus),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (backendStatus is BackendStatus.Checking) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(12.dp)
                    .size(20.dp),
                strokeWidth = 2.5.dp,
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
}
