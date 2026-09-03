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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.ui.theme.AccentCyan
import com.focusme.app.ui.theme.AccentEmerald
import com.focusme.app.ui.theme.AccentIndigo
import com.focusme.app.ui.theme.AccentRose
import com.focusme.app.ui.theme.BgDark
import com.focusme.app.ui.theme.CardDark
import com.focusme.app.ui.theme.FocusMeTheme
import com.focusme.app.ui.theme.TextDim
import com.focusme.app.ui.theme.TextMain
import com.focusme.app.ui.theme.TextMuted

class OverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reason = intent.getStringExtra("overlay_reason") ?: "quota_exhausted"
        val targetPkg = intent.getStringExtra("target_package") ?: ""
        val isWebTarget = targetPkg.startsWith("web:")

        setContent {
            FocusMeTheme {
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
                    "outside_schedule" -> {
                        val siteName = if (isWebTarget) targetPkg.removePrefix("web:") else "This app"
                        LockedScreen(
                            title = "Restricted Hours Lockout",
                            targetName = siteName,
                            description = "Leisure browsing on $siteName is completely locked outside 10:00 AM – 9:00 PM.",
                            isWebTarget = isWebTarget,
                            onOpenGoogle = { openGoogleSearch() },
                            onClose = { goToHomeScreen() }
                        )
                    }
                    else -> {
                        val siteName = if (isWebTarget) targetPkg.removePrefix("web:") else "This app"
                        LockedScreen(
                            title = "Hourly Quota Exhausted",
                            targetName = siteName,
                            description = "You have used your 5-minute combined allowance for $siteName for this clock hour. Zero rollover.",
                            isWebTarget = isWebTarget,
                            onOpenGoogle = { openGoogleSearch() },
                            onClose = { goToHomeScreen() }
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

@Composable
fun LockedScreen(
    title: String,
    targetName: String,
    description: String,
    isWebTarget: Boolean,
    onOpenGoogle: () -> Unit,
    onClose: () -> Unit
) {
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF090D16)
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
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = AccentIndigo.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(28.dp))
                .background(cardGradient)
                .border(1.dp, Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lock Glowing Icon Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AccentRose,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Target Badge
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain,
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.sp
            )

            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Blocked Target: $targetName",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Premium Google Search Action Button (For browser targets)
            if (isWebTarget) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(googleButtonGradient)
                        .clickable { onOpenGoogle() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Continue to Google Search",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Search freely without distraction traps",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Home Screen Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFF334155).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = TextDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Return to Home Screen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                    }
                }
            } else {
                // Standalone App Lock Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentIndigo)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Return to Home Screen",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
