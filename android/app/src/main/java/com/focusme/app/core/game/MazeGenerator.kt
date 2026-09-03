package com.focusme.app.core.game

import java.util.Stack
import kotlin.random.Random

data class MazeCell(
    val row: Int,
    val col: Int,
    var topWall: Boolean = true,
    var rightWall: Boolean = true,
    var bottomWall: Boolean = true,
    var leftWall: Boolean = true,
    var visited: Boolean = false
)

data class MazeGrid(
    val rows: Int,
    val cols: Int,
    val cells: Array<Array<MazeCell>>,
    val startRow: Int = 0,
    val startCol: Int = 0,
    val exitRow: Int,
    val exitCol: Int
)

/**
 * Procedural 2D Maze Generator using the Recursive Backtracker algorithm.
 * Guarantees a solvable labyrinth with rich pathways and dead ends.
 */
object MazeGenerator {

    fun generate(rows: Int = 10, cols: Int = 8): MazeGrid {
        val grid = Array(rows) { r ->
            Array(cols) { c ->
                MazeCell(r, c)
            }
        }

        val stack = Stack<MazeCell>()
        var current = grid[0][0]
        current.visited = true
        var visitedCount = 1
        val totalCells = rows * cols

        while (visitedCount < totalCells) {
            val unvisitedNeighbors = getUnvisitedNeighbors(current, grid, rows, cols)

            if (unvisitedNeighbors.isNotEmpty()) {
                val next = unvisitedNeighbors[Random.nextInt(unvisitedNeighbors.size)]
                stack.push(current)

                removeWalls(current, next)
                next.visited = true
                visitedCount++
                current = next
            } else if (stack.isNotEmpty()) {
                current = stack.pop()
            }
        }

        return MazeGrid(
            rows = rows,
            cols = cols,
            cells = grid,
            startRow = 0,
            startCol = 0,
            exitRow = rows - 1,
            exitCol = cols - 1
        )
    }

    private fun getUnvisitedNeighbors(cell: MazeCell, grid: Array<Array<MazeCell>>, rows: Int, cols: Int): List<MazeCell> {
        val neighbors = mutableListOf<MazeCell>()
        val r = cell.row
        val c = cell.col

        if (r > 0 && !grid[r - 1][c].visited) neighbors.add(grid[r - 1][c]) // Top
        if (c < cols - 1 && !grid[r][c + 1].visited) neighbors.add(grid[r][c + 1]) // Right
        if (r < rows - 1 && !grid[r + 1][c].visited) neighbors.add(grid[r + 1][c]) // Bottom
        if (c > 0 && !grid[r][c - 1].visited) neighbors.add(grid[r][c - 1]) // Left

        return neighbors
    }

    private fun removeWalls(current: MazeCell, next: MazeCell) {
        val dr = next.row - current.row
        val dc = next.col - current.col

        if (dr == -1) { // Next is Top
            current.topWall = false
            next.bottomWall = false
        } else if (dr == 1) { // Next is Bottom
            current.bottomWall = false
            next.topWall = false
        } else if (dc == 1) { // Next is Right
            current.rightWall = false
            next.leftWall = false
        } else if (dc == -1) { // Next is Left
            current.leftWall = false
            next.rightWall = false
        }
    }
}
