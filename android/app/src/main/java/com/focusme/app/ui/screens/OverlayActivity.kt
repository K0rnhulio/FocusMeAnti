package com.focusme.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.FocusMeApp
import com.focusme.app.data.preferences.AppPreferences
import com.focusme.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayActivity : ComponentActivity() {

    private var currentReason by mutableStateOf("gauntlet_required")
    private var currentTargetPkg by mutableStateOf("")

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentReason = intent.getStringExtra("overlay_reason") ?: "gauntlet_required"
        currentTargetPkg = intent.getStringExtra("target_package") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        currentReason = intent.getStringExtra("overlay_reason") ?: "gauntlet_required"
        currentTargetPkg = intent.getStringExtra("target_package") ?: ""

        setContent {
            FocusMeTheme {
                val prefs = FocusMeApp.instance.preferences
                val lifeGoal by prefs.lifeGoal.collectAsState(initial = AppPreferences.DEFAULT_LIFE_GOAL)

                val hourKey = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault()).format(Date())
                val mazeHour by prefs.mazeSolvedHour.collectAsState(initial = "")
                val shakeHour by prefs.shakeSolvedHour.collectAsState(initial = "")
                val pushUpHour by prefs.pushUpSolvedHour.collectAsState(initial = "")

                val isMazeSolved = mazeHour == hourKey
                val isShakeSolved = shakeHour == hourKey
                val isPushUpSolved = pushUpHour == hourKey

                var currentStep by remember(currentReason) { mutableStateOf(currentReason) }
                val targetPkg = currentTargetPkg
                val isWebTarget = targetPkg.startsWith("web:")

                BackHandler {
                    when {
                        currentStep.startsWith("screen_") -> {
                            currentStep = "gauntlet_required"
                        }
                        isWebTarget -> {
                            openGoogleSearch()
                        }
                        else -> {
                            goToHomeScreen()
                        }
                    }
                }

                when (currentStep) {
                    "screen_maze" -> {
                        MazeGameScreen(
                            onCompleted = {
                                currentStep = "gauntlet_required"
                            }
                        )
                    }

                    "screen_shake" -> {
                        ShakeChallengeScreen(
                            onCompleted = {
                                currentStep = "gauntlet_required"
                            }
                        )
                    }

                    "screen_pushup" -> {
                        PushUpCounterScreen(
                            onCompleted = {
                                currentStep = "gauntlet_required"
                            }
                        )
                    }

                    "reflection_required" -> {
                        ReflectionOverlayScreen(
                            targetPackage = targetPkg,
                            onUnlocked = {
                                finish()
                            },
                            onCancel = {
                                if (isWebTarget) openGoogleSearch() else goToHomeScreen()
                            }
                        )
                    }

                    else -> {
                        val siteName = when {
                            isWebTarget -> targetPkg.removePrefix("web:")
                            targetPkg.contains("reddit") -> "Reddit"
                            targetPkg.contains("twitter") -> "Twitter / X"
                            targetPkg.contains("facebook") || targetPkg.contains("katana") -> "Facebook"
                            targetPkg.contains("instagram") -> "Instagram"
                            targetPkg.contains("tiktok") -> "TikTok"
                            targetPkg.contains("youtube") -> "YouTube"
                            else -> "Distracting Apps"
                        }

                        UnifiedLockOverlayScreen(
                            targetName = siteName,
                            lifeGoal = lifeGoal,
                            isMazeSolved = isMazeSolved,
                            isShakeSolved = isShakeSolved,
                            isPushUpSolved = isPushUpSolved,
                            isWebTarget = isWebTarget,
                            lockReason = currentReason,
                            onOpenGoogle = { openGoogleSearch() },
                            onLaunchMaze = { currentStep = "screen_maze" },
                            onLaunchShake = { currentStep = "screen_shake" },
                            onLaunchPushUp = { currentStep = "screen_pushup" },
                            onProceedToReflection = { currentStep = "reflection_required" },
                            onReturnToLife = { goToHomeScreen() }
                        )
                    }
                }
            }
        }
    }

    private fun openGoogleSearch() {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(browserIntent)
        } catch (e: Exception) {
            goToHomeScreen()
        }
        finish()
    }

    private fun goToHomeScreen() {
        val startMain = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(startMain)
        finish()
    }
}

/**
 * ⚡ UNIFIED LOCK OVERLAY SCREEN
 * Combines North Star Life Goal reality check, existential reflection, and the 3 Physical/Cognitive Toll Gates.
 * Built edge-to-edge with statusBarsPadding() & navigationBarsPadding() so no buttons are ever obscured.
 */
@Composable
fun UnifiedLockOverlayScreen(
    targetName: String,
    lifeGoal: String,
    isMazeSolved: Boolean,
    isShakeSolved: Boolean,
    isPushUpSolved: Boolean,
    isWebTarget: Boolean = false,
    lockReason: String = "gauntlet_required",
    onOpenGoogle: () -> Unit = {},
    onLaunchMaze: () -> Unit,
    onLaunchShake: () -> Unit,
    onLaunchPushUp: () -> Unit,
    onProceedToReflection: () -> Unit,
    onReturnToLife: () -> Unit
) {
    val scrollState = rememberScrollState()

    val completedCount = (if (isMazeSolved) 1 else 0) +
                         (if (isShakeSolved) 1 else 0) +
                         (if (isPushUpSolved) 1 else 0)
    val allCleared = completedCount == 3

    val isNightLocked = lockReason == "outside_schedule"
    val isQuotaExhausted = lockReason == "quota_exhausted"

    val lifeGoalGlowGradient = Brush.linearGradient(
        colors = listOf(
            Color(0x33F59E0B),
            Color(0x2238BDF8),
            Color(0x116366F1)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: Badge & Quick Google Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isNightLocked) "NIGHT LOCK (10PM - 10AM)" else "MEMENTO MORI • TIME IS FINITE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFBBF24),
                        letterSpacing = 0.8.sp
                    )
                }

                if (isWebTarget) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(9999.dp))
                            .clickable { onOpenGoogle() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Google",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Existential Prompt Title
            Text(
                text = "Is $targetName moving you closer to your real life?",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. North Star Goal Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFF59E0B).copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(lifeGoalGlowGradient)
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "YOUR NORTH STAR GOAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24),
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"$lifeGoal\"",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Compact Bento Reality Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1F1622).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("❌ Dopamine Trap", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentRose)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Brain fog, wasted hours, lost momentum.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF102820).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("🏆 Deep Work Pride", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentEmeraldGlow)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Real achievements, clarity, peace of mind.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Physical & Cognitive Toll Gates Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE60F172A))
                    .border(1.dp, GlassBorderGradient, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DISCIPLINE TOLL GATES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (allCleared) Color(0x2210B981) else AccentIndigo.copy(alpha = 0.25f))
                            .border(1.dp, if (allCleared) AccentEmerald else AccentIndigo.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (allCleared) "3/3 Cleared ✓" else "$completedCount/3 Solved",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allCleared) AccentEmeraldGlow else AccentCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Complete all 3 physical & cognitive challenges to unlock a 5-minute break this hour.",
                    fontSize = 11.sp,
                    color = TextDim,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Challenge 1: Tilt Maze
                TollGateRowCard(
                    title = "1. Gyro Tilt Maze",
                    subtitle = "Guide the ball to the green center",
                    icon = Icons.Default.SportsEsports,
                    isCompleted = isMazeSolved,
                    onClick = { if (!isNightLocked && !isQuotaExhausted) onLaunchMaze() }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Challenge 2: 50 Shakes
                TollGateRowCard(
                    title = "2. 50 Rapid Device Shakes",
                    subtitle = "Get blood moving & wake up the nervous system",
                    icon = Icons.Default.Bolt,
                    isCompleted = isShakeSolved,
                    onClick = { if (!isNightLocked && !isQuotaExhausted) onLaunchShake() }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Challenge 3: AR Overhead Reach & Pop
                TollGateRowCard(
                    title = "3. AR Overhead Reach & Pop (10 Orbs)",
                    subtitle = "Anchor head & stretch overhead to pop 10 floating orbs",
                    icon = Icons.Default.TouchApp,
                    isCompleted = isPushUpSolved,
                    onClick = { if (!isNightLocked && !isQuotaExhausted) onLaunchPushUp() }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Unlock Session / Gates Status Button
                if (allCleared) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = AccentEmerald.copy(alpha = 0.45f))
                            .clip(RoundedCornerShape(16.dp))
                            .background(SuccessGradient)
                            .clickable { onProceedToReflection() },
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
                                text = "All 3 Gates Cleared • Unlock Session ➔",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardInner.copy(alpha = 0.6f))
                            .border(1.dp, Color(0x33475569), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextDim,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isNightLocked -> "Locked Outside 10:00 AM - 9:00 PM"
                                    isQuotaExhausted -> "Hourly Quota Exhausted (5m max)"
                                    else -> "Solve ${3 - completedCount} More Gate(s) to Unlock"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDim
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Action Escape Routes
            if (isWebTarget) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(14.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF0284C7).copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0284C7), Color(0xFF2563EB), Color(0xFF4F46E5))
                            )
                        )
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onOpenGoogle() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Search On Google Instead (Work & Study)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Return to Life / Home Screen Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6))
                        )
                    )
                    .clickable { onReturnToLife() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Choose My Real Goals & Life (Exit)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun TollGateRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCompleted) Color(0x2210B981) else CardInner.copy(alpha = 0.55f))
            .border(
                1.dp,
                if (isCompleted) AccentEmerald.copy(alpha = 0.5f) else Color(0x22475569),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) AccentEmerald.copy(alpha = 0.2f) else AccentIndigo.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.Check else icon,
                        contentDescription = null,
                        tint = if (isCompleted) AccentEmeraldGlow else AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) AccentEmeraldGlow else TextMain
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = TextDim,
                        lineHeight = 14.sp
                    )
                }
            }

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCompleted) AccentEmerald.copy(alpha = 0.2f) else AccentIndigo.copy(alpha = 0.25f))
                    .border(
                        1.dp,
                        if (isCompleted) AccentEmerald.copy(alpha = 0.4f) else AccentIndigo.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isCompleted) "✓ Cleared" else "Start ➔",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) AccentEmeraldGlow else AccentCyan
                )
            }
        }
    }
}

