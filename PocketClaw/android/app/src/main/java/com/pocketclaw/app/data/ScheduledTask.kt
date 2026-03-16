package com.pocketclaw.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scheduled_tasks")
data class ScheduledTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val action: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val repeating: Boolean = false,
    val lastRun: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface ScheduledTaskDao {
    @Query("SELECT * FROM scheduled_tasks ORDER BY hour, minute")
    fun allTasks(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks WHERE enabled = 1 ORDER BY hour, minute")
    suspend fun enabledTasks(): List<ScheduledTask>

    @Upsert
    suspend fun upsert(task: ScheduledTask): Long

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE scheduled_tasks SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE scheduled_tasks SET lastRun = :timestamp WHERE id = :id")
    suspend fun updateLastRun(id: Long, timestamp: Long)
}
