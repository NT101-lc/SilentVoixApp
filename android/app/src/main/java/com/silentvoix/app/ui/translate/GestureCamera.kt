package com.silentvoix.app.ui.translate

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.silentvoix.app.recognition.GestureRecognizerEngine
import com.silentvoix.app.recognition.Recognition
import com.silentvoix.app.recognition.RecognitionFailure
import com.silentvoix.app.recognition.phraseFor
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private const val TAG = "GestureCamera"

/**
 * Live camera preview with on-device gesture recognition. The camera and model run while this
 * is in the composition and the screen is started; leaving the composition releases both.
 *
 * Callbacks arrive on the main thread: [onReady] once the camera is streaming frames to the
 * model, [onRecognized] per stabilised gesture, [onFailure] if the camera or model fails.
 */
@Composable
fun GestureCamera(
    onReady: () -> Unit,
    onRecognized: (Recognition) -> Unit,
    onFailure: (RecognitionFailure) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnRecognized by rememberUpdatedState(onRecognized)
    val currentOnFailure by rememberUpdatedState(onFailure)

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // TextureView keeps the preview clipped to the stage's rounded corners.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        // Loads the model, runs analysis and closes the engine: every engine call is on this thread.
        val analysisExecutor = Executors.newSingleThreadExecutor()
        var engine: GestureRecognizerEngine? = null
        var cameraProvider: ProcessCameraProvider? = null
        var useCases: Array<UseCase> = emptyArray()
        var disposed = false

        fun bindCamera(readyEngine: GestureRecognizerEngine) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                if (disposed) return@addListener
                try {
                    val provider = providerFuture.get()
                    val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    val preview = Preview.Builder().build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also { it.setAnalyzer(analysisExecutor, readyEngine::analyze) }
                    useCases = arrayOf(preview, analysis)
                    provider.bindToLifecycle(lifecycleOwner, selector, *useCases)
                    cameraProvider = provider
                    currentOnReady()
                } catch (e: Exception) {
                    Log.e(TAG, "Camera bind failed", e)
                    currentOnFailure(RecognitionFailure.CAMERA_UNAVAILABLE)
                }
            }, mainExecutor)
        }

        analysisExecutor.execute {
            val created = try {
                GestureRecognizerEngine.create(
                    context = context.applicationContext,
                    callbackExecutor = mainExecutor,
                    onGesture = { gesture ->
                        phraseFor(gesture.label)?.let { phrase ->
                            currentOnRecognized(
                                Recognition(
                                    text = context.getString(phrase),
                                    confidencePercent = (gesture.confidence * 100).roundToInt(),
                                ),
                            )
                        }
                    },
                    onFailure = { currentOnFailure(it) },
                )
            } catch (e: Exception) {
                Log.e(TAG, "Gesture model failed to load", e)
                mainExecutor.execute {
                    if (!disposed) currentOnFailure(RecognitionFailure.MODEL_UNAVAILABLE)
                }
                return@execute
            }
            engine = created
            mainExecutor.execute { if (!disposed) bindCamera(created) }
        }

        onDispose {
            disposed = true
            cameraProvider?.unbind(*useCases)
            analysisExecutor.execute { engine?.close() }
            analysisExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
