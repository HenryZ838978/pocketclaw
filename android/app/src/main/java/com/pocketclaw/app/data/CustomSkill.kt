package com.pocketclaw.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Entity(tableName = "custom_skills")
data class CustomSkill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: String,
    val name: String,
    val description: String,
    val keywords: String,
    val createdAt: Instant = Instant.now(),
    val enabled: Boolean = true,
)

@Dao
interface CustomSkillDao {

    @Query("SELECT * FROM custom_skills WHERE enabled = 1 ORDER BY createdAt DESC")
    fun getAllEnabledFlow(): Flow<List<CustomSkill>>

    @Query("SELECT * FROM custom_skills WHERE enabled = 1 ORDER BY createdAt DESC")
    suspend fun getAllEnabled(): List<CustomSkill>

    @Query("SELECT * FROM custom_skills ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<CustomSkill>>

    @Query("SELECT * FROM custom_skills WHERE skillId = :skillId LIMIT 1")
    suspend fun findBySkillId(skillId: String): CustomSkill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(skill: CustomSkill): Long

    @Update
    suspend fun update(skill: CustomSkill)

    @Delete
    suspend fun delete(skill: CustomSkill)
}
