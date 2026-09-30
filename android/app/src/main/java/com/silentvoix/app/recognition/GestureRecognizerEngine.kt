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
 * [analyze]; stabilised gestures come back through [onGesture], the hand's landmarks for every
 * frame through [onHand] (null once when the hand leaves) and errors through [onFailure], all on
 * [callbackExecutor].
 *
 * Creating it loads the model, so do that off the main thread. [create] throws if the model
 * cannot be loaded.
 */
class GestureRecognizerEngine private constructor(
    private val callbackExecutor: Executor,
    private val onGesture: (StableGesture) -> Unit,
    private val onHand: (HandPose?) -> Unit,
    private val onFailure: (RecognitionFailure) -> Unit,
) : AutoCloseable {

    private val stabilizer = GestureStabilizer()
    private lateinit var recognizer: GestureRecognizer
    private var lastTimestampMs = 0L
    private var handVisible = false

    // Geometry of the frames being analysed, read on the result thread to place landmarks.
    @Volatile private var frameRotation = 0
    @Volatile private var frameWidth = 1
    @Volatile private var frameHeight = 1

    @Volatile
    private var closed = false

    /** Runs on CameraX's analysis thread. Always closes [image]. */
    fun analyze(image: ImageProxy) {
        image.use {
            if (closed) return
            // MediaPipe requires strictly increasing timestamps in live-stream mode.
            val timestamp = maxOf(SystemClock.uptimeMillis(), lastTimestampMs + 1)
            lastTimestampMs = timestamp
            frameRotation = it.imageInfo.rotationDegrees
            frameWidth = it.width
            frameHeight = it.height
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
        reportHand(result)
        val top = result.gestures().firstOrNull()?.firstOrNull()
        val prediction = top
            ?.takeIf { phraseFor(it.categoryName()) != null }
            ?.let { FramePrediction(it.categoryName(), it.score()) }
        val gesture = stabilizer.onFrame(prediction) ?: return
        callbackExecutor.execute { if (!closed) onGesture(gesture) }
    }

    /** The model reports landmarks in the sensor image's frame, even when told the rotation. */
    private fun reportHand(result: GestureRecognizerResult) {
        val landmarks = result.landmarks().firstOrNull()?.takeIf { it.size == HandPose.LANDMARK_COUNT }
        if (landmarks == null) {
            if (handVisible) {
                handVisible = false
                callbackExecutor.execute { if (!closed) onHand(null) }
            }
            return
        }
        handVisible = true
        val points = FloatArray(HandPose.LANDMARK_COUNT * 2)
        landmarks.forEachIndexed { i, landmark ->
            points[i * 2] = landmark.x()
            points[i * 2 + 1] = landmark.y()
        }
        val pose = HandPose.fromSensor(points, frameRotation, frameWidth, frameHeight)
        callbackExecutor.execute { if (!closed) onHand(pose) }
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
            onHand: (HandPose?) -> Unit,
            onFailure: (RecognitionFailure) -> Unit,
        ): GestureRecognizerEngine {
            val engine = GestureRecognizerEngine(callbackExecutor, onGesture, onHand, onFailure)
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
