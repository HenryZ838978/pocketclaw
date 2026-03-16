package com.pocketclaw.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.llmhub.llmhub.data.LlmHubDatabase
import com.llmhub.llmhub.inference.InferenceService
import com.llmhub.llmhub.inference.UnifiedInferenceService
import com.llmhub.llmhub.repository.ChatRepository
import com.pocketclaw.app.data.Preferences
import com.pocketclaw.app.messaging.*
import com.pocketclaw.app.service.TaskWorker
import com.pocketclaw.claw.bond.BondEngine
import com.pocketclaw.claw.prompt.ContextBudget
import com.pocketclaw.claw.prompt.PromptAssembler
import com.pocketclaw.claw.prompt.UserProfile
import com.pocketclaw.claw.security.AuditLog
import com.pocketclaw.claw.security.PermissionGuard
import com.pocketclaw.claw.tools.*
import com.pocketclaw.app.api.DashScopeProvider
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class PocketClawApplication : Application() {

    companion object {
        const val CHANNEL_AGENT = "pocketclaw_agent"
        const val CHANNEL_TRIAGE = "pocketclaw_triage"
        const val CHANNEL_REMINDERS = "pocketclaw_reminders"

        lateinit var instance: PocketClawApplication
            private set
    }

    val database by lazy { LlmHubDatabase.getDatabase(this) }
    val chatRepository by lazy {
        ChatRepository(database.chatDao(), database.messageDao(), database.creatorDao())
    }
    val bondEngine by lazy {
        BondEngine(database.bondMemoryDao(), database.bondGrowthDao())
    }
    val permissionGuard by lazy { PermissionGuard() }
    val auditLog by lazy { AuditLog() }
    val toolExecutor by lazy { ToolExecutor(this, permissionGuard, auditLog) }

    private var _inferenceService: InferenceService? = null
    val inferenceService: InferenceService
        get() {
            if (_inferenceService == null) {
                _inferenceService = UnifiedInferenceService(this)
            }
            return _inferenceService!!
        }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Preferences.init(this)
        createNotificationChannels()
        registerTools()
        registerMessagingChannels()
        scheduleTaskChecker()
    }

    private fun registerTools() {
        ToolRegistry.register(FileReadTool())
        ToolRegistry.register(FileWriteTool())
        ToolRegistry.register(FileListTool())
        ToolRegistry.register(FileDeleteTool())
        ToolRegistry.register(ScreenReadTool())
        ToolRegistry.register(ScreenTapTool())
        ToolRegistry.register(ScreenSwipeTool())
        ToolRegistry.register(ScreenInputTool())
        ToolRegistry.register(ScreenBackTool())
        ToolRegistry.register(ScheduleCreateTool())
        ToolRegistry.register(ScheduleListTool())
        ToolRegistry.register(TelegramSendTool())
        ToolRegistry.register(ClipboardReadTool())
        ToolRegistry.register(ClipboardWriteTool())
        ToolRegistry.register(AppLaunchTool())
        ToolRegistry.register(WebSearchTool())
    }

    private fun registerMessagingChannels() {
        val handler: suspend (IncomingMessage) -> String? = { msg -> handleIncomingMessage(msg) }
        val tg = TelegramBotService().also { it.setIncomingHandler(handler) }
        val dc = DiscordBotService().also { it.setIncomingHandler(handler) }
        val fs = FeishuBotService().also { it.setIncomingHandler(handler) }
        val sl = SlackBotService().also { it.setIncomingHandler(handler) }
        MessageBridge.register(tg)
        MessageBridge.register(dc)
        MessageBridge.register(fs)
        MessageBridge.register(sl)
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun handleIncomingMessage(msg: IncomingMessage): String? {
        return try {
            val memories = bondEngine.getMemoriesForPrompt()
            val budget = ContextBudget.forMode(Preferences.llmMode)
            val assembled = PromptAssembler.assemble(
                memories = memories,
                skills = emptyList(),
                recentHistory = emptyList(),
                userMessage = msg.text,
                toolResults = emptyList(),
                budget = budget,
            )

            val sb = StringBuilder()
            DashScopeProvider.generateStream(assembled).collect { chunk ->
                sb.append(chunk)
            }
            val raw = sb.toString()
            bondEngine.processResponse(raw)
        } catch (e: Exception) {
            "Sorry, I couldn't process that: ${e.message}"
        }
    }

    private fun scheduleTaskChecker() {
        val request = PeriodicWorkRequestBuilder<TaskWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TaskWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
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

        val remindersChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled task reminders"
        }

        manager.createNotificationChannel(agentChannel)
        manager.createNotificationChannel(triageChannel)
        manager.createNotificationChannel(remindersChannel)
    }
}
