package com.focusme.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.core.sensors.ShakeDetector
import com.focusme.app.ui.theme.*

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
        animationSpec = tween(durationMillis = 200),
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
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 28.dp, elevation = 20.dp)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BLOOD CIRCULATION TOLL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFBBF24),
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "50-Shake Kinetic Surge",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain
            )
            Text(
                text = "Vigorously pump the phone in your hand to activate your nervous system.",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp),
                lineHeight = 18.sp
            )

            // Circular Power Meter
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = CardInner,
                    strokeWidth = 14.dp,
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDone) AccentEmerald else Color(0xFFF59E0B),
                    strokeWidth = 14.dp,
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currentShakes",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                    Text(
                        text = "/ $targetShakes PUMPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDim,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isDone) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SuccessGradient)
                        .clickable { onCompleted() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Surge Complete • Claim Break", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }
                }
            } else {
                Text(
                    text = "Keep pumping until the energy circle fills up!",
                    fontSize = 12.sp,
                    color = TextDim
                )
            }
        }
    }
}
