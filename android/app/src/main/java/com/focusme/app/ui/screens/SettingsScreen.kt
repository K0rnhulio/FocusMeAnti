package com.focusme.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.FocusMeApp
import com.focusme.app.ui.theme.AccentCyan
import com.focusme.app.ui.theme.AccentCyanGlow
import com.focusme.app.ui.theme.AccentEmerald
import com.focusme.app.ui.theme.AccentEmeraldGlow
import com.focusme.app.ui.theme.AccentIndigo
import com.focusme.app.ui.theme.AccentRose
import com.focusme.app.ui.theme.BgDark
import com.focusme.app.ui.theme.CardInner
import com.focusme.app.ui.theme.TextDim
import com.focusme.app.ui.theme.TextMain
import com.focusme.app.ui.theme.TextMuted
import com.focusme.app.ui.theme.glassCard
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentIndigo.copy(alpha = 0.15f))
                        .border(1.dp, AccentIndigo.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Shields & Permissions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                    Text(
                        text = "Anti-Tamper & In-App Filter Status",
                        fontSize = 11.sp,
                        color = TextDim
                    )
                }
            }
        }

        // Required Permissions Checklist Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24.dp, elevation = 10.dp)
                    .padding(20.dp)
            ) {
                Text(
                    text = "🛡️ Core Android Permissions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(14.dp))

                ModernPermissionRow(
                    title = "Accessibility Engine",
                    subtitle = "Blocks target apps & in-app newsfeeds",
                    icon = Icons.Rounded.Shield,
                    granted = true,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ModernPermissionRow(
                    title = "Display Over Other Apps",
                    subtitle = "Draws floating timer pill & reflection HUD",
                    icon = Icons.Rounded.Visibility,
                    granted = hasOverlay,
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ModernPermissionRow(
                    title = "Notification Access",
                    subtitle = "3-minute reactive night reply window",
                    icon = Icons.Rounded.Notifications,
                    granted = true,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }

        // In-App Anti-Doomscroll Shields
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24.dp, elevation = 10.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🚫 In-App Anti-Doomscroll Shields",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                ModernSwitchRow(
                    title = "WhatsApp Status / Stories Shield",
                    desc = "Bounces you to Chats tab when tapping Status/Updates",
                    icon = Icons.Rounded.Chat,
                    checked = whatsappStatus,
                    onCheckedChange = {}
                )

                ModernSwitchRow(
                    title = "Zalo Full Newsfeed Shield",
                    desc = "Blocks 'Nhật ký' (Timeline) & 'Khám phá' feed tabs",
                    icon = Icons.Rounded.Shield,
                    checked = zaloVideo,
                    onCheckedChange = {}
                )

                ModernSwitchRow(
                    title = "Reactive Night Messaging (9PM - 10AM)",
                    desc = "Only incoming notifications unlock a 3-minute reply window",
                    icon = Icons.Rounded.Nightlight,
                    checked = reactiveNight,
                    onCheckedChange = {}
                )

                ModernSwitchRow(
                    title = "Floating In-App Timer Pill",
                    desc = "Shows countdown pill overlay on target social apps",
                    icon = Icons.Rounded.Timer,
                    checked = showPill,
                    onCheckedChange = { scope.launch { prefs.setShowPill(it) } }
                )
            }
        }

        // Active Rules Info Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 20.dp, elevation = 6.dp)
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ironclad Schedule Rules",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• 10:00 AM – 9:00 PM: 5 min/hour combined allowance\n• 9:00 PM – 10:00 AM: 100% Lockout (Reactive reply only)\n• Zero rollover between clock hours",
                        fontSize = 12.sp,
                        color = TextDim,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ModernPermissionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardInner.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (granted) AccentEmerald.copy(alpha = 0.15f) else AccentRose.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted) AccentEmerald else AccentRose,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Text(subtitle, fontSize = 10.sp, color = TextDim)
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (granted) AccentEmerald.copy(alpha = 0.15f) else AccentIndigo)
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (granted) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = AccentEmeraldGlow,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (granted) "Active" else "Grant",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (granted) AccentEmeraldGlow else Color.White
                )
            }
        }
    }
}

@Composable
fun ModernSwitchRow(
    title: String,
    desc: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AccentIndigo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Text(desc, fontSize = 10.sp, color = TextDim, lineHeight = 14.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = CardInner
            )
        )
    }
}
