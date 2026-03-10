package com.crabagent.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.crabagent.app.CrabApplication
import com.crabagent.app.MainActivity
import com.crabagent.app.R
import com.crabagent.app.cloud.CloudResponse

/**
 * Executes actions returned by the Cloud Brain.
 *
 * Translates abstract action types (notify, remind, draft_reply, silent)
 * into concrete Android operations.
 */
object ActionExecutor {

    private const val TAG = "CrabAction"
    private var notificationId = 1000

    fun execute(context: Context, response: CloudResponse) {
        for (action in response.actions) {
            when (action.type) {
                "notify" -> handleNotify(context, action.priority, action.title, action.body, action.suggestions)
                "remind" -> handleRemind(context, action.at, action.body)
                "draft_reply" -> handleDraftReply(context, action.targetApp, action.text)
                "silent" -> Log.d(TAG, "Silent action — no interruption")
                else -> Log.w(TAG, "Unknown action type: ${action.type}")
            }
        }

        // Log stats for the signal export
        Log.i(TAG, "🦀 Skill: ${response.meta.skillUsed} | " +
            "Tokens: ${response.meta.tokensConsumed} | " +
            "Saved: ${response.meta.tokensSaved} | " +
            "Latency: ${response.meta.latencyMs}ms")
    }

    private fun handleNotify(
        context: Context,
        priority: String?,
        title: String?,
        body: String?,
        suggestions: List<String>?
    ) {
        val importance = when (priority) {
            "urgent" -> NotificationCompat.PRIORITY_MAX
            "high" -> NotificationCompat.PRIORITY_HIGH
            "normal" -> NotificationCompat.PRIORITY_DEFAULT
            "low" -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CrabApplication.CHANNEL_TRIAGE)
            .setSmallIcon(R.drawable.ic_crab)
            .setContentTitle(title ?: "🦀 CrabAgent")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(importance)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)

        // Add quick-reply suggestion buttons
        suggestions?.take(3)?.forEachIndexed { index, suggestion ->
            val replyIntent = PendingIntent.getBroadcast(
                context, index,
                Intent("com.crabagent.QUICK_REPLY").putExtra("text", suggestion),
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, suggestion, replyIntent)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId++, builder.build())

        Log.i(TAG, "Notified: $title | $body")
    }

    private fun handleRemind(context: Context, at: String?, body: String?) {
        // TODO: Schedule AlarmManager or WorkManager for future reminder
        Log.i(TAG, "Reminder scheduled: $body at $at")
    }

    private fun handleDraftReply(context: Context, targetApp: String?, text: String?) {
        // TODO: Use Accessibility or Intent to pre-fill reply in target app
        Log.i(TAG, "Draft reply for $targetApp: $text")
    }
}
