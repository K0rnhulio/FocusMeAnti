package com.focusme.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusme.app.data.model.HourlyUsage
import com.focusme.app.data.model.ReflectionEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM hourly_usage WHERE hourKey = :hourKey LIMIT 1")
    suspend fun getUsage(hourKey: String): HourlyUsage?

    @Query("SELECT * FROM hourly_usage WHERE hourKey = :hourKey LIMIT 1")
    fun observeUsage(hourKey: String): Flow<HourlyUsage?>

    @Query("SELECT * FROM hourly_usage WHERE hourKey LIKE :datePrefix || '%'")
    fun observeTodayUsage(datePrefix: String): Flow<List<HourlyUsage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usage: HourlyUsage)
}

@Dao
interface ReflectionDao {
    @Query("SELECT * FROM reflections ORDER BY timestamp DESC LIMIT 50")
    fun observeAllReflections(): Flow<List<ReflectionEntry>>

    @Query("SELECT * FROM reflections WHERE hourKey LIKE :datePrefix || '%' ORDER BY timestamp DESC")
    fun observeTodayReflections(datePrefix: String): Flow<List<ReflectionEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ReflectionEntry): Long
}
