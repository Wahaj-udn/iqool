package com.example.geminiapi

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

@Composable
fun PoseOverlay(
    results: PoseLandmarkerResult?,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        results?.let { poseLandmarkerResult ->
            for (landmarks in poseLandmarkerResult.landmarks()) {
                // Draw connections
                PoseLandmarker.POSE_LANDMARKS.forEach { connection ->
                    val start = landmarks[connection.start()]
                    val end = landmarks[connection.end()]

                    val startX = if (isFrontCamera) (1f - start.x()) * canvasWidth else start.x() * canvasWidth
                    val endX = if (isFrontCamera) (1f - end.x()) * canvasWidth else end.x() * canvasWidth

                    drawLine(
                        color = Color.Green,
                        start = Offset(startX, start.y() * canvasHeight),
                        end = Offset(endX, end.y() * canvasHeight),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw landmarks
                landmarks.forEach { landmark ->
                    val x = if (isFrontCamera) (1f - landmark.x()) * canvasWidth else landmark.x() * canvasWidth
                    drawCircle(
                        color = Color.Red,
                        radius = 8f,
                        center = Offset(x, landmark.y() * canvasHeight)
                    )
                }
            }
        }
    }
}
