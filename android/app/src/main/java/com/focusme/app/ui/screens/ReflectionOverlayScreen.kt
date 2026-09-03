package com.focusme.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.FocusMeApp
import com.focusme.app.data.model.HourlyUsage
import com.focusme.app.data.model.ReflectionEntry
import com.focusme.app.ui.theme.AccentCyan
import com.focusme.app.ui.theme.AccentEmerald
import com.focusme.app.ui.theme.AccentIndigo
import com.focusme.app.ui.theme.BgDark
import com.focusme.app.ui.theme.CardDark
import com.focusme.app.ui.theme.CardInner
import com.focusme.app.ui.theme.TextDim
import com.focusme.app.ui.theme.TextMain
import com.focusme.app.ui.theme.TextMuted
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

    val quickTags = listOf(
        "💻 Coded feature: ",
        "🐛 Fixed bug: ",
        "📚 Studied: ",
        "✍️ Wrote docs: ",
        "📧 Handled emails: "
    )

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AccentIndigo.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "INTENTIONAL PAUSE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "What have you done the last 30 minutes?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Text(
                text = "Take 10 seconds to acknowledge your progress before opening social media.",
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Quick Tags
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickTags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardInner)
                            .clickable {
                                if (!reflectionText.startsWith(tag)) {
                                    reflectionText = tag + reflectionText
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(tag.trim(), fontSize = 11.sp, color = TextMain)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Input Field
            OutlinedTextField(
                value = reflectionText,
                onValueChange = { reflectionText = it },
                placeholder = { Text("Describe what you completed...", color = TextDim, fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = CardInner,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Counter status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isValid) "✓ Ready to unlock!" else "Need ${minChars - reflectionText.trim().length} more chars",
                    fontSize = 11.sp,
                    color = if (isValid) AccentEmerald else TextDim
                )
                Text(
                    text = "${reflectionText.trim().length} / $minChars",
                    fontSize = 11.sp,
                    color = if (isValid) AccentEmerald else TextDim
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Submit Button
            Button(
                onClick = {
                    if (isValid) {
                        scope.launch {
                            val hourKey = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault()).format(Date())
                            val db = FocusMeApp.instance.database
                            
                            // Save reflection
                            db.reflectionDao().insert(
                                ReflectionEntry(
                                    hourKey = hourKey,
                                    answer = reflectionText.trim(),
                                    targetApp = targetPackage
                                )
                            )

                            // Mark hour as reflected
                            val existing = db.usageDao().getUsage(hourKey) ?: HourlyUsage(hourKey = hourKey, usedSeconds = 0)
                            db.usageDao().insertOrUpdate(existing.copy(hasReflected = true))

                            onUnlocked()
                        }
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentIndigo,
                    disabledContainerColor = CardInner
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Unlock 5-Minute Session", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cancel Button
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nevermind, stay in deep flow", color = TextDim, fontSize = 12.sp)
            }
        }
    }
}
