package com.focusme.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.focusme.app.FocusMeApp
import com.focusme.app.core.vision.PoseAnalyzer
import com.focusme.app.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun PushUpCounterScreen(
    targetReps: Int = 5,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var useFrontCamera by remember { mutableStateOf(true) }
    var repCount by remember { mutableIntStateOf(0) }
    var reachProgress by remember { mutableFloatStateOf(0f) }
    var isTargetReached by remember { mutableStateOf(false) }
    var targetLineRatio by remember { mutableFloatStateOf(0.25f) }
    var coachingMessage by remember { mutableStateOf("Position phone ~0.5-1.5m away facing you") }
    var isDone by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(isTargetReached) {
        if (isTargetReached) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        if (!hasCameraPermission) {
            // Camera Permission Request Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(20.dp, CircleShape, spotColor = AccentCyan.copy(alpha = 0.4f))
                        .clip(CircleShape)
                        .background(AccentIndigo.copy(alpha = 0.2f))
                        .border(2.dp, AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = AccentCyanGlow,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Camera Access Required",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Prop phone upright on your desk facing your torso.\n\nPerform 5 Overhead Air Presses (Military Press without weights) by pressing both hands straight above your head.\n\n🔒 100% Private: All computer vision runs strictly on your device without internet.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryGradient)
                        .clickable {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Grant Camera Permission & Begin",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenHeight = maxHeight

                // Camera Live View + ML Kit Pose Detection
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                val analyzer = PoseAnalyzer(
                                    isPushUpMode = true,
                                    targetReps = targetReps,
                                    onRepProgress = { count, _, progress, status, reached, lineRatio ->
                                        repCount = count
                                        reachProgress = progress
                                        coachingMessage = status
                                        isTargetReached = reached
                                        targetLineRatio = lineRatio
                                    },
                                    onGoalReached = {
                                        isDone = true
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                                        }
                                    }
                                )

                                imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                                val preferredSelector = if (useFrontCamera) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }

                                val selector = if (cameraProvider.hasCamera(preferredSelector)) {
                                    preferredSelector
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("PushUpCounter", "Error binding CameraX: ${e.message}", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Dynamic Overhead Target Line (adapts dynamically to user's head & scale)
                val lineOffsetY = screenHeight * targetLineRatio
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = lineOffsetY)
                        .padding(horizontal = 16.dp)
                ) {
                    // Glowing horizontal line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isTargetReached) 4.dp else 2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isTargetReached)
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF10B981))
                                    )
                                else
                                    Brush.horizontalGradient(
                                        listOf(Color(0x2238BDF8), Color(0xFF38BDF8), Color(0x2238BDF8))
                                    )
                            )
                    )

                    // Target Line Center Pill Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-14).dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isTargetReached) Color(0xE6065F46) else Color(0xD90F172A))
                            .border(
                                1.dp,
                                if (isTargetReached) Color(0xFF34D399) else Color(0x8838BDF8),
                                RoundedCornerShape(9999.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isTargetReached) Color(0xFF34D399) else AccentCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isTargetReached) "✓ TARGET REACHED!" else "OVERHEAD TARGET LINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isTargetReached) Color(0xFF34D399) else AccentCyan,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Instant screen edge green glow when target lockout is reached
                if (isTargetReached) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(5.dp, Color(0xCC10B981))
                    )
                }

                // Modern Transparent HUD Overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(Color(0xD90F172A))
                                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(9999.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Overhead Air Press",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }

                        // Camera Switch Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xD90F172A))
                                .border(1.dp, Color(0x3338BDF8), CircleShape)
                                .clickable { useFrontCamera = !useFrontCamera },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Bottom Floating Rep Counter HUD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 24.dp, elevation = 20.dp)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$repCount / $targetReps",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDone || isTargetReached) AccentEmeraldGlow else TextMain,
                                letterSpacing = 1.sp
                            )

                            // Reach Extension Bar
                            val reachPct = (reachProgress * 100).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(0.8f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Overhead Reach: $reachPct%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTargetReached || reachPct >= 90) AccentEmeraldGlow else TextDim
                                )
                                Text(
                                    text = if (isTargetReached) "✓ Lockout" else "Press Up",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTargetReached) AccentEmeraldGlow else AccentCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Glowing Progress Track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0x33475569))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(reachProgress)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (isTargetReached)
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF10B981), Color(0xFF34D399))
                                                )
                                            else
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                                                )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = coachingMessage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (coachingMessage.contains("✓") || coachingMessage.contains("Good") || coachingMessage.contains("Lockout") || isTargetReached) AccentEmeraldGlow else AccentCyanGlow,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isDone) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SuccessGradient)
                                .clickable {
                                    scope.launch {
                                        val hourKey = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault()).format(Date())
                                        FocusMeApp.instance.preferences.markGateSolved("pushup", hourKey)
                                        onCompleted()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "5 Overhead Presses Verified • Claim Break",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xD9000000))
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Prop phone upright on desk • Press both hands above target line",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
