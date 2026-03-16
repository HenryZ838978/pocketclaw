package com.llmhub.llmhub.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.pocketclaw.claw.bond.BondMemory
import com.pocketclaw.claw.bond.BondGrowth
import com.pocketclaw.claw.bond.BondMemoryDao
import com.pocketclaw.claw.bond.BondGrowthDao
import com.pocketclaw.claw.skills.CustomSkill
import com.pocketclaw.claw.skills.CustomSkillDao
import com.pocketclaw.app.data.ScheduledTask
import com.pocketclaw.app.data.ScheduledTaskDao

@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        MemoryDocument::class,
        MemoryChunkEmbedding::class,
        CreatorEntity::class,
        BondMemory::class,
        BondGrowth::class,
        CustomSkill::class,
        ScheduledTask::class,
    ],
    version = 7,
    exportSchema = false
)
abstract class LlmHubDatabase : RoomDatabase() {
    
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun creatorDao(): CreatorDao
    abstract fun bondMemoryDao(): BondMemoryDao
    abstract fun bondGrowthDao(): BondGrowthDao
    abstract fun customSkillDao(): CustomSkillDao
    abstract fun scheduledTaskDao(): ScheduledTaskDao
    
    companion object {
        @Volatile
        private var INSTANCE: LlmHubDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE MessageEntity ADD COLUMN attachmentFileName TEXT")
                database.execSQL("ALTER TABLE MessageEntity ADD COLUMN attachmentFileSize INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `memory_documents` (`id` TEXT NOT NULL, `fileName` TEXT NOT NULL, `content` TEXT NOT NULL, `metadata` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `status` TEXT NOT NULL, `chunkCount` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `memory_chunk_embeddings` (`id` TEXT NOT NULL, `docId` TEXT NOT NULL, `fileName` TEXT NOT NULL, `chunkIndex` INTEGER NOT NULL, `content` TEXT NOT NULL, `embedding` BLOB NOT NULL, `embeddingModel` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `creators` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `pctfPrompt` TEXT NOT NULL, `description` TEXT NOT NULL, `icon` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                database.execSQL("ALTER TABLE chats ADD COLUMN creatorId TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `scheduled_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `action` TEXT NOT NULL, `hour` INTEGER NOT NULL, `minute` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `repeating` INTEGER NOT NULL, `lastRun` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bond_memories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `key` TEXT NOT NULL, `value` TEXT NOT NULL, `confidence` REAL NOT NULL, `source` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bond_growth` (`id` TEXT NOT NULL, `stage` INTEGER NOT NULL, `totalInteractions` INTEGER NOT NULL, `positiveInteractions` INTEGER NOT NULL, `streakDays` INTEGER NOT NULL, `lastInteractionDate` TEXT NOT NULL, `xp` INTEGER NOT NULL, `traits` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_skills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `keywords` TEXT NOT NULL, `exampleQuery` TEXT NOT NULL, `exampleAnswer` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `usageCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
            }
        }
        
        fun getDatabase(context: Context): LlmHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LlmHubDatabase::class.java,
                    "llmhub_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 