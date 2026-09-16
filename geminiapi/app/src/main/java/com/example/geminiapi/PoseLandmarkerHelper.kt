package com.example.geminiapi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseLandmarkerHelper(
    private val context: Context,
    private val runningMode: RunningMode = RunningMode.LIVE_STREAM,
    private val minPoseDetectionConfidence: Float = 0.5f,
    private val minPoseTrackingConfidence: Float = 0.5f,
    private val minPosePresenceConfidence: Float = 0.5f,
    var poseLandmarkerHelperListener: LandmarkerListener? = null
) {

    interface LandmarkerListener {
        fun onError(error: String)
        fun onResults(resultBundle: ResultBundle)
    }

    data class ResultBundle(
        val results: List<PoseLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int
    )

    private var poseLandmarker: PoseLandmarker? = null

    init {
        setupPoseLandmarker()
    }

    fun clearPoseLandmarker() {
        try {
            poseLandmarker?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing landmarker: ${e.message}")
        }
        poseLandmarker = null
    }

    fun setupPoseLandmarker() {
        val modelName = "pose_landmarker_lite.task"
        
        // Manual Linker Priming: Help Android find the JNI library
        try {
            System.loadLibrary("mediapipe_tasks_vision_jni")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Manual load failed, letting MediaPipe handle it: ${e.message}")
        }
        
        // Safety check for asset file
        try {
            context.assets.open(modelName).close()
        } catch (e: Exception) {
            val error = "Model file missing in assets: $modelName"
            Log.e(TAG, error)
            poseLandmarkerHelperListener?.onError(error)
            return
        }

        val baseOptionBuilder = BaseOptions.builder()
        // Use CPU on emulators to avoid native crashes
        baseOptionBuilder.setDelegate(Delegate.CPU)
        baseOptionBuilder.setModelAssetPath(modelName)

        try {
            val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptionBuilder.build())
                .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                .setMinTrackingConfidence(minPoseTrackingConfidence)
                .setMinPosePresenceConfidence(minPosePresenceConfidence)
                .setRunningMode(runningMode)

            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder.setResultListener(this::returnLivestreamResult)
                optionsBuilder.setErrorListener(this::returnLivestreamError)
            }

            val options = optionsBuilder.build()
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            val errorMsg = "MediaPipe Error: ${e.message ?: "Unknown Exception"}"
            Log.e(TAG, errorMsg, e)
            poseLandmarkerHelperListener?.onError(errorMsg)
        } catch (e: Error) {
            val errorMsg = "MediaPipe Native Error: ${e.message ?: e.toString()}"
            Log.e(TAG, errorMsg, e)
            poseLandmarkerHelperListener?.onError(errorMsg)
        }
    }

    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (runningMode != RunningMode.LIVE_STREAM || poseLandmarker == null) return

        val frameTime = SystemClock.uptimeMillis()

        // Extract bitmap from ImageProxy safely
        val bitmap = try {
            imageProxy.toBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap conversion error", e)
            null
        } ?: return

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }

        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        poseLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(result: PoseLandmarkerResult, input: MPImage) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        poseLandmarkerHelperListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
    }

    private fun returnLivestreamError(error: RuntimeException) {
        poseLandmarkerHelperListener?.onError(error.message ?: "An unknown error has occurred")
    }

    companion object {
        const val TAG = "PoseLandmarkerHelper"
    }
}
