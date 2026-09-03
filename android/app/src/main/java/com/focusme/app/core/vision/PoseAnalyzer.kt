package com.focusme.app.core.vision

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlin.math.atan2

/**
 * Real-time Pose Analyzer using Google ML Kit.
 * Tracks push-ups and squats using 3-joint angle kinematics.
 */
class PoseAnalyzer(
    private val isPushUpMode: Boolean = true,
    private val targetReps: Int = 5,
    private val onRepProgress: (current: Int, target: Int, angle: Double) -> Unit,
    private val onGoalReached: () -> Unit
) : ImageAnalysis.Analyzer {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)

    private var repCount = 0
    private var isDownPhase = false

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { pose ->
                    processPose(pose)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun processPose(pose: Pose) {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) return

        if (isPushUpMode) {
            // Push-up analysis using Shoulder -> Elbow -> Wrist
            val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
            val elbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
            val wrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

            if (shoulder != null && elbow != null && wrist != null) {
                val elbowAngle = calculateAngle(shoulder, elbow, wrist)
                onRepProgress(repCount, targetReps, elbowAngle)

                // Push-up Rep State Machine
                if (elbowAngle < 90.0) {
                    isDownPhase = true
                } else if (elbowAngle > 155.0 && isDownPhase) {
                    isDownPhase = false
                    repCount++
                    onRepProgress(repCount, targetReps, elbowAngle)

                    if (repCount >= targetReps) {
                        onGoalReached()
                    }
                }
            }
        } else {
            // Squat analysis using Hip -> Knee -> Ankle
            val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
            val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

            if (hip != null && knee != null && ankle != null) {
                val kneeAngle = calculateAngle(hip, knee, ankle)
                onRepProgress(repCount, targetReps, kneeAngle)

                if (kneeAngle < 100.0) {
                    isDownPhase = true
                } else if (kneeAngle > 160.0 && isDownPhase) {
                    isDownPhase = false
                    repCount++
                    onRepProgress(repCount, targetReps, kneeAngle)

                    if (repCount >= targetReps) {
                        onGoalReached()
                    }
                }
            }
        }
    }

    private fun calculateAngle(first: PoseLandmark, middle: PoseLandmark, last: PoseLandmark): Double {
        val result = Math.toDegrees(
            (atan2(last.position.y - middle.position.y, last.position.x - middle.position.x) -
             atan2(first.position.y - middle.position.y, first.position.x - middle.position.x)).toDouble()
        )
        var angle = Math.abs(result)
        if (angle > 180) {
            angle = 360.0 - angle
        }
        return angle
    }
}
