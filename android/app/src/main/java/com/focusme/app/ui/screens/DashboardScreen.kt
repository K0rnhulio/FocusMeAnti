package com.focusme.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.FocusMeApp
import com.focusme.app.ui.theme.AccentCyan
import com.focusme.app.ui.theme.AccentEmerald
import com.focusme.app.ui.theme.AccentIndigo
import com.focusme.app.ui.theme.BgDark
import com.focusme.app.ui.theme.CardDark
import com.focusme.app.ui.theme.CardInner
import com.focusme.app.ui.theme.TextDim
import com.focusme.app.ui.theme.TextMain
import com.focusme.app.ui.theme.TextMuted
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
    val db = FocusMeApp.instance.database
    val hourKey = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault()).format(Date())
    val currentUsage by db.usageDao().observeUsage(hourKey).collectAsState(initial = null)
    val reflections by db.reflectionDao().observeAllReflections().collectAsState(initial = emptyList())

    val cal = Calendar.getInstance()
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
    val isPermittedWindow = currentHour in 10..20

    val used = currentUsage?.usedSeconds ?: 0
    val quota = 300
    val remaining = if (isPermittedWindow) (quota - used).coerceAtLeast(0) else 0

    val progress by animateFloatAsState(
        targetValue = (remaining.toFloat() / quota).coerceIn(0f, 1f),
        label = "timer_ring"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("🛡️ FocusMe", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Text("Android Anti-Distraction Engine", fontSize = 12.sp, color = TextDim)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isPermittedWindow) AccentEmerald.copy(alpha = 0.15f) else Color(0x3364748B))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isPermittedWindow) "Active: 10AM - 9PM" else "Locked Window",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPermittedWindow) AccentEmerald else TextDim
                    )
                }
            }
        }

        // Circular Timer Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDark)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = CardInner,
                            strokeWidth = 12.dp
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = if (remaining <= 60) Color(0xFFEF4444) else AccentCyan,
                            strokeWidth = 12.dp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val mins = remaining / 60
                            val secs = remaining % 60
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                            Text(
                                text = "/ 5m hourly allowance",
                                fontSize = 11.sp,
                                color = TextDim
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isPermittedWindow) "Combined limit • Zero rollover" else "Locked outside 10:00 AM - 9:00 PM",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // Morning Gauntlet & Physical Toll Quick Launchers
        item {
            Text("⚡ Physical & Mindful Challenge Gates", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLaunchMaze,
                    colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🐭 Tilt Maze", fontSize = 12.sp, color = TextMain)
                }
                Button(
                    onClick = onLaunchShakes,
                    colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⚡ 50 Shakes", fontSize = 12.sp, color = TextMain)
                }
                Button(
                    onClick = onLaunchPushUps,
                    colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("💪 Push-Ups", fontSize = 12.sp, color = TextMain)
                }
            }
        }

        // Today's Accountability Journal
        item {
            Text("✍️ Today's 30-Minute Reflection Log", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
        }

        if (reflections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardDark)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No check-ins logged yet today.", fontSize = 12.sp, color = TextDim)
                }
            }
        } else {
            items(reflections) { entry ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDark)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestamp)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                            Text("30-min Check-in", fontSize = 10.sp, color = TextDim)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${entry.answer}\"",
                            fontSize = 12.sp,
                            color = TextMain,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
