package com.focusme.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.FocusMeApp
import com.focusme.app.data.model.HourlyUsage
import com.focusme.app.data.model.ReflectionEntry
import com.focusme.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReflectionOverlayScreen(
    targetPackage: String = "",
    onUnlocked: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var reflectionText by remember { mutableStateOf("") }
    val minChars = 15
    val isValid = reflectionText.trim().length >= minChars
    val charProgress = (reflectionText.trim().length.toFloat() / minChars).coerceIn(0f, 1f)

    val quickTags = listOf(
        "💻 Coded feature",
        "🐛 Fixed bug",
        "📚 Studied / Read",
        "✍️ Planned & Wrote",
        "📧 Client emails"
    )

    val isWeb = targetPackage.startsWith("web:")
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 28.dp, elevation = 20.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Header Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AccentIndigo.copy(alpha = 0.15f))
                    .border(1.dp, AccentIndigo.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MINDFUL REFLECTION GATE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentCyan,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "What have you accomplished in the last 30 minutes?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                lineHeight = 24.sp
            )
            Text(
                text = "Acknowledge real-world progress before opening social feeds.",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            // Interactive Quick Tags
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickTags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardInner.copy(alpha = 0.7f))
                            .border(1.dp, Color(0x22475569), RoundedCornerShape(10.dp))
                            .clickable {
                                if (!reflectionText.contains(tag)) {
                                    reflectionText = "$tag: $reflectionText".trim()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextMain)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Modern Text Input
            OutlinedTextField(
                value = reflectionText,
                onValueChange = { reflectionText = it },
                placeholder = {
                    Text(
                        "e.g. Finished the project summary and answered team messages...",
                        color = TextDim,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = CardInner,
                    focusedContainerColor = Color(0x66060913),
                    unfocusedContainerColor = Color(0x66060913),
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar & Char Counter
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { charProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isValid) AccentEmerald else AccentCyan,
                    trackColor = CardInner
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isValid) "✓ Verified! Ready to claim 5-min session" else "Need ${minChars - reflectionText.trim().length} more characters",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isValid) AccentEmeraldGlow else TextDim
                    )
                    Text(
                        text = "${reflectionText.trim().length} / $minChars",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) AccentEmeraldGlow else TextDim
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Unlock Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isValid) PrimaryGradient else Brush.linearGradient(listOf(CardInner, CardInner))
                    )
                    .clickable(enabled = isValid) {
                        scope.launch {
                            val hourKey = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault()).format(Date())
                            val db = FocusMeApp.instance.database

                            db.reflectionDao().insert(
                                ReflectionEntry(
                                    hourKey = hourKey,
                                    answer = reflectionText.trim(),
                                    targetApp = targetPackage
                                )
                            )

                            val existing = db.usageDao().getUsage(hourKey) ?: HourlyUsage(hourKey = hourKey, usedSeconds = 0)
                            db.usageDao().insertOrUpdate(existing.copy(hasReflected = true))

                            onUnlocked()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isValid) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isValid) "Claim 5-Minute Session" else "Enter 15+ Characters to Unlock",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) Color.White else TextDim
                    )
                }
            }

            if (isWeb) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF0284C7).copy(alpha = 0.35f))
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0284C7),
                                    Color(0xFF2563EB),
                                    Color(0xFF4F46E5)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onCancel() },
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
                            text = "Return to Google Search",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cancel / Return
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCancel() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isWeb) "Return to Home Screen" else "Nevermind, stay focused in deep work",
                    fontSize = 12.sp,
                    color = TextDim
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
