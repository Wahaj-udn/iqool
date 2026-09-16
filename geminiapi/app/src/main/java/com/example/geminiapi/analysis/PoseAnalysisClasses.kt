package com.example.geminiapi.analysis

import kotlin.math.*

data class Point3D(val x: Float, val y: Float, val z: Float) {
    operator fun minus(other: Point3D) = Point3D(x - other.x, y - other.y, z - other.z)
    operator fun plus(other: Point3D) = Point3D(x + other.x, y + other.y, z + other.z)
    operator fun div(scalar: Float) = Point3D(x / scalar, y / scalar, z / scalar)
    operator fun times(scalar: Float) = Point3D(x * scalar, y * scalar, z * scalar)
    fun norm() = sqrt(x.pow(2) + y.pow(2) + z.pow(2))

    companion object {
        fun angle(first: Point3D, mid: Point3D, last: Point3D): Double {
            val res = Math.toDegrees(
                atan2(last.y - mid.y, last.x - mid.x) -
                        atan2(first.y - mid.y, first.x - mid.x).toDouble()
            )
            var angle = abs(res)
            if (angle > 180) angle = 360 - angle
            return angle
        }
    }
}

data class PoseSample(
    val name: String,
    val className: String,
    val embedding: List<Point3D>
)

object PoseEmbedding {
    private const val LEFT_HIP = 23
    private const val RIGHT_HIP = 24
    private const val LEFT_SHOULDER = 11
    private const val RIGHT_SHOULDER = 12
    private const val TORSO_MULTIPLIER = 2.5f

    fun getEmbedding(landmarks: List<Point3D>): List<Point3D> {
        val normalized = normalize(landmarks)
        val embedding = mutableListOf<Point3D>()
        embedding.add(normalized[11] - normalized[13]) 
        embedding.add(normalized[13] - normalized[15]) 
        embedding.add(normalized[12] - normalized[14]) 
        embedding.add(normalized[14] - normalized[16]) 
        embedding.add(normalized[23] - normalized[25]) 
        embedding.add(normalized[25] - normalized[27]) 
        embedding.add(normalized[24] - normalized[26]) 
        embedding.add(normalized[26] - normalized[28]) 
        embedding.add(normalized[11] - normalized[15]) 
        embedding.add(normalized[12] - normalized[16]) 
        embedding.add(normalized[23] - normalized[27]) 
        embedding.add(normalized[24] - normalized[28]) 
        embedding.add(normalized[11] - normalized[23]) 
        embedding.add(normalized[12] - normalized[24]) 
        embedding.add(normalized[11] - normalized[12]) 
        embedding.add(normalized[23] - normalized[24]) 
        embedding.add(normalized[15] - normalized[16]) 
        embedding.add(normalized[27] - normalized[28]) 
        return embedding
    }

    private fun normalize(landmarks: List<Point3D>): List<Point3D> {
        val centerHips = Point3D(
            (landmarks[LEFT_HIP].x + landmarks[RIGHT_HIP].x) / 2f,
            (landmarks[LEFT_HIP].y + landmarks[RIGHT_HIP].y) / 2f,
            (landmarks[LEFT_HIP].z + landmarks[RIGHT_HIP].z) / 2f
        )
        val translated = landmarks.map { it - centerHips }
        val centerShoulders = Point3D(
            (landmarks[LEFT_SHOULDER].x + landmarks[RIGHT_SHOULDER].x) / 2f,
            (landmarks[LEFT_SHOULDER].y + landmarks[RIGHT_SHOULDER].y) / 2f,
            (landmarks[LEFT_SHOULDER].z + landmarks[RIGHT_SHOULDER].z) / 2f
        )
        val torsoSize = (centerShoulders - centerHips).norm()
        var maxDist = torsoSize * TORSO_MULTIPLIER
        translated.forEach { maxDist = max(maxDist, it.norm()) }
        return translated.map { (it / maxDist) * 100f }
    }
}

class PoseClassifier(private val poseSamples: List<PoseSample>) {
    private val MAX_DISTANCE_TOP_K = 30f
    private val TOP_K = 5
    private val AXES_WEIGHTS = Point3D(1f, 1f, 0.2f)

    fun classify(landmarks: List<Point3D>): ClassificationResult {
        val embedding = PoseEmbedding.getEmbedding(landmarks)
        val distances = poseSamples.map { sample ->
            Pair(sample.className, computeDist(sample.embedding, embedding))
        }.sortedBy { it.second }

        val topK = distances.take(TOP_K)
        val allClassNames = poseSamples.map { it.className }.distinct()
        val confidences = mutableMapOf<String, Float>()

        for (className in allClassNames) {
            val relevantTopK = topK.filter { it.first == className }
            val count = relevantTopK.size
            if (count >= (TOP_K / 2) + 1) {
                val avgDist = relevantTopK.map { it.second }.average().toFloat()
                val score = (1f - (avgDist / MAX_DISTANCE_TOP_K)).coerceIn(0f, 1f) * 10f
                confidences[className] = score
            } else {
                confidences[className] = 0f
            }
        }

        val bestClass = confidences.maxByOrNull { it.value }?.key ?: "unknown"
        return ClassificationResult(bestClass, confidences)
    }

    private fun computeDist(s1: List<Point3D>, s2: List<Point3D>): Float {
        var sum = 0f
        for (i in s1.indices) {
            val diff = s1[i] - s2[i]
            sum += (diff.x * AXES_WEIGHTS.x).pow(2) + (diff.y * AXES_WEIGHTS.y).pow(2) + (diff.z * AXES_WEIGHTS.z).pow(2)
        }
        return sqrt(sum) / s1.size
    }
}

data class ClassificationResult(
    val winnerClassName: String,
    val confidences: Map<String, Float>
)

class EMASmoothing(private val windowSize: Int = 10) {
    private val history = mutableMapOf<String, MutableList<Float>>()

    fun getSmoothedResult(result: ClassificationResult): ClassificationResult {
        val smoothedConfidences = mutableMapOf<String, Float>()
        
        for ((className, confidence) in result.confidences) {
            val classHistory = history.getOrPut(className) { mutableListOf() }
            classHistory.add(0, confidence)
            if (classHistory.size > windowSize) classHistory.removeAt(classHistory.size - 1)
            smoothedConfidences[className] = classHistory.average().toFloat()
        }

        val bestClass = smoothedConfidences.maxByOrNull { it.value }?.key ?: "unknown"
        return ClassificationResult(bestClass, smoothedConfidences)
    }
}

class RepetitionCounter(
    val exerciseName: String,
    private val enterThreshold: Float = 6.0f,
    private val exitThreshold: Float = 4.0f
) {
    private var numRepeats = 0
    private var poseEntered = false

    fun addClassificationResult(result: ClassificationResult): Int {
        val confidence = result.confidences[exerciseName] ?: 0f

        if (!poseEntered) {
            if (confidence > enterThreshold) {
                poseEntered = true
            }
        } else {
            if (confidence < exitThreshold) {
                numRepeats++
                poseEntered = false
            }
        }
        return numRepeats
    }
}
