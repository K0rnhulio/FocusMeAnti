package com.focusme.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.core.sensors.ShakeDetector
import com.focusme.app.ui.theme.AccentEmerald
import com.focusme.app.ui.theme.AccentIndigo
import com.focusme.app.ui.theme.BgDark
import com.focusme.app.ui.theme.CardDark
import com.focusme.app.ui.theme.TextDim
import com.focusme.app.ui.theme.TextMain

@Composable
fun ShakeChallengeScreen(
    targetShakes: Int = 50,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    var currentShakes by remember { mutableIntStateOf(0) }
    var isDone by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val detector = ShakeDetector(
            context = context,
            targetShakes = targetShakes,
            onShakeProgress = { count, _ ->
                currentShakes = count
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(30, 80))
                }
            },
            onShakeCompleted = {
                isDone = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        )
        detector.start()
        onDispose {
            detector.stop()
        }
    }

    val progress by animateFloatAsState(
        targetValue = (currentShakes.toFloat() / targetShakes).coerceIn(0f, 1f),
        label = "shake_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚡ 50-Shake Blood Surge",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Text(
                text = "Pump the phone vigorously in your hand to get blood flowing!",
                fontSize = 14.sp,
                color = TextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Circular Power Meter
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = CardDark,
                    strokeWidth = 14.dp,
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDone) AccentEmerald else AccentIndigo,
                    strokeWidth = 14.dp,
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currentShakes",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                    Text(
                        text = "/ $targetShakes pumps",
                        fontSize = 13.sp,
                        color = TextDim
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (isDone) {
                Button(
                    onClick = onCompleted,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("✓ Claim Unlock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Text(
                    text = "Keep shaking until the circle is full!",
                    fontSize = 13.sp,
                    color = TextDim
                )
            }
        }
    }
}
