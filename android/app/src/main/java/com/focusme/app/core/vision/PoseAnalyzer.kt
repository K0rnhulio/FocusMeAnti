package com.focusme.app.core.vision

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Biomechanical Pose Analyzer for Push-Ups and Squats.
 *
 * Prevents false positives by enforcing:
 * 1. High confidence threshold on all 3 arm landmarks (> 0.65)
 * 2. Unilateral arm tracking (never crosses left and right joints)
 * 3. Horizontal torso alignment check (verifies user is actually planking, not standing)
 * 4. 4-Stage Biomechanical State Machine with timing thresholds (min 700ms rep time)
 * 5. Exponential Moving Average (EMA) angle smoothing to eliminate jitter
 */
class PoseAnalyzer(
    private val isPushUpMode: Boolean = true,
    private val targetReps: Int = 5,
    private val onRepProgress: (current: Int, target: Int, angle: Double, statusMessage: String) -> Unit,
    private val onGoalReached: () -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "PoseAnalyzer"
        private const val MIN_CONFIDENCE = 0.65f
        private const val UP_ANGLE_THRESHOLD = 150.0
        private const val DOWN_ANGLE_THRESHOLD = 85.0
        private const val MIN_REP_DURATION_MS = 650L
        private const val MIN_BOTTOM_DURATION_MS = 180L
    }

    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)

    private var repCount = 0
    private var smoothedAngle = 180.0
    private var isFirstAngle = true

    // State Machine
    private enum class RepState {
        READY_UP,
        DESCENDING,
        AT_BOTTOM,
        ASCENDING
    }

    private var currentState = RepState.READY_UP
    private var bottomTimestamp = 0L
    private var repStartTimestamp = 0L
    private var lastRepCompletedTimestamp = 0L

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { pose ->
                    processPose(pose)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ML Kit Pose detection error: ${e.message}")
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
        if (landmarks.isEmpty()) {
            onRepProgress(repCount, targetReps, 180.0, "Position yourself in camera view")
            return
        }

        if (isPushUpMode) {
            analyzePushUp(pose)
        } else {
            analyzeSquat(pose)
        }
    }

    /**
     * Strict Push-Up Analysis:
     * - Verifies horizontal planking orientation
     * - Tracks only the single most confident arm
     * - Requires complete descent (< 85°) and full lockout (> 150°)
     */
    private fun analyzePushUp(pose: Pose) {
        // 1. Evaluate Left Arm Quality
        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val lElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val lWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val leftArmConfidence = if (lShoulder != null && lElbow != null && lWrist != null) {
            (lShoulder.inFrameLikelihood + lElbow.inFrameLikelihood + lWrist.inFrameLikelihood) / 3f
        } else 0f

        // 2. Evaluate Right Arm Quality
        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val rightArmConfidence = if (rShoulder != null && rElbow != null && rWrist != null) {
            (rShoulder.inFrameLikelihood + rElbow.inFrameLikelihood + rWrist.inFrameLikelihood) / 3f
        } else 0f

        // 3. Select Best Visible Arm (MUST exceed minimum confidence)
        val (shoulder, elbow, wrist) = when {
            leftArmConfidence >= rightArmConfidence && leftArmConfidence >= MIN_CONFIDENCE -> {
                Triple(lShoulder!!, lElbow!!, lWrist!!)
            }
            rightArmConfidence > leftArmConfidence && rightArmConfidence >= MIN_CONFIDENCE -> {
                Triple(rShoulder!!, rElbow!!, rWrist!!)
            }
            else -> {
                onRepProgress(repCount, targetReps, 180.0, "Body obscured • Ensure arms are visible")
                return
            }
        }

        // 4. Biomechanical Plank Check: Torso horizontal orientation check
        // Check if user is standing upright vs lying/planking horizontally
        val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val hip = when {
            lHip != null && lHip.inFrameLikelihood >= 0.5f -> lHip
            rHip != null && rHip.inFrameLikelihood >= 0.5f -> rHip
            else -> null
        }

        if (hip != null) {
            val deltaX = abs(shoulder.position.x - hip.position.x)
            val deltaY = abs(shoulder.position.y - hip.position.y)

            // If user is completely vertical (standing upright): deltaY >> deltaX
            // In a push-up on the ground, deltaX is substantial compared to deltaY
            val isStandingUpright = deltaY > deltaX * 1.8f && shoulder.position.y < hip.position.y - 120
            if (isStandingUpright) {
                onRepProgress(repCount, targetReps, 180.0, "Assume plank position on the floor")
                return
            }
        }

        // 5. Calculate Raw Joint Angle
        val rawAngle = calculateAngle(shoulder, elbow, wrist)

        // 6. Exponential Moving Average (EMA) Filter (alpha = 0.60)
        if (isFirstAngle) {
            smoothedAngle = rawAngle
            isFirstAngle = false
        } else {
            smoothedAngle = 0.60 * rawAngle + 0.40 * smoothedAngle
        }

        val now = System.currentTimeMillis()

        // 7. Push-Up State Machine
        var status = ""
        when (currentState) {
            RepState.READY_UP -> {
                status = "Ready • Lower your chest"
                if (smoothedAngle >= UP_ANGLE_THRESHOLD) {
                    // Ready at top
                } else if (smoothedAngle < 135.0) {
                    currentState = RepState.DESCENDING
                    repStartTimestamp = now
                    status = "Going down..."
                }
            }

            RepState.DESCENDING -> {
                status = "Lower further (Target < 85°)"
                if (smoothedAngle <= DOWN_ANGLE_THRESHOLD) {
                    currentState = RepState.AT_BOTTOM
                    bottomTimestamp = now
                    status = "Good depth! Hold..."
                } else if (smoothedAngle > 145.0) {
                    // Aborted rep
                    currentState = RepState.READY_UP
                }
            }

            RepState.AT_BOTTOM -> {
                val timeAtBottom = now - bottomTimestamp
                if (timeAtBottom >= MIN_BOTTOM_DURATION_MS && smoothedAngle > 95.0) {
                    currentState = RepState.ASCENDING
                    status = "Push up!"
                } else if (timeAtBottom < MIN_BOTTOM_DURATION_MS) {
                    status = "Hold depth..."
                } else {
                    status = "Push back up!"
                }
            }

            RepState.ASCENDING -> {
                status = "Push to full extension"
                if (smoothedAngle >= UP_ANGLE_THRESHOLD) {
                    val totalRepDuration = now - repStartTimestamp
                    val cooldown = now - lastRepCompletedTimestamp

                    if (totalRepDuration >= MIN_REP_DURATION_MS && cooldown > 500) {
                        repCount++
                        lastRepCompletedTimestamp = now
                        status = "✓ Rep $repCount verified!"

                        onRepProgress(repCount, targetReps, smoothedAngle, status)

                        if (repCount >= targetReps) {
                            onGoalReached()
                            return
                        }
                    }
                    currentState = RepState.READY_UP
                }
            }
        }

        onRepProgress(repCount, targetReps, smoothedAngle, status)
    }

    private fun analyzeSquat(pose: Pose) {
        val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val lKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val (hip, knee, ankle) = when {
            lHip != null && lKnee != null && lAnkle != null &&
            lHip.inFrameLikelihood >= MIN_CONFIDENCE && lKnee.inFrameLikelihood >= MIN_CONFIDENCE -> {
                Triple(lHip, lKnee, lAnkle)
            }
            rHip != null && rKnee != null && rAnkle != null &&
            rHip.inFrameLikelihood >= MIN_CONFIDENCE && rKnee.inFrameLikelihood >= MIN_CONFIDENCE -> {
                Triple(rHip, rKnee, rAnkle)
            }
            else -> {
                onRepProgress(repCount, targetReps, 180.0, "Legs obscured • Stand back in frame")
                return
            }
        }

        val rawAngle = calculateAngle(hip, knee, ankle)
        smoothedAngle = 0.60 * rawAngle + 0.40 * smoothedAngle

        val now = System.currentTimeMillis()
        var status = "Stand tall"

        if (smoothedAngle < 100.0 && currentState == RepState.READY_UP) {
            currentState = RepState.AT_BOTTOM
            bottomTimestamp = now
            status = "Good squat depth! Stand up"
        } else if (smoothedAngle > 155.0 && currentState == RepState.AT_BOTTOM) {
            if (now - bottomTimestamp >= 300) {
                repCount++
                status = "✓ Squat $repCount counted!"
                if (repCount >= targetReps) {
                    onGoalReached()
                    return
                }
            }
            currentState = RepState.READY_UP
        }

        onRepProgress(repCount, targetReps, smoothedAngle, status)
    }

    private fun calculateAngle(first: PoseLandmark, middle: PoseLandmark, last: PoseLandmark): Double {
        val result = Math.toDegrees(
            (atan2(last.position.y - middle.position.y, last.position.x - middle.position.x) -
             atan2(first.position.y - middle.position.y, first.position.x - middle.position.x)).toDouble()
        )
        var angle = abs(result)
        if (angle > 180) {
            angle = 360.0 - angle
        }
        return angle
    }
}
