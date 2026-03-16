package com.pocketclaw.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val key: String,
    val value: String,
    val confidence: Float = 0.5f,
    val source: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<Memory>>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Memory>

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Int

    @Query("SELECT * FROM memories WHERE type = :type ORDER BY updatedAt DESC")
    suspend fun getByType(type: String): List<Memory>

    @Query("SELECT * FROM memories WHERE `key` = :key LIMIT 1")
    suspend fun findByKey(key: String): Memory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: Memory): Long

    @Update
    suspend fun update(memory: Memory)

    @Delete
    suspend fun delete(memory: Memory)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)
}

class InstantConverter {
    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter
    fun toInstant(epochMilli: Long): Instant = Instant.ofEpochMilli(epochMilli)
}

@Database(
    entities = [Memory::class, CustomSkill::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(InstantConverter::class)
abstract class PocketClawDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun customSkillDao(): CustomSkillDao
}
