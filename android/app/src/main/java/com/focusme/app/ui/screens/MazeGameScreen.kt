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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusme.app.core.game.MazeGenerator
import com.focusme.app.core.game.MazePhysicsEngine
import com.focusme.app.core.sensors.TiltSensorManager
import com.focusme.app.ui.theme.AccentCyan
import com.focusme.app.ui.theme.AccentEmerald
import com.focusme.app.ui.theme.AccentIndigo
import com.focusme.app.ui.theme.BgDark
import com.focusme.app.ui.theme.CardDark
import com.focusme.app.ui.theme.TextDim
import com.focusme.app.ui.theme.TextMain
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

    // Tilt Sensor Listener
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
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "🐭 Tilt Labyrinth Challenge",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Text(
                text = "Physically tilt your phone to guide the mouse to the cheese!",
                fontSize = 13.sp,
                color = TextDim,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Maze Canvas Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(CardDark, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                var physicsEngine by remember { mutableStateOf<MazePhysicsEngine?>(null) }
                var ballPos by remember { mutableStateOf(Offset(0f, 0f)) }

                LaunchedEffect(physicsEngine, tiltX, tiltY) {
                    while (!isSolved) {
                        physicsEngine?.let {
                            it.update(tiltX, tiltY)
                            ballPos = Offset(it.ball.x, it.ball.y)
                        }
                        delay(16) // ~60 FPS
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

                    // Draw Walls
                    val wallColor = Color(0xFF334155)
                    val strokeWidth = 5f

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

                    // Draw Golden Cheese Goal
                    val goalX = (maze.exitCol + 0.5f) * cellWidth
                    val goalY = (maze.exitRow + 0.5f) * cellHeight
                    drawCircle(
                        color = Color(0xFFFBBF24),
                        radius = cellWidth * 0.32f,
                        center = Offset(goalX, goalY)
                    )

                    // Draw Mouse Ball
                    drawCircle(
                        color = AccentCyan,
                        radius = 18f,
                        center = ballPos
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Completion Banner
            AnimatedVisibility(
                visible = isSolved,
                enter = fadeIn() + scaleIn()
            ) {
                Button(
                    onClick = onCompleted,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Text(" Challenge Completed • Unlock", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
