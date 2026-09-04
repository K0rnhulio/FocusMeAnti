package com.focusme.app.ui.screens

import android.net.Uri
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.FocusMeApp
import com.focusme.app.service.FocusAccessibilityService
import com.focusme.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onLaunchMaze: () -> Unit,
    onLaunchShakes: () -> Unit,
    onLaunchPushUps: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAccessibilityActive by remember { mutableStateOf(FocusAccessibilityService.isEnabled(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityActive = FocusAccessibilityService.isEnabled(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val db = FocusMeApp.instance.database
    val prefs = FocusMeApp.instance.preferences
    val hourKey = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault()).format(Date())
    val currentUsage by db.usageDao().observeUsage(hourKey).collectAsState(initial = null)
    val reflections by db.reflectionDao().observeAllReflections().collectAsState(initial = emptyList())

    val mazeHour by prefs.mazeSolvedHour.collectAsState(initial = "")
    val shakeHour by prefs.shakeSolvedHour.collectAsState(initial = "")
    val pushUpHour by prefs.pushUpSolvedHour.collectAsState(initial = "")
    val isMazeSolved = mazeHour == hourKey
    val isShakeSolved = shakeHour == hourKey
    val isPushUpSolved = pushUpHour == hourKey

    val cal = Calendar.getInstance()
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
    val isPermittedWindow = currentHour in 10..20

    val used = currentUsage?.usedSeconds ?: 0
    val quota = 300
    val remaining = if (isPermittedWindow) (quota - used).coerceAtLeast(0) else 0

    val progress by animateFloatAsState(
        targetValue = (remaining.toFloat() / quota).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "timer_ring"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Modern Top Brand Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(12.dp, CircleShape, spotColor = AccentCyan.copy(alpha = 0.4f))
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(AccentCyan, AccentIndigo)))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "FocusMe",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = "Discipline Engine",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDim
                        )
                    }
                }

                // Active Protection Status Chip
                val statusBg = when {
                    !isAccessibilityActive -> Color(0x33EF4444)
                    isPermittedWindow -> AccentEmerald.copy(alpha = 0.12f)
                    else -> Color(0x22EF4444)
                }
                val statusBorder = when {
                    !isAccessibilityActive -> AccentRose.copy(alpha = 0.6f)
                    isPermittedWindow -> AccentEmerald.copy(alpha = 0.3f)
                    else -> AccentRose.copy(alpha = 0.3f)
                }
                val statusDot = when {
                    !isAccessibilityActive -> AccentRose
                    isPermittedWindow -> AccentEmerald
                    else -> AccentRose
                }
                val statusText = when {
                    !isAccessibilityActive -> "Service OFF"
                    isPermittedWindow -> "Active (10AM - 9PM)"
                    else -> "Restricted (Locked)"
                }
                val statusTextColor = when {
                    !isAccessibilityActive -> AccentRose
                    isPermittedWindow -> AccentEmeraldGlow
                    else -> AccentRose
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(statusBg)
                        .border(1.dp, statusBorder, RoundedCornerShape(9999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusDot)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor
                        )
                    }
                }
            }
        }

        // Prominent Warning Banner if Accessibility is disabled
        if (!isAccessibilityActive) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = AccentRose.copy(alpha = 0.45f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF7F1D1D), Color(0xFF991B1B))
                            )
                        )
                        .border(1.dp, AccentRose.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .clickable {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Accessibility Service is OFF",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Android paused it during app update. Tap here to turn it back ON.",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Turn ON ➔",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }

        // Prominent Warning Banner if Display Over Other Apps is missing
        if (!hasOverlayPermission) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFF59E0B).copy(alpha = 0.45f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF78350F), Color(0xFF92400E))
                            )
                        )
                        .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .clickable {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Overlay Permission Required",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Required by Android to lock apps and display the mindful timer.",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Enable ➔",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }

        // Hero Circular Gauge Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 28.dp, elevation = 16.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(190.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background track
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = CardInner.copy(alpha = 0.8f),
                            strokeWidth = 14.dp
                        )
                        // Active Progress Arc
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = if (!isPermittedWindow) CardBorder else if (remaining <= 60) AccentRose else AccentCyanGlow,
                            strokeWidth = 14.dp
                        )

                        // Center Counter
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val mins = remaining / 60
                            val secs = remaining % 60
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "HOURLY QUOTA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDim,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPermittedWindow) "5m / clock hour • Zero rollover" else "100% Locked outside 10AM - 9PM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Quick Metrics Bento Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Daily Focus",
                    value = if (isPermittedWindow) "In Flow" else "Night Lock",
                    sub = "Strict Window",
                    icon = Icons.Rounded.Shield,
                    color = AccentCyan,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Reflections",
                    value = "${reflections.size} logged",
                    sub = "Mindful logs",
                    icon = Icons.Rounded.CheckCircle,
                    color = AccentEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Physical & Mental Challenge Gates Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Physical & Cognitive Toll Gates",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                if (isMazeSolved || isShakeSolved || isPushUpSolved) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentRose.copy(alpha = 0.15f))
                            .border(1.dp, AccentRose.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                scope.launch {
                                    prefs.resetHourlyGates()
                                    Toast.makeText(context, "Toll gates reset! Apps locked 🔒", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Reset & Lock 🔒",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentRose
                        )
                    }
                }
            }
        }

        // Challenge Launchers Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ChallengeLaunchCard(
                    title = "Tilt Maze",
                    subtitle = "Gyro Labyrinth",
                    icon = Icons.Rounded.Games,
                    gradient = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
                    isCompleted = isMazeSolved,
                    onClick = onLaunchMaze,
                    modifier = Modifier.weight(1f)
                )

                ChallengeLaunchCard(
                    title = "50 Shakes",
                    subtitle = "Blood Surge",
                    icon = Icons.Rounded.Bolt,
                    gradient = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
                    isCompleted = isShakeSolved,
                    onClick = onLaunchShakes,
                    modifier = Modifier.weight(1f)
                )

                ChallengeLaunchCard(
                    title = "Air Press",
                    subtitle = "5 Overhead Reps",
                    icon = Icons.Rounded.FitnessCenter,
                    gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4))),
                    isCompleted = isPushUpSolved,
                    onClick = onLaunchPushUps,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Today's Accountability Journal Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✍️ 30-Minute Accountability Log",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = "${reflections.size} entries",
                    fontSize = 11.sp,
                    color = TextDim
                )
            }
        }

        if (reflections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 18.dp, elevation = 6.dp)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reflections logged yet today. Each 5-minute break requires a 30-minute progress check-in.",
                        fontSize = 12.sp,
                        color = TextDim,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            items(reflections) { entry ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 16.dp, elevation = 4.dp)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestamp)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan
                                )
                            }
                            Text(
                                text = "Mindful Check-in",
                                fontSize = 10.sp,
                                color = TextDim
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${entry.answer}\"",
                            fontSize = 13.sp,
                            color = TextMain,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassCard(cornerRadius = 20.dp, elevation = 8.dp)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDim
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Text(
                text = sub,
                fontSize = 11.sp,
                color = TextDim
            )
        }
    }
}

@Composable
fun ChallengeLaunchCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    isCompleted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassCard(cornerRadius = 18.dp, elevation = 8.dp)
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isCompleted) Brush.linearGradient(listOf(AccentEmerald, AccentCyan)) else gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Rounded.CheckCircle else icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) AccentEmeraldGlow else TextMain
            )
            Text(
                text = if (isCompleted) "✓ Cleared" else subtitle,
                fontSize = 9.sp,
                fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                color = if (isCompleted) AccentEmeraldGlow else TextDim
            )
        }
    }
}
