package com.focusme.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.hypot

data class PopOrb(
    val id: Int,
    val xRatio: Float,
    val yRatio: Float,
    val label: String
)

val orbList = listOf(
    PopOrb(1, 0.25f, 0.14f, "Stretch Left Hand Up! ↖"),
    PopOrb(2, 0.75f, 0.14f, "Stretch Right Hand Up! ↗"),
    PopOrb(3, 0.50f, 0.08f, "Reach Straight to Ceiling! ⬆"),
    PopOrb(4, 0.20f, 0.18f, "Reach Left Overhead! ↖"),
    PopOrb(5, 0.80f, 0.18f, "Reach Right Overhead! ↗"),
    PopOrb(6, 0.35f, 0.10f, "High Stretch Left! ⬆"),
    PopOrb(7, 0.65f, 0.10f, "High Stretch Right! ⬆"),
    PopOrb(8, 0.18f, 0.12f, "Full Reach Top-Left! ↖"),
    PopOrb(9, 0.82f, 0.12f, "Full Reach Top-Right! ↗"),
    PopOrb(10, 0.50f, 0.07f, "Final Grand Stretch to Top! 🌟")
)

@Composable
fun PushUpCounterScreen(
    targetReps: Int = 10,
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
    var isHeadAligned by remember { mutableStateOf(false) }
    var handPoints by remember { mutableStateOf<List<PointF>>(emptyList()) }
    var poppedCount by remember { mutableIntStateOf(0) }
    var isDone by remember { mutableStateOf(false) }
    var burstTarget by remember { mutableStateOf<PointF?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Floating orb pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val orbScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    // Clear burst effect after 350ms
    LaunchedEffect(burstTarget) {
        if (burstTarget != null) {
            delay(350)
            burstTarget = null
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
                    text = "Prop phone upright on your desk facing you.\n\nAnchor your head in the center guide and stretch your hands overhead to touch and pop 10 floating energy orbs.\n\n🔒 100% Private: Pose and motion detection runs strictly on your device without internet.",
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
                val screenWidth = maxWidth
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
                                    isFrontCamera = useFrontCamera,
                                    onFrameUpdate = { isAligned, hands, _ ->
                                        isHeadAligned = isAligned
                                        handPoints = hands

                                        // Collision Detection with Current Active Orb
                                        if (!isDone && poppedCount < orbList.size) {
                                            val target = orbList[poppedCount]
                                            var isHit = false

                                            for (pt in hands) {
                                                val dx = pt.x - target.xRatio
                                                val dy = (pt.y - target.yRatio) * 1.5f
                                                val dist = hypot(dx, dy)
                                                val isOverheadSweep = pt.y <= (target.yRatio + 0.04f) && abs(pt.x - target.xRatio) < 0.14f

                                                if (dist < 0.12f || isOverheadSweep) {
                                                    isHit = true
                                                    break
                                                }
                                            }

                                            if (isHit) {
                                                val poppedIndex = poppedCount
                                                poppedCount++
                                                burstTarget = PointF(target.xRatio, target.yRatio)

                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                                                }

                                                if (poppedIndex + 1 >= orbList.size) {
                                                    isDone = true
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        vibrator.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
                                                    }
                                                }
                                            }
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

                // 1. Center Head Anchor Guide Oval (Aligns Head / Torso)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 20.dp)
                        .size(width = 175.dp, height = 230.dp)
                        .clip(RoundedCornerShape(90.dp))
                        .background(if (isHeadAligned) Color(0x2210B981) else Color(0x1538BDF8))
                        .border(
                            width = if (isHeadAligned) 3.dp else 2.dp,
                            color = if (isHeadAligned) Color(0xFF10B981) else Color(0x6638BDF8),
                            shape = RoundedCornerShape(90.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = 12.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isHeadAligned) Color(0xE6065F46) else Color(0xD90F172A))
                            .border(
                                1.dp,
                                if (isHeadAligned) Color(0xFF34D399) else Color(0x6638BDF8),
                                RoundedCornerShape(9999.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isHeadAligned) "✓ Head In Position" else "Align Head In Guide",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isHeadAligned) Color(0xFF34D399) else AccentCyan,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                // 2. Active Popping Orb (Top 5% - 25% Zone)
                if (!isDone && poppedCount < orbList.size) {
                    val activeOrb = orbList[poppedCount]
                    val orbX = screenWidth * activeOrb.xRatio
                    val orbY = screenHeight * activeOrb.yRatio

                    Box(
                        modifier = Modifier
                            .offset(x = orbX - 35.dp, y = orbY - 35.dp)
                            .size(70.dp)
                            .scale(orbScale),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Pulsing Glow
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            (if (activeOrb.id == 10) Color(0xFFF59E0B) else AccentCyan).copy(alpha = 0.5f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Core Orb
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(14.dp, CircleShape, spotColor = if (activeOrb.id == 10) Color(0xFFF59E0B) else AccentCyan)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (activeOrb.id == 10)
                                            listOf(Color(0xFFFBBF24), Color(0xFFEA580C))
                                        else
                                            listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${activeOrb.id}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                // 3. Popped Burst Particle Effect
                if (burstTarget != null) {
                    val burstX = screenWidth * burstTarget!!.x
                    val burstY = screenHeight * burstTarget!!.y
                    Box(
                        modifier = Modifier
                            .offset(x = burstX - 50.dp, y = burstY - 50.dp)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0x6610B981))
                            .border(3.dp, Color(0xFF34D399), CircleShape)
                    )
                }

                // 4. Real-Time Hand Trackers (Fingertip Halos)
                for (hand in handPoints) {
                    val handX = screenWidth * hand.x
                    val handY = screenHeight * hand.y
                    Box(
                        modifier = Modifier
                            .offset(x = handX - 10.dp, y = handY - 10.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0x8838BDF8))
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }

                // 5. Modern Transparent HUD Overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
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
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Overhead Reach & Pop",
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

                    // Bottom Floating Score & Guidance HUD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 24.dp, elevation = 20.dp)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$poppedCount / ${orbList.size} Orbs Popped",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDone) AccentEmeraldGlow else TextMain,
                                letterSpacing = 0.5.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 10-Step Progress Track
                            Row(
                                modifier = Modifier.fillMaxWidth(0.85f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (i in 0 until orbList.size) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                if (i < poppedCount) AccentEmeraldGlow
                                                else Color(0x33475569)
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val currentPrompt = if (isDone) {
                                "✓ All 10 Orbs Popped! Excellent stretch!"
                            } else if (!isHeadAligned) {
                                "Anchor head in the center guide to target orbs"
                            } else {
                                orbList[poppedCount].label
                            }

                            Text(
                                text = currentPrompt,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDone || isHeadAligned) AccentEmeraldGlow else Color(0xFFFBBF24),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                    text = "10 Orbs Popped • Claim 5-Min Break ➔",
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
                                text = "Keep head in guide • Reach hands overhead to pop orbs",
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
