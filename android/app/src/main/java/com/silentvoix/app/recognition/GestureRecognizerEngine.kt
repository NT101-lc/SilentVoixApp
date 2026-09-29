package com.silentvoix.app.recognition

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.util.concurrent.Executor

private const val TAG = "GestureEngine"
private const val MODEL_ASSET = "gesture_recognizer.task"

/**
 * On-device gesture recognition with MediaPipe in live-stream mode. Feed it camera frames with
 * [analyze]; stabilised gestures come back through [onGesture] and errors through [onFailure],
 * both on [callbackExecutor].
 *
 * Creating it loads the model, so do that off the main thread. [create] throws if the model
 * cannot be loaded.
 */
class GestureRecognizerEngine private constructor(
    private val callbackExecutor: Executor,
    private val onGesture: (StableGesture) -> Unit,
    private val onFailure: (RecognitionFailure) -> Unit,
) : AutoCloseable {

    private val stabilizer = GestureStabilizer()
    private lateinit var recognizer: GestureRecognizer
    private var lastTimestampMs = 0L

    @Volatile
    private var closed = false

    /** Runs on CameraX's analysis thread. Always closes [image]. */
    fun analyze(image: ImageProxy) {
        image.use {
            if (closed) return
            // MediaPipe requires strictly increasing timestamps in live-stream mode.
            val timestamp = maxOf(SystemClock.uptimeMillis(), lastTimestampMs + 1)
            lastTimestampMs = timestamp
            val mpImage = BitmapImageBuilder(it.toBitmap()).build()
            val options = ImageProcessingOptions.builder()
                .setRotationDegrees(it.imageInfo.rotationDegrees)
                .build()
            try {
                recognizer.recognizeAsync(mpImage, options, timestamp)
            } catch (e: RuntimeException) {
                Log.e(TAG, "recognizeAsync failed", e)
                reportFailure()
            }
        }
    }

    /** Runs on MediaPipe's result thread, the only caller of [stabilizer]. */
    private fun onResult(result: GestureRecognizerResult) {
        if (closed) return
        val top = result.gestures().firstOrNull()?.firstOrNull()
        val prediction = top
            ?.takeIf { phraseFor(it.categoryName()) != null }
            ?.let { FramePrediction(it.categoryName(), it.score()) }
        val gesture = stabilizer.onFrame(prediction) ?: return
        callbackExecutor.execute { if (!closed) onGesture(gesture) }
    }

    private fun reportFailure() {
        callbackExecutor.execute { if (!closed) onFailure(RecognitionFailure.INFERENCE) }
    }

    override fun close() {
        closed = true
        if (::recognizer.isInitialized) recognizer.close()
    }

    companion object {
        fun create(
            context: Context,
            callbackExecutor: Executor,
            onGesture: (StableGesture) -> Unit,
            onFailure: (RecognitionFailure) -> Unit,
        ): GestureRecognizerEngine {
            val engine = GestureRecognizerEngine(callbackExecutor, onGesture, onFailure)
            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setResultListener { result, _ -> engine.onResult(result) }
                .setErrorListener { e ->
                    Log.e(TAG, "MediaPipe error", e)
                    engine.reportFailure()
                }
                .build()
            engine.recognizer = GestureRecognizer.createFromOptions(context, options)
            return engine
        }
    }
}
