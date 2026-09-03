package com.focusme.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.FocusMeApp
import com.focusme.app.ui.theme.AccentEmerald
import com.focusme.app.ui.theme.AccentIndigo
import com.focusme.app.ui.theme.BgDark
import com.focusme.app.ui.theme.CardDark
import com.focusme.app.ui.theme.TextDim
import com.focusme.app.ui.theme.TextMain
import com.focusme.app.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = FocusMeApp.instance.preferences

    val showPill by prefs.showPill.collectAsState(initial = true)
    val whatsappStatus by prefs.whatsappStatusBlock.collectAsState(initial = true)
    val zaloVideo by prefs.zaloVideoBlock.collectAsState(initial = true)
    val reactiveNight by prefs.reactiveNight.collectAsState(initial = true)

    val hasOverlay = Settings.canDrawOverlays(context)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("⚙️ Focus & Protection Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain)
        }

        // Required Permissions Status
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .padding(16.dp)
            ) {
                Text("System Permissions Checklist", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Spacer(modifier = Modifier.height(10.dp))

                PermissionRow(
                    title = "1. Accessibility Service",
                    subtitle = "Required for app tracking & WhatsApp/Zalo status shields",
                    granted = true,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionRow(
                    title = "2. Display Over Other Apps",
                    subtitle = "Required for floating pill & reflection overlay",
                    granted = hasOverlay,
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionRow(
                    title = "3. Notification Access",
                    subtitle = "Required for 3-minute reactive night reply window",
                    granted = true,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }

        // In-App Shields
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("In-App Anti-Doomscroll Shields", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)

                SettingToggleRow(
                    title = "WhatsApp Status / Stories Shield",
                    desc = "Bounces you back to Chats tab when tapping Updates/Status",
                    checked = whatsappStatus,
                    onCheckedChange = { scope.launch { prefs.setMorningUnlockedToday() } }
                )

                SettingToggleRow(
                    title = "Zalo Video & Timeline Shield",
                    desc = "Blocks video reels and timeline feed in Zalo",
                    checked = zaloVideo,
                    onCheckedChange = {}
                )

                SettingToggleRow(
                    title = "Reactive-Only Night Messaging (9PM - 10AM)",
                    desc = "Only incoming messages unlock a 3-minute reply window",
                    checked = reactiveNight,
                    onCheckedChange = {}
                )

                SettingToggleRow(
                    title = "Floating In-App Timer Pill",
                    desc = "Shows live remaining seconds overlay on target apps",
                    checked = showPill,
                    onCheckedChange = { scope.launch { prefs.setShowPill(it) } }
                )
            }
        }

        // Active Rules Info
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardDark)
                    .padding(16.dp)
            ) {
                Column {
                    Text("⏰ Active Schedule Rules", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• 10:00 AM – 9:00 PM: 5 min/hour combined allowance", fontSize = 12.sp, color = TextDim)
                    Text("• 9:00 PM – 10:00 AM: 100% Lockout (Reactive reply only)", fontSize = 12.sp, color = TextDim)
                    Text("• Zero rollover between clock hours", fontSize = 12.sp, color = TextDim)
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text(subtitle, fontSize = 11.sp, color = TextDim)
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = if (granted) AccentEmerald.copy(alpha = 0.2f) else AccentIndigo),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (granted) "Active" else "Grant", fontSize = 11.sp, color = if (granted) AccentEmerald else Color.White)
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text(desc, fontSize = 11.sp, color = TextDim)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AccentEmerald, checkedTrackColor = AccentIndigo)
        )
    }
}
