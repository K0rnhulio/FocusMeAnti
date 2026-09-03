package com.focusme.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hourly_usage")
data class HourlyUsage(
    @PrimaryKey val hourKey: String, // Format: YYYY-MM-DD-HH
    val usedSeconds: Int,
    val hasReflected: Boolean = false,
    val hasCompletedToll: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "reflections")
data class ReflectionEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hourKey: String,
    val answer: String,
    val timestamp: Long = System.currentTimeMillis(),
    val targetApp: String = ""
)

enum class ChallengeType {
    TILT_MAZE,
    SHAKE_SURGE,
    STEP_WALK,
    PUSH_UPS,
    SQUATS
}
