package com.pocketclaw.claw.bond

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Bond State: user-owned, model-agnostic relationship data.
 * Portable across devices and model swaps.
 */
@Entity(tableName = "bond_memories")
data class BondMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,      // pref, habit, rel, fact
    val key: String,
    val value: String,
    val confidence: Float = 0.5f,
    val source: String = "chat",  // chat, manual, notification
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "bond_growth")
data class BondGrowth(
    @PrimaryKey val id: String = "singleton",
    val stage: Int = 0,            // 0=egg, 1=hatchling, 2=juvenile, 3=adult, 4=elder
    val totalInteractions: Int = 0,
    val positiveInteractions: Int = 0,
    val streakDays: Int = 0,
    val lastInteractionDate: String = "",
    val xp: Int = 0,
    val traits: String = "",       // JSON array of discovered traits
)

@Dao
interface BondMemoryDao {
    @Query("SELECT * FROM bond_memories ORDER BY updatedAt DESC")
    fun allMemories(): Flow<List<BondMemory>>

    @Query("SELECT * FROM bond_memories ORDER BY updatedAt DESC")
    suspend fun getAllMemories(): List<BondMemory>

    @Query("SELECT * FROM bond_memories WHERE type = :type ORDER BY confidence DESC LIMIT :limit")
    suspend fun byType(type: String, limit: Int = 10): List<BondMemory>

    @Query("SELECT * FROM bond_memories WHERE `key` = :key AND type = :type LIMIT 1")
    suspend fun findByKey(key: String, type: String): BondMemory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: BondMemory)

    @Query("DELETE FROM bond_memories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM bond_memories")
    suspend fun count(): Int

    @Query("SELECT * FROM bond_memories ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<BondMemory>
}

@Dao
interface BondGrowthDao {
    @Query("SELECT * FROM bond_growth WHERE id = 'singleton'")
    suspend fun getGrowth(): BondGrowth?

    @Query("SELECT * FROM bond_growth WHERE id = 'singleton'")
    fun observeGrowth(): Flow<BondGrowth?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(growth: BondGrowth)
}
