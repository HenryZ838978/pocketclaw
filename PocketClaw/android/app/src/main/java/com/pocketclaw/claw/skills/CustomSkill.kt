package com.pocketclaw.claw.skills

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_skills")
data class CustomSkill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val keywords: String = "",     // comma-separated
    val exampleQuery: String = "",
    val exampleAnswer: String = "",
    val enabled: Boolean = true,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface CustomSkillDao {
    @Query("SELECT * FROM custom_skills ORDER BY usageCount DESC")
    fun allSkills(): Flow<List<CustomSkill>>

    @Query("SELECT * FROM custom_skills WHERE enabled = 1 ORDER BY usageCount DESC")
    suspend fun enabledSkills(): List<CustomSkill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(skill: CustomSkill): Long

    @Query("UPDATE custom_skills SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)

    @Query("UPDATE custom_skills SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM custom_skills WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM custom_skills")
    suspend fun count(): Int
}
