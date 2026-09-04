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
 * Biomechanical Pose Analyzer for Overhead Air Press (Military Press) & Squats.
 *
 * Overhead Air Press Biomechanics:
 * 1. Normalized Anatomical Scale: scale = abs(shoulderY - noseY) adapts dynamically to phone distance.
 * 2. Hand & Fingertip Tracking: Uses WRIST, INDEX, and PINKY landmarks for highest reach point.
 * 3. Overhead Target Zone: Hands must cross 20%-40% above head top (handY < headTop - 0.3 * scale).
 * 4. Ceiling Hit Detection: If close to phone and hands touch top edge (<= 7% frame height), counts as top reach.
 * 5. Elbow Elevation Check: Elbows must rise above shoulders (elbowY < shoulderY) for valid press lockout.
 * 6. Bottom Rack Zone: Hands return down to shoulder level (handY >= shoulderY - 0.15 * scale).
 */
class PoseAnalyzer(
    private val isPushUpMode: Boolean = true, // true = Overhead Air Press mode
    private val targetReps: Int = 5,
    private val onRepProgress: (
        current: Int,
        target: Int,
        progressPercent: Float,
        statusMessage: String,
        isTargetReached: Boolean,
        targetLineYRatio: Float
    ) -> Unit,
    private val onGoalReached: () -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "PoseAnalyzer"
        private const val MIN_CONFIDENCE = 0.35f
        private const val MIN_REP_DURATION_MS = 500L
        private const val MIN_TOP_HOLD_MS = 120L
    }

    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)

    private var repCount = 0
    private var smoothedProgress = 0f
    private var isFirstProgress = true

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
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
            val frameHeight = if (isRotated) imageProxy.width else imageProxy.height

            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { pose ->
                    processPose(pose, frameHeight.toFloat())
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

    private fun processPose(pose: Pose, frameHeight: Float) {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) {
            onRepProgress(repCount, targetReps, 0f, "Place phone facing you • Torso in frame", false, 0.25f)
            return
        }

        if (isPushUpMode) {
            analyzeOverheadAirPress(pose, frameHeight)
        } else {
            analyzeSquat(pose)
        }
    }

    /**
     * Overhead Air Press (Military Press) Analysis:
     * - Normalized Anatomical Proportions
     * - Hands & Fingertips (Wrist, Index, Pinky)
     * - Ceiling Hit Detection & Elbow Elevation Check
     */
    private fun analyzeOverheadAirPress(pose: Pose, frameHeight: Float) {
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val lEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
        val rEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)

        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val lElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)

        val lWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val lIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
        val rIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)

        val lPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
        val rPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)

        // 1. Calculate Shoulder Center & Check Confidence
        val shoulderYs = listOfNotNull(
            lShoulder?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y,
            rShoulder?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y
        )

        if (shoulderYs.isEmpty()) {
            onRepProgress(repCount, targetReps, smoothedProgress, "Position shoulders and chest in frame", false, 0.25f)
            return
        }

        val shoulderCenterY = shoulderYs.average().toFloat()

        // 2. Head / Nose Reference
        val noseY = nose?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y
            ?: lEye?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y
            ?: rEye?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y
            ?: (shoulderCenterY - 120f)

        // 3. Dynamic Scale Unit (Nose to Shoulder Center distance)
        val scale = abs(shoulderCenterY - noseY).coerceAtLeast(60f)

        // Top of the head: ~0.4 * scale above nose (Y decreases going up)
        val headTopY = noseY - 0.4f * scale

        // Overhead Target Line: 30% scale above head top (20%-40% zone)
        val targetOverheadY = headTopY - 0.3f * scale

        // Bottom Rack Position: Hands near or below shoulder level
        val rackY = shoulderCenterY - 0.15f * scale

        // Ceiling Threshold (top 7% of camera frame height)
        val ceilingThresholdY = frameHeight * 0.07f

        // Normalized Y ratio for rendering the visual target line on screen
        val targetLineYRatio = (targetOverheadY / frameHeight).coerceIn(0.08f, 0.65f)

        // 4. Track Hands & Fingertips (Highest point reached by hands)
        val leftHandYs = listOfNotNull(
            lWrist?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y,
            lIndex?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y,
            lPinky?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y
        )
        val rightHandYs = listOfNotNull(
            rWrist?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y,
            rIndex?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y,
            rPinky?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y
        )

        val lHandY = leftHandYs.minOrNull()
        val rHandY = rightHandYs.minOrNull()

        if (lHandY == null && rHandY == null) {
            onRepProgress(repCount, targetReps, smoothedProgress, "Bring hands into view at shoulder level", false, targetLineYRatio)
            return
        }

        // The highest hand point
        val highestHandY = listOfNotNull(lHandY, rHandY).minOrNull()!!

        // 5. Track Elbows for Biomechanical Elevation Check
        val lElbowY = lElbow?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y
        val rElbowY = rElbow?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.position?.y
        val highestElbowY = listOfNotNull(lElbowY, rElbowY).minOrNull()
        val lowestElbowY = listOfNotNull(lElbowY, rElbowY).maxOrNull()

        // Full overhead criteria:
        // - Hands cross above overhead target line OR hit top ceiling
        val isHandOverhead = highestHandY <= targetOverheadY || highestHandY <= ceilingThresholdY

        // - Elbows elevated above shoulders
        val isElbowElevated = (highestElbowY != null && highestElbowY < (shoulderCenterY + 15f)) || (highestHandY <= ceilingThresholdY)

        val isFullOverheadLockout = isHandOverhead && isElbowElevated

        // Rack / Bottom criteria:
        // - Hands returned to shoulder level
        val isHandsAtRack = highestHandY >= rackY || (lowestElbowY != null && lowestElbowY > (shoulderCenterY - 0.1f * scale))

        // 6. Smooth Progress Calculation (0.0 at rack -> 1.0 at overhead target)
        val rawProgress = ((rackY - highestHandY) / (rackY - targetOverheadY)).coerceIn(0f, 1f)
        if (isFirstProgress) {
            smoothedProgress = rawProgress
            isFirstProgress = false
        } else {
            smoothedProgress = 0.50f * rawProgress + 0.50f * smoothedProgress
        }

        val now = System.currentTimeMillis()
        var status = ""
        var isTargetReached = false

        when (currentPressState) {
            PressState.AT_SHOULDERS -> {
                status = "Hands at shoulders • Press straight up overhead"
                isTargetReached = false
                if (smoothedProgress > 0.30f && highestHandY < shoulderCenterY) {
                    currentPressState = PressState.PRESSING_UP
                    repStartTimestamp = now
                    status = "Pressing overhead..."
                }
            }

            PressState.PRESSING_UP -> {
                status = "Press above the target line!"
                if (isFullOverheadLockout) {
                    currentPressState = PressState.TOP_OVERHEAD
                    topTimestamp = now
                    isTargetReached = true
                    status = "✓ Overhead Lockout reached! Now lower down"
                } else if (isHandsAtRack && smoothedProgress < 0.20f && (now - repStartTimestamp > 1200)) {
                    // Reset if aborted
                    currentPressState = PressState.AT_SHOULDERS
                }
            }

            PressState.TOP_OVERHEAD -> {
                isTargetReached = true
                val holdDuration = now - topTimestamp
                if (holdDuration >= MIN_TOP_HOLD_MS) {
                    currentPressState = PressState.LOWERING_DOWN
                    status = "Good! Lower hands back to shoulder level"
                } else {
                    status = "Hold overhead..."
                }
            }

            PressState.LOWERING_DOWN -> {
                isTargetReached = false
                status = "Lower hands to shoulders"
                if (isHandsAtRack || smoothedProgress <= 0.25f) {
                    val totalDuration = now - repStartTimestamp
                    val cooldown = now - lastRepCompletedTimestamp

                    if (totalDuration >= MIN_REP_DURATION_MS && cooldown > 400) {
                        repCount++
                        lastRepCompletedTimestamp = now
                        status = "✓ Rep $repCount verified!"

                        onRepProgress(repCount, targetReps, smoothedProgress, status, false, targetLineYRatio)

                        if (repCount >= targetReps) {
                            onGoalReached()
                            return
                        }
                    }
                    currentPressState = PressState.AT_SHOULDERS
                }
            }
        }

        onRepProgress(repCount, targetReps, smoothedProgress, status, isTargetReached, targetLineYRatio)
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
                onRepProgress(repCount, targetReps, 0f, "Legs obscured • Stand back in frame", false, 0.3f)
                return
            }
        }

        val rawAngle = calculateAngle(hip, knee, ankle)
        val now = System.currentTimeMillis()
        var status = "Stand tall"
        var isTargetReached = false

        if (rawAngle < 105.0 && currentPressState == PressState.AT_SHOULDERS) {
            currentPressState = PressState.TOP_OVERHEAD
            topTimestamp = now
            isTargetReached = true
            status = "Good squat depth! Stand up"
        } else if (rawAngle > 155.0 && currentPressState == PressState.TOP_OVERHEAD) {
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

        val progress = ((180.0 - rawAngle) / 80.0).toFloat().coerceIn(0f, 1f)
        onRepProgress(repCount, targetReps, progress, status, isTargetReached, 0.3f)
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
