package com.silentvoix.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.silentvoix.app.ui.scene.skyWash
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.silentvoix.app.R
import com.silentvoix.app.ui.theme.EyebrowStyle

/**
 * A phrase shown as large as it will go, to hold up to the other person. Tap anywhere to close;
 * [onReplay], when given, adds a button that speaks it again.
 */
@Composable
fun FullscreenCaptionDialog(text: String, onDismiss: () -> Unit, onReplay: (() -> Unit)? = null) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        FullscreenCaption(text = text, onDismiss = onDismiss, onReplay = onReplay)
    }
}

@Composable
internal fun FullscreenCaption(text: String, onDismiss: () -> Unit, onReplay: (() -> Unit)? = null) {
    // The page colour sits under the sky wash, which the Surface itself would otherwise paint over.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .skyWash(height = 320.dp)
                .clickable(onClick = onDismiss),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.caption_fullscreen_eyebrow).uppercase(),
                        style = EyebrowStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(end = 56.dp),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.caption_close))
                    }
                }
                // Long typed sentences scroll rather than shrink below a readable size.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(end = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    // The phrase floats up into place and settles, like a leaf landing.
                    val arrival = remember(text) { Animatable(0f) }
                    LaunchedEffect(text) { arrival.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow)) }
                    Text(
                        text = text,
                        style = when {
                            text.length <= 18 -> MaterialTheme.typography.displayLarge
                            text.length <= 48 -> MaterialTheme.typography.displayMedium
                            else -> MaterialTheme.typography.displaySmall
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .graphicsLayer {
                                val p = arrival.value
                                alpha = p.coerceIn(0f, 1f)
                                translationY = (1f - p) * 64.dp.toPx()
                                scaleX = 0.92f + 0.08f * p
                                scaleY = 0.92f + 0.08f * p
                            }
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (onReplay != null) {
                        FilledTonalButton(
                            onClick = onReplay,
                            modifier = Modifier.heightIn(min = 52.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_volume_up), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_replay))
                        }
                    }
                    Text(
                        text = stringResource(R.string.caption_fullscreen_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
