package com.focusme.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.ui.theme.AccentCyan
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
                            title = "Outside Allowed Hours",
                            description = "$siteName is completely locked before 10:00 AM and after 9:00 PM.",
                            isWebTarget = isWebTarget,
                            onOpenGoogle = { openGoogleSearch() },
                            onClose = { goToHomeScreen() }
                        )
                    }
                    else -> {
                        val siteName = if (isWebTarget) targetPkg.removePrefix("web:") else "This app"
                        LockedScreen(
                            title = "Hourly Quota Exhausted",
                            description = "You have used your 5-minute combined allowance for $siteName for this clock hour.",
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
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
    description: String,
    isWebTarget: Boolean,
    onOpenGoogle: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardDark)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔒", fontSize = 44.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AccentRose,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isWebTarget) {
                Button(
                    onClick = onOpenGoogle,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("🔍 Open Google Search Instead", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Return to Home Screen", color = TextDim)
                }
            } else {
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Return to Home Screen", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
