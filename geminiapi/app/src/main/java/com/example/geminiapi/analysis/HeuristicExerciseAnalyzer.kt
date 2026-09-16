package com.example.geminiapi.analysis

import kotlin.math.abs

data class HeuristicResult(
    val exerciseName: String,
    val isDown: Boolean,
    val progress: Float, // 0.0 to 1.0 based on form
    val repCount: Int
)

class HeuristicExerciseAnalyzer {
    private var pushupReps = 0
    private var situpReps = 0
    
    private var inDownState = false

    fun reset() {
        pushupReps = 0
        situpReps = 0
        inDownState = false
    }

    fun analyzePushup(landmarks: List<Point3D>): HeuristicResult {
        // 1. Elbow angle (Shoulder-Elbow-Wrist)
        val leftElbow = Point3D.angle(landmarks[11], landmarks[13], landmarks[15])
        val rightElbow = Point3D.angle(landmarks[12], landmarks[14], landmarks[16])
        val avgElbowAngle = (leftElbow + rightElbow) / 2.0

        // 2. Robustness Check: Is the person actually in a pushup position?
        // We check the vertical projection of the torso. 
        // If standing, the Y distance between shoulder and hip is large (~0.2 - 0.4).
        // If horizontal (pushup), this distance shrinks significantly.
        val shoulderY = (landmarks[11].y + landmarks[12].y) / 2f
        val hipY = (landmarks[23].y + landmarks[24].y) / 2f
        val torsoVerticalDist = abs(shoulderY - hipY)
        
        // If the vertical distance is too large, the person is likely standing.
        // We'll ignore elbow bends if they are standing upright.
        val isHorizontal = torsoVerticalDist < 0.15f

        // Pushup Thresholds
        val UP_ANGLE = 155.0
        val DOWN_ANGLE = 95.0

        var progress = 0f
        if (isHorizontal) {
            progress = ((UP_ANGLE - avgElbowAngle) / (UP_ANGLE - DOWN_ANGLE)).toFloat().coerceIn(0f, 1f)

            if (!inDownState && avgElbowAngle < DOWN_ANGLE) {
                inDownState = true
            } else if (inDownState && avgElbowAngle > UP_ANGLE) {
                pushupReps++
                inDownState = false
            }
        } else {
            // Reset state if they stand up mid-exercise
            inDownState = false
            progress = 0f
        }

        return HeuristicResult("pushups", inDownState, progress, pushupReps)
    }

    fun analyzeSitup(landmarks: List<Point3D>): HeuristicResult {
        // Hip angle (Shoulder-Hip-Knee)
        val leftHip = Point3D.angle(landmarks[11], landmarks[23], landmarks[25])
        val rightHip = Point3D.angle(landmarks[12], landmarks[24], landmarks[26])
        val avgHipAngle = (leftHip + rightHip) / 2.0

        // Situp Thresholds
        val DOWN_ANGLE = 150.0 // Lying flat
        val UP_ANGLE = 65.0   // Fully up

        val progress = ((DOWN_ANGLE - avgHipAngle) / (DOWN_ANGLE - UP_ANGLE)).toFloat().coerceIn(0f, 1f)

        if (!inDownState && avgHipAngle < UP_ANGLE) {
            inDownState = true // Reached top
        } else if (inDownState && avgHipAngle > DOWN_ANGLE) {
            situpReps++
            inDownState = false // Returned to bottom
        }

        return HeuristicResult("situps", inDownState, progress, situpReps)
    }
}
