package com.focusme.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.FocusMeApp
import com.focusme.app.data.preferences.AppPreferences
import com.focusme.app.ui.theme.*

class OverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reason = intent.getStringExtra("overlay_reason") ?: "quota_exhausted"
        val targetPkg = intent.getStringExtra("target_package") ?: ""
        val isWebTarget = targetPkg.startsWith("web:")

        setContent {
            FocusMeTheme {
                val lifeGoal by FocusMeApp.instance.preferences.lifeGoal.collectAsState(
                    initial = AppPreferences.DEFAULT_LIFE_GOAL
                )

                var currentStep by remember { mutableStateOf(reason) }

                when (currentStep) {
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

                        PurposeRealityCheckScreen(
                            targetName = siteName,
                            lifeGoal = lifeGoal,
                            isWebTarget = isWebTarget,
                            onOpenGoogle = { openGoogleSearch() },
                            onReturnToLife = { goToHomeScreen() }
                        )
                    }
                }
            }
        }
    }

    private fun openGoogleSearch() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(browserIntent)
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
 * Deep Existential Purpose & Goals Reality Check:
 * Shown whenever the user tries to open Reddit or distracting social apps.
 */
@Composable
fun PurposeRealityCheckScreen(
    targetName: String,
    lifeGoal: String,
    isWebTarget: Boolean,
    onOpenGoogle: () -> Unit,
    onReturnToLife: () -> Unit
) {
    val scrollState = rememberScrollState()

    val realityGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF111827),
            Color(0xFF070B14)
        )
    )

    val lifeGoalGlowGradient = Brush.linearGradient(
        colors = listOf(
            Color(0x33F59E0B),
            Color(0x2238BDF8),
            Color(0x116366F1)
        )
    )

    val actionButtonGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF10B981),
            Color(0xFF06B6D4),
            Color(0xFF3B82F6)
        )
    )

    val googleButtonGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF38BDF8),
            Color(0xFF6366F1),
            Color(0xFF8B5CF6)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(28.dp, RoundedCornerShape(32.dp), spotColor = AccentCyan.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(32.dp))
                .background(realityGradient)
                .border(1.dp, GlassBorderGradient, RoundedCornerShape(32.dp))
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Memento Mori Header Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(9999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MEMENTO MORI • TIME IS FINITE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFBBF24),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Wakeup Headline
            Text(
                text = "Is $targetName moving you closer to your real life?",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // YOUR ACTIVE NORTH STAR LIFE GOAL CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(lifeGoalGlowGradient)
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"$lifeGoal\"",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                        lineHeight = 22.sp,
                        fontFamily = FontFamily.Default
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // The Contrast: Cheap Dopamine vs Your Legacy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cost Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1F1622).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("❌ The Dopamine Trap", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentRose)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Brain fog, wasted hours, lost momentum, and regret.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Reward Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF102820).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("🏆 The Deep Work Pride", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentEmeraldGlow)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Real achievements, financial freedom, clarity, and peace of mind.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reality Statement
            Text(
                text = "Today will only happen once in your entire life. Do not trade your irreplaceable energy for an algorithm.",
                fontSize = 12.sp,
                color = TextDim,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PRIMARY HERO BUTTON: Return to Life & Real Goals
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = AccentEmerald.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(actionButtonGradient)
                    .clickable { onReturnToLife() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Choose My Real Goals & Life",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Optional: Continue to Google Search (if triggered in browser)
            if (isWebTarget) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable { onOpenGoogle() }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Search On Google Instead (Work & Study)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                    }
                }
            }
        }
    }
}
