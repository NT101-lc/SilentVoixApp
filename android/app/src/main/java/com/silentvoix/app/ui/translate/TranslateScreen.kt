package com.silentvoix.app.ui.translate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.silentvoix.app.R
import com.silentvoix.app.recognition.Recognition
import com.silentvoix.app.recognition.RecognitionFailure
import com.silentvoix.app.recognition.SessionStatus
import com.silentvoix.app.recognition.TranslateSession
import com.silentvoix.app.ui.common.rememberHapticTap
import com.silentvoix.app.ui.theme.EyebrowStyle
import com.silentvoix.app.ui.theme.StagePalette

private val TwoPaneMinWidth = 840.dp
private val ControlSize = 76.dp

/**
 * The main screen: live camera with captions over it, and a camera-style control bar. Owns the
 * recognition session, the camera permission and the camera itself; drawing is in [TranslateContent].
 */
@Composable
fun TranslateScreen(
    largeResultText: Boolean,
    autoSpeak: Boolean,
    hapticsEnabled: Boolean,
    onToggleAutoSpeak: () -> Unit,
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
    TranslateContent(
        session = session,
        largeResultText = largeResultText,
        autoSpeak = autoSpeak,
        onToggleSession = {
            tap()
            if (session.isRunning || session.status == SessionStatus.AwaitingPermission) {
                session = session.onStopRequested()
            } else {
                val hasPermission = context.checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                session = session.onStartRequested(hasPermission)
                if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onReplay = {
            tap()
            session.latest?.let { onReplay(it.text) }
        },
        onToggleAutoSpeak = {
            tap()
            onToggleAutoSpeak()
        },
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null)),
            )
        },
        contentPadding = contentPadding,
        camera = { modifier ->
            GestureCamera(
                onReady = { session = session.onEngineReady() },
                onRecognized = { session = session.onRecognized(it) },
                onFailure = { session = session.onFailure(it) },
                modifier = modifier,
            )
        },
    )
}

/**
 * Stateless Translate screen. [camera] is composed only while the session runs; screenshot
 * tooling passes a stand-in so every state can be rendered without CameraX.
 */
@Composable
internal fun TranslateContent(
    session: TranslateSession,
    largeResultText: Boolean,
    autoSpeak: Boolean,
    onToggleSession: () -> Unit,
    onReplay: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
    camera: @Composable (Modifier) -> Unit,
) {
    var showFullscreen by rememberSaveable { mutableStateOf(false) }
    val latest = session.latest
    if (showFullscreen && latest != null) {
        FullscreenCaptionDialog(text = latest.text, onDismiss = { showFullscreen = false })
    }

    val stage: @Composable (Modifier) -> Unit = { modifier ->
        CaptureStage(
            session = session,
            largeResultText = largeResultText,
            onExpand = { showFullscreen = true },
            onOpenSettings = onOpenSettings,
            camera = camera,
            modifier = modifier,
        )
    }
    val controls: @Composable () -> Unit = {
        ControlBar(
            session = session,
            autoSpeak = autoSpeak,
            onToggleSession = onToggleSession,
            onReplay = onReplay,
            onToggleAutoSpeak = onToggleAutoSpeak,
        )
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
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TopBar(session.status)
                    stage(Modifier.weight(1f).fillMaxWidth())
                    controls()
                    ModelNote()
                }
                TranscriptPanel(
                    transcript = session.transcript,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .align(Alignment.TopCenter)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TopBar(session.status)
                stage(Modifier.weight(1f).fillMaxWidth().heightIn(min = 280.dp))
                controls()
                ModelNote()
            }
        }
    }
}

/** Wordmark and a status chip that always says, in words, what the camera is doing. */
@Composable
private fun TopBar(status: SessionStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val primary = MaterialTheme.colorScheme.primary
        Text(
            text = buildAnnotatedString {
                append("Silent")
                withStyle(SpanStyle(color = primary)) { append("Voix") }
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        StatusChip(status)
    }
}

private enum class ChipTone { Neutral, Live, Error }

@Composable
private fun StatusChip(status: SessionStatus) {
    val (label, tone) = when (status) {
        SessionStatus.Idle -> R.string.translate_status_ready to ChipTone.Neutral
        SessionStatus.Starting -> R.string.translate_status_starting to ChipTone.Neutral
        SessionStatus.Listening -> R.string.translate_status_live to ChipTone.Live
        SessionStatus.AwaitingPermission -> R.string.translate_status_permission to ChipTone.Neutral
        SessionStatus.PermissionDenied -> R.string.translate_status_permission_denied to ChipTone.Error
        is SessionStatus.Failed -> R.string.translate_status_error to ChipTone.Error
    }
    val dot = when (tone) {
        ChipTone.Neutral -> MaterialTheme.colorScheme.outline
        ChipTone.Live -> MaterialTheme.colorScheme.primary
        ChipTone.Error -> MaterialTheme.colorScheme.error
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
            Text(text = stringResource(label), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * The capture area: an ink panel showing the camera while a session runs, with the framing guide,
 * and live captions docked to the bottom like subtitles, so the person reading never has to look
 * away from the signer. Permission and failure states replace the captions with an explanation.
 */
@Composable
private fun CaptureStage(
    session: TranslateSession,
    largeResultText: Boolean,
    onExpand: () -> Unit,
    onOpenSettings: () -> Unit,
    camera: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = session.status
    val isLive = status == SessionStatus.Listening
    val latest = session.latest
    val blocking = status == SessionStatus.AwaitingPermission || status == SessionStatus.PermissionDenied ||
        status is SessionStatus.Failed

    val guideColor by animateColorAsState(
        targetValue = if (isLive) StagePalette.Guide else StagePalette.GuideIdle,
        animationSpec = tween(durationMillis = 450),
        label = "guideColor",
    )
    // One slow breath: enough to read as live, not enough to distract.
    val breath by rememberInfiniteTransition(label = "stage").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_800), RepeatMode.Reverse),
        label = "breath",
    )
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(StagePalette.Ink)
            .border(1.dp, StagePalette.InkEdge, shape),
    ) {
        if (session.isRunning) camera(Modifier.fillMaxSize())

        HandFramingGuide(
            color = guideColor,
            alpha = when {
                blocking -> 0.35f
                isLive -> breath
                else -> 0.7f
            },
            // Leave the lower part to the captions once there is something to read.
            bottomInsetFraction = if (latest != null || isLive) 0.36f else 0.12f,
            modifier = Modifier.fillMaxSize(),
        )

        if (blocking || (latest == null && !isLive)) {
            StageMessage(
                status = status,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            CaptionPanel(
                session = session,
                largeResultText = largeResultText,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        }

        if (latest != null && !blocking) {
            IconButton(
                onClick = onExpand,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = StagePalette.OnInk.copy(alpha = 0.12f),
                    contentColor = StagePalette.OnInk,
                ),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_fullscreen),
                    contentDescription = stringResource(R.string.caption_expand),
                )
            }
        }
    }
}

/**
 * Subtitles over the camera: the latest phrase large, the two before it smaller and faded above
 * it, and the model's confidence as a thin meter. Announced politely by TalkBack.
 */
@Composable
private fun CaptionPanel(
    session: TranslateSession,
    largeResultText: Boolean,
    modifier: Modifier = Modifier,
) {
    val latest = session.latest
    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color.Transparent, StagePalette.CaptionScrim)))
            .padding(start = 22.dp, end = 22.dp, top = 40.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (latest == null) {
            // Listening, nothing recognised yet.
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = StagePalette.Guide,
                )
                Text(
                    text = stringResource(R.string.stage_active_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = StagePalette.OnInk,
                )
            }
            return@Column
        }

        session.transcript.dropLast(1).takeLast(2).forEachIndexed { index, earlier ->
            Text(
                text = earlier.text,
                style = MaterialTheme.typography.titleMedium,
                color = StagePalette.OnInkMuted.copy(alpha = if (index == 0) 0.55f else 0.85f),
            )
        }

        val description = stringResource(R.string.result_content_description, latest.text)
        AnimatedContent(
            targetState = session.resultCount to latest.text,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 }) togetherWith fadeOut(tween(120))
            },
            label = "caption",
        ) { (_, text) ->
            Text(
                text = text,
                style = if (largeResultText) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                color = StagePalette.OnInk,
                modifier = Modifier.semantics {
                    contentDescription = description
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }
        ConfidenceMeter(latest.confidencePercent)
    }
}

@Composable
private fun ConfidenceMeter(percent: Int) {
    val label = stringResource(R.string.result_confidence, percent)
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(72.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(StagePalette.OnInk.copy(alpha = 0.18f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(StagePalette.Guide),
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = StagePalette.OnInkMuted)
    }
}

/** Copy for idle, starting, permission and failure states. Announced so TalkBack hears changes. */
@Composable
private fun StageMessage(
    status: SessionStatus,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, body) = when (status) {
        SessionStatus.Idle, SessionStatus.Listening ->
            R.string.stage_idle_title to R.string.stage_idle_body
        SessionStatus.AwaitingPermission ->
            R.string.stage_permission_title to R.string.stage_permission_body
        SessionStatus.PermissionDenied ->
            R.string.stage_permission_denied_title to R.string.stage_permission_denied_body
        SessionStatus.Starting -> R.string.stage_starting to null
        is SessionStatus.Failed -> when (status.failure) {
            RecognitionFailure.CAMERA_UNAVAILABLE ->
                R.string.stage_error_camera_title to R.string.stage_error_camera_body
            RecognitionFailure.MODEL_UNAVAILABLE ->
                R.string.stage_error_model_title to R.string.stage_error_model_body
            RecognitionFailure.INFERENCE ->
                R.string.stage_error_inference_title to R.string.stage_error_inference_body
        }
    }
    val isError = status == SessionStatus.PermissionDenied || status is SessionStatus.Failed

    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (status == SessionStatus.Starting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = StagePalette.Guide,
                    strokeWidth = 3.dp,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isError) StagePalette.ErrorSoft else StagePalette.OnInk.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_videocam),
                        contentDescription = null,
                        tint = if (isError) StagePalette.Error else StagePalette.OnInkMuted,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                color = StagePalette.OnInk,
                textAlign = TextAlign.Center,
            )
            if (body != null) {
                Text(
                    text = stringResource(body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StagePalette.OnInkMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (status == SessionStatus.PermissionDenied) {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.heightIn(min = 48.dp),
                border = BorderStroke(1.dp, StagePalette.OnInk.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StagePalette.OnInk),
            ) {
                Text(stringResource(R.string.action_open_settings))
            }
        }
    }
}

/** Four corner brackets marking where a hand should sit. Decorative; the copy carries the meaning. */
@Composable
private fun HandFramingGuide(
    color: Color,
    alpha: Float,
    bottomInsetFraction: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val insetX = size.width * 0.16f
        val left = insetX
        val top = size.height * 0.12f
        val right = size.width - insetX
        val bottom = size.height * (1f - bottomInsetFraction)
        val arm = minOf(right - left, bottom - top) * 0.2f
        val radius = 20.dp.toPx()
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)

        fun corner(x: Float, y: Float, dx: Float, dy: Float): Path = Path().apply {
            moveTo(x + dx * arm, y)
            lineTo(x + dx * radius, y)
            quadraticTo(x, y, x, y + dy * radius)
            lineTo(x, y + dy * arm)
        }

        listOf(
            corner(left, top, 1f, 1f),
            corner(right, top, -1f, 1f),
            corner(left, bottom, 1f, -1f),
            corner(right, bottom, -1f, -1f),
        ).forEach { drawPath(path = it, color = color, alpha = alpha, style = stroke) }
    }
}

/**
 * Camera-style controls: replay on the left, the big start/stop in the middle, auto-speak on the
 * right. Every control carries a visible text label: the app's users rely on reading, not sound.
 */
@Composable
private fun ControlBar(
    session: TranslateSession,
    autoSpeak: Boolean,
    onToggleSession: () -> Unit,
    onReplay: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
) {
    val isActive = session.isRunning || session.status == SessionStatus.AwaitingPermission
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        SecondaryControl(
            icon = ImageVector.vectorResource(R.drawable.ic_volume_up),
            label = stringResource(R.string.action_replay),
            enabled = session.latest != null,
            selected = false,
            role = Role.Button,
            onClick = onReplay,
        )
        PrimaryControl(
            isActive = isActive,
            label = stringResource(
                when {
                    isActive -> R.string.action_stop
                    session.status is SessionStatus.Failed -> R.string.action_retry
                    else -> R.string.action_start
                },
            ),
            onClick = onToggleSession,
        )
        SecondaryControl(
            icon = ImageVector.vectorResource(if (autoSpeak) R.drawable.ic_volume_up else R.drawable.ic_volume_off),
            label = stringResource(if (autoSpeak) R.string.control_auto_speak_on else R.string.control_auto_speak_off),
            enabled = true,
            selected = autoSpeak,
            role = Role.Switch,
            onClick = onToggleAutoSpeak,
        )
    }
}

@Composable
private fun PrimaryControl(isActive: Boolean, label: String, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        label = "primaryControl",
    )
    val content = if (isActive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(ControlSize)
                .border(3.dp, container.copy(alpha = 0.28f), CircleShape)
                .padding(6.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isActive) ImageVector.vectorResource(R.drawable.ic_stop) else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun SecondaryControl(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    selected: Boolean,
    role: Role,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val interaction = if (role == Role.Switch) {
        Modifier.toggleable(value = selected, enabled = enabled, role = role, onValueChange = { onClick() })
    } else {
        Modifier.clickable(enabled = enabled, role = role, onClick = onClick)
    }
    val alpha = if (enabled) 1f else 0.38f
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(interaction)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(ControlSize), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(container.copy(alpha = if (enabled) 1f else 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = content.copy(alpha = alpha))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
    }
}

/** One honest line about what the model can do; the full explanation lives in Settings. */
@Composable
private fun ModelNote() {
    Text(
        text = stringResource(R.string.model_note_short),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Wide screens: the session so far, newest first, as a readable transcript. */
@Composable
private fun TranscriptPanel(transcript: List<Recognition>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(R.string.transcript_title).uppercase(),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            if (transcript.isEmpty()) {
                Text(
                    text = stringResource(R.string.transcript_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            transcript.asReversed().forEachIndexed { index, item ->
                Text(
                    text = item.text,
                    style = if (index == 0) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                    color = if (index == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Full-screen phrase to show the other person. Tap anywhere to close. */
@Composable
private fun FullscreenCaptionDialog(text: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        FullscreenCaption(text = text, onDismiss = onDismiss)
    }
}

@Composable
internal fun FullscreenCaption(text: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.caption_fullscreen_eyebrow).uppercase(),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 14.dp),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.caption_close))
            }
            Text(
                text = text,
                style = if (text.length <= 18) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(
                text = stringResource(R.string.caption_fullscreen_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }
    }
}
