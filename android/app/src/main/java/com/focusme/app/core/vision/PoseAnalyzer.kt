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
 * Biomechanical Pose Analyzer for Overhead Air Press / Military Press & Squats.
 *
 * Designed for desk/tabletop phone placement:
 * 1. Tracks head, shoulders, elbows, and wrists with phone propped in front of user.
 * 2. Bottom Phase: Hands at shoulder height, elbows bent.
 * 3. Top Phase: Hands pressed fully straight overhead above head level.
 * 4. Temporal constraints and EMA smoothing ensure no accidental false counts.
 */
class PoseAnalyzer(
    private val isPushUpMode: Boolean = true, // true = Overhead Air Press mode
    private val targetReps: Int = 5,
    private val onRepProgress: (current: Int, target: Int, angle: Double, statusMessage: String) -> Unit,
    private val onGoalReached: () -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "PoseAnalyzer"
        private const val MIN_CONFIDENCE = 0.60f
        private const val MIN_REP_DURATION_MS = 600L
        private const val MIN_TOP_HOLD_MS = 150L
    }

    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)

    private var repCount = 0
    private var smoothedAngle = 90.0
    private var isFirstAngle = true

    // State Machine for Overhead Air Press
    private enum class PressState {
        AT_SHOULDERS,
        PRESSING_UP,
        TOP_OVERHEAD,
        LOWERING_DOWN
    }

    private var currentPressState = PressState.AT_SHOULDERS
    private var topTimestamp = 0L
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
            onRepProgress(repCount, targetReps, 90.0, "Place phone on desk facing your torso")
            return
        }

        if (isPushUpMode) {
            analyzeOverheadAirPress(pose)
        } else {
            analyzeSquat(pose)
        }
    }

    /**
     * Overhead Air Press (Military Press) Analysis:
     * User sits or stands in front of phone placed on desk/table.
     * - Hands start at shoulder height (elbows bent).
     * - Press straight overhead above head level with arms extended.
     * - Return hands to shoulder height to complete 1 rep.
     */
    private fun analyzeOverheadAirPress(pose: Pose) {
        // Head / Nose reference landmark
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

        // Evaluate Left Arm Quality
        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val lElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val lWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val leftArmConfidence = if (lShoulder != null && lElbow != null && lWrist != null) {
            (lShoulder.inFrameLikelihood + lElbow.inFrameLikelihood + lWrist.inFrameLikelihood) / 3f
        } else 0f

        // Evaluate Right Arm Quality
        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val rightArmConfidence = if (rShoulder != null && rElbow != null && rWrist != null) {
            (rShoulder.inFrameLikelihood + rElbow.inFrameLikelihood + rWrist.inFrameLikelihood) / 3f
        } else 0f

        // Select Best Visible Arm
        val (shoulder, elbow, wrist) = when {
            leftArmConfidence >= rightArmConfidence && leftArmConfidence >= MIN_CONFIDENCE -> {
                Triple(lShoulder!!, lElbow!!, lWrist!!)
            }
            rightArmConfidence > leftArmConfidence && rightArmConfidence >= MIN_CONFIDENCE -> {
                Triple(rShoulder!!, rElbow!!, rWrist!!)
            }
            else -> {
                onRepProgress(repCount, targetReps, 90.0, "Torso obscured • Ensure shoulders & arms in frame")
                return
            }
        }

        // Calculate Joint Angle (Shoulder -> Elbow -> Wrist)
        val rawAngle = calculateAngle(shoulder, elbow, wrist)

        // Exponential Moving Average (EMA) Filter
        if (isFirstAngle) {
            smoothedAngle = rawAngle
            isFirstAngle = false
        } else {
            smoothedAngle = 0.60 * rawAngle + 0.40 * smoothedAngle
        }

        val now = System.currentTimeMillis()

        // Coordinate Check: In Android/MLKit, Y increases downwards!
        // So a lower Y value means physically HIGHER on the screen.
        val headReferenceY = nose?.position?.y ?: (shoulder.position.y - 120f)
        val isWristAboveHead = wrist.position.y < headReferenceY
        val isWristAtShoulders = wrist.position.y >= (headReferenceY - 20f)

        var status = ""

        when (currentPressState) {
            PressState.AT_SHOULDERS -> {
                status = "Hands at shoulders • Press up overhead"
                // Transition to PRESSING_UP when arms begin reaching above shoulders
                if (wrist.position.y < shoulder.position.y && smoothedAngle > 110.0) {
                    currentPressState = PressState.PRESSING_UP
                    repStartTimestamp = now
                    status = "Pressing overhead..."
                }
            }

            PressState.PRESSING_UP -> {
                status = "Press all the way above your head!"
                // Reached top overhead position: hands above head & arm extended (angle >= 140°)
                if (isWristAboveHead && smoothedAngle >= 140.0) {
                    currentPressState = PressState.TOP_OVERHEAD
                    topTimestamp = now
                    status = "Top reached! Lower down"
                } else if (isWristAtShoulders && smoothedAngle < 100.0) {
                    // Aborted rep
                    currentPressState = PressState.AT_SHOULDERS
                }
            }

            PressState.TOP_OVERHEAD -> {
                val holdDuration = now - topTimestamp
                if (holdDuration >= MIN_TOP_HOLD_MS) {
                    currentPressState = PressState.LOWERING_DOWN
                    status = "Now lower hands to shoulder level"
                } else {
                    status = "Hold overhead..."
                }
            }

            PressState.LOWERING_DOWN -> {
                status = "Lower hands to shoulders"
                // Hands returned to shoulder level: rep completed!
                if (isWristAtShoulders || smoothedAngle <= 115.0) {
                    val totalDuration = now - repStartTimestamp
                    val cooldown = now - lastRepCompletedTimestamp

                    if (totalDuration >= MIN_REP_DURATION_MS && cooldown > 450) {
                        repCount++
                        lastRepCompletedTimestamp = now
                        status = "✓ Rep $repCount verified!"

                        onRepProgress(repCount, targetReps, smoothedAngle, status)

                        if (repCount >= targetReps) {
                            onGoalReached()
                            return
                        }
                    }
                    currentPressState = PressState.AT_SHOULDERS
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

        if (smoothedAngle < 100.0 && currentPressState == PressState.AT_SHOULDERS) {
            currentPressState = PressState.TOP_OVERHEAD
            topTimestamp = now
            status = "Good squat depth! Stand up"
        } else if (smoothedAngle > 155.0 && currentPressState == PressState.TOP_OVERHEAD) {
            if (now - topTimestamp >= 300) {
                repCount++
                status = "✓ Squat $repCount counted!"
                if (repCount >= targetReps) {
                    onGoalReached()
                    return
                }
            }
            currentPressState = PressState.AT_SHOULDERS
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
