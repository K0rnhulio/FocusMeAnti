package com.focusme.app.core.game

import kotlin.math.sqrt

data class BallState(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 22f
)

/**
 * 2D Particle Simulation with Gyroscope tilt gravity and Maze Wall collision responses.
 */
class MazePhysicsEngine(
    private val maze: MazeGrid,
    private val cellWidth: Float,
    private val cellHeight: Float,
    private val onGoalReached: () -> Unit
) {
    val ball = BallState(
        x = cellWidth / 2f,
        y = cellHeight / 2f
    )

    private val friction = 0.94f
    private val tiltSensitivity = 1.4f
    private var isGoalReached = false

    fun update(tiltX: Float, tiltY: Float) {
        if (isGoalReached) return

        // 1. Apply tilt gravity
        ball.vx += tiltX * tiltSensitivity
        ball.vy += tiltY * tiltSensitivity

        // 2. Apply friction
        ball.vx *= friction
        ball.vy *= friction

        // 3. Move X with wall collisions
        var nextX = ball.x + ball.vx
        val currentCellCol = (ball.x / cellWidth).toInt().coerceIn(0, maze.cols - 1)
        val currentCellRow = (ball.y / cellHeight).toInt().coerceIn(0, maze.rows - 1)
        val cell = maze.cells[currentCellRow][currentCellCol]

        // Left wall collision
        if (cell.leftWall && (nextX - ball.radius) < currentCellCol * cellWidth) {
            nextX = currentCellCol * cellWidth + ball.radius
            ball.vx = 0f
        }
        // Right wall collision
        if (cell.rightWall && (nextX + ball.radius) > (currentCellCol + 1) * cellWidth) {
            nextX = (currentCellCol + 1) * cellWidth - ball.radius
            ball.vx = 0f
        }
        ball.x = nextX

        // 4. Move Y with wall collisions
        var nextY = ball.y + ball.vy
        val updatedCellCol = (ball.x / cellWidth).toInt().coerceIn(0, maze.cols - 1)
        val updatedCellRow = (ball.y / cellHeight).toInt().coerceIn(0, maze.rows - 1)
        val updatedCell = maze.cells[updatedCellRow][updatedCellCol]

        // Top wall collision
        if (updatedCell.topWall && (nextY - ball.radius) < updatedCellRow * cellHeight) {
            nextY = updatedCellRow * cellHeight + ball.radius
            ball.vy = 0f
        }
        // Bottom wall collision
        if (updatedCell.bottomWall && (nextY + ball.radius) > (updatedCellRow + 1) * cellHeight) {
            nextY = (updatedCellRow + 1) * cellHeight - ball.radius
            ball.vy = 0f
        }
        ball.y = nextY

        // 5. Check Cheese Goal
        val goalCenterX = (maze.exitCol + 0.5f) * cellWidth
        val goalCenterY = (maze.exitRow + 0.5f) * cellHeight
        val dx = ball.x - goalCenterX
        val dy = ball.y - goalCenterY
        val distToGoal = sqrt((dx * dx + dy * dy).toDouble())

        if (distToGoal < cellWidth * 0.45f) {
            isGoalReached = true
            onGoalReached()
        }
    }
}
