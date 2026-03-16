package com.pocketclaw.app.service

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pocketclaw.app.PocketClawApplication
import com.pocketclaw.app.R

/**
 * WorkManager worker that checks for due scheduled tasks and fires notifications.
 */
class TaskWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TaskWorker"
        const val WORK_NAME = "pocketclaw_task_checker"
    }

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? PocketClawApplication ?: return Result.failure()
            val dao = app.database.scheduledTaskDao()
            val tasks = dao.enabledTasks()

            val now = java.util.Calendar.getInstance()
            val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(java.util.Calendar.MINUTE)

            for (task in tasks) {
                if (task.hour == currentHour && Math.abs(task.minute - currentMinute) <= 7) {
                    val thirtyMinAgo = System.currentTimeMillis() - 30 * 60_000
                    if (task.lastRun < thirtyMinAgo) {
                        fireNotification(task.name, task.action)
                        dao.updateLastRun(task.id, System.currentTimeMillis())

                        if (!task.repeating) {
                            dao.setEnabled(task.id, false)
                        }
                    }
                }
            }

            Log.d(TAG, "Checked ${tasks.size} tasks at $currentHour:$currentMinute")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Task check failed: ${e.message}", e)
            Result.retry()
        }
    }

    private fun fireNotification(title: String, body: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, PocketClawApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PocketClaw: $title")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}
