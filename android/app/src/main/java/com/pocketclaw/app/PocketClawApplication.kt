package com.pocketclaw.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketclaw.app.data.PocketClawDatabase
import com.pocketclaw.app.data.Preferences

class PocketClawApplication : Application() {

    lateinit var database: PocketClawDatabase
        private set

    companion object {
        const val CHANNEL_AGENT = "pocketclaw_agent"
        const val CHANNEL_TRIAGE = "pocketclaw_triage"

        lateinit var instance: PocketClawApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Preferences.init(this)

        val migration1to2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_skills (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        skillId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        keywords TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
            }
        }

        database = Room.databaseBuilder(
            this,
            PocketClawDatabase::class.java,
            "pocketclaw.db"
        ).addMigrations(migration1to2).build()

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val agentChannel = NotificationChannel(
            CHANNEL_AGENT,
            "PocketClaw Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps PocketClaw running in background"
        }

        val triageChannel = NotificationChannel(
            CHANNEL_TRIAGE,
            "Message Triage",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important messages flagged by your pocket butler"
        }

        manager.createNotificationChannel(agentChannel)
        manager.createNotificationChannel(triageChannel)
    }
}
