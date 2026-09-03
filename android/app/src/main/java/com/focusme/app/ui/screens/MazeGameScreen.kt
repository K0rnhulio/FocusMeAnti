package com.focusme.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.core.game.MazeGenerator
import com.focusme.app.core.game.MazePhysicsEngine
import com.focusme.app.core.sensors.TiltSensorManager
import com.focusme.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MazeGameScreen(
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    val maze = remember { MazeGenerator.generate(rows = 9, cols = 7) }
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var isSolved by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val tiltManager = TiltSensorManager(context) { x, y ->
            tiltX = x
            tiltY = y
        }
        tiltManager.start()
        onDispose {
            tiltManager.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🐭 Gyroscope Labyrinth",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                    Text(
                        text = "Physically tilt phone to guide the ball to golden exit",
                        fontSize = 11.sp,
                        color = TextDim
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(AccentCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isSolved) "✓ Solved" else "60 FPS Tilt",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSolved) AccentEmerald else AccentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Maze Glass Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24.dp, elevation = 16.dp)
                    .padding(10.dp)
            ) {
                var physicsEngine by remember { mutableStateOf<MazePhysicsEngine?>(null) }
                var ballPos by remember { mutableStateOf(Offset(0f, 0f)) }

                LaunchedEffect(physicsEngine, tiltX, tiltY) {
                    while (!isSolved) {
                        physicsEngine?.let {
                            it.update(tiltX, tiltY)
                            ballPos = Offset(it.ball.x, it.ball.y)
                        }
                        delay(16)
                    }
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val cellWidth = canvasWidth / maze.cols
                    val cellHeight = canvasHeight / maze.rows

                    if (physicsEngine == null) {
                        physicsEngine = MazePhysicsEngine(
                            maze = maze,
                            cellWidth = cellWidth,
                            cellHeight = cellHeight
                        ) {
                            isSolved = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(200)
                            }
                        }
                    }

                    // Walls
                    val wallColor = Color(0xFF334155)
                    val strokeWidth = 6f

                    for (r in 0 until maze.rows) {
                        for (c in 0 until maze.cols) {
                            val cell = maze.cells[r][c]
                            val x1 = c * cellWidth
                            val y1 = r * cellHeight
                            val x2 = (c + 1) * cellWidth
                            val y2 = (r + 1) * cellHeight

                            if (cell.topWall) drawLine(wallColor, Offset(x1, y1), Offset(x2, y1), strokeWidth, StrokeCap.Round)
                            if (cell.rightWall) drawLine(wallColor, Offset(x2, y1), Offset(x2, y2), strokeWidth, StrokeCap.Round)
                            if (cell.bottomWall) drawLine(wallColor, Offset(x1, y2), Offset(x2, y2), strokeWidth, StrokeCap.Round)
                            if (cell.leftWall) drawLine(wallColor, Offset(x1, y1), Offset(x1, y2), strokeWidth, StrokeCap.Round)
                        }
                    }

                    // Golden Cheese Goal with glow
                    val goalX = (maze.exitCol + 0.5f) * cellWidth
                    val goalY = (maze.exitRow + 0.5f) * cellHeight
                    drawCircle(
                        color = Color(0xFFFBBF24).copy(alpha = 0.3f),
                        radius = cellWidth * 0.42f,
                        center = Offset(goalX, goalY)
                    )
                    drawCircle(
                        color = Color(0xFFFBBF24),
                        radius = cellWidth * 0.28f,
                        center = Offset(goalX, goalY)
                    )

                    // Mouse Ball with glow
                    drawCircle(
                        color = AccentCyan.copy(alpha = 0.35f),
                        radius = 26f,
                        center = ballPos
                    )
                    drawCircle(
                        color = AccentCyanGlow,
                        radius = 18f,
                        center = ballPos
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Completion Banner
            AnimatedVisibility(
                visible = isSolved,
                enter = fadeIn() + scaleIn()
            ) {
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
                        Text(
                            text = "Labyrinth Solved • Claim Break",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
