package com.focusme.app.core.vision

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

/**
 * AR Spatial Pose Analyzer for Overhead Reach & Pop Minigame.
 *
 * Tracks:
 * 1. Head Anchor Position: Verifies user's head is aligned inside the center guide.
 * 2. Fingertip & Hand Points: Extracts normalized (X, Y) coordinates of wrists, index fingers,
 *    and pinkies (with front-camera horizontal mirroring) for touching floating AR orbs.
 */
class PoseAnalyzer(
    private val isFrontCamera: Boolean = true,
    private val onFrameUpdate: (isHeadAligned: Boolean, handPoints: List<PointF>, headPoint: PointF?) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "PoseAnalyzer"
        private const val MIN_CONFIDENCE = 0.30f
    }

    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
            val frameWidth = (if (isRotated) imageProxy.height else imageProxy.width).toFloat()
            val frameHeight = (if (isRotated) imageProxy.width else imageProxy.height).toFloat()

            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { pose ->
                    processPose(pose, frameWidth, frameHeight)
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

    private fun processPose(pose: Pose, frameWidth: Float, frameHeight: Float) {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) {
            onFrameUpdate(false, emptyList(), null)
            return
        }

        // 1. Head Position (Nose primary, Eyes/Ears fallback)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }
        val lEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }
        val rEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }

        val headRawX = nose?.position?.x ?: ((lEye?.position?.x ?: 0f) + (rEye?.position?.x ?: 0f)) / 2f
        val headRawY = nose?.position?.y ?: lEye?.position?.y ?: rEye?.position?.y

        val headPoint = if (headRawY != null && headRawY > 0f && headRawX > 0f) {
            val normX = if (isFrontCamera) (1f - (headRawX / frameWidth)) else (headRawX / frameWidth)
            val normY = headRawY / frameHeight
            PointF(normX.coerceIn(0f, 1f), normY.coerceIn(0f, 1f))
        } else null

        // Head is aligned when inside center guide region: X in [0.28..0.72], Y in [0.35..0.80]
        val isHeadAligned = headPoint != null &&
                headPoint.x in 0.28f..0.72f &&
                headPoint.y in 0.32f..0.80f

        // 2. Hand & Fingertip Points
        val handLandmarkTypes = listOf(
            PoseLandmark.LEFT_INDEX,
            PoseLandmark.RIGHT_INDEX,
            PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_PINKY,
            PoseLandmark.RIGHT_PINKY,
            PoseLandmark.LEFT_THUMB,
            PoseLandmark.RIGHT_THUMB
        )

        val handPoints = mutableListOf<PointF>()
        for (landmarkType in handLandmarkTypes) {
            val lm = pose.getPoseLandmark(landmarkType)
            if (lm != null && lm.inFrameLikelihood >= MIN_CONFIDENCE) {
                val normX = if (isFrontCamera) (1f - (lm.position.x / frameWidth)) else (lm.position.x / frameWidth)
                val normY = lm.position.y / frameHeight
                handPoints.add(PointF(normX.coerceIn(0f, 1f), normY.coerceIn(0f, 1f)))
            }
        }

        onFrameUpdate(isHeadAligned, handPoints, headPoint)
    }
}
