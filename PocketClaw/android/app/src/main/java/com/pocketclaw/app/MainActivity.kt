package com.pocketclaw.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.pocketclaw.app.ui.MainViewModel
import com.pocketclaw.app.ui.chat.ChatScreen
import com.pocketclaw.app.ui.components.ToolConfirmDialog
import com.pocketclaw.app.ui.memory.MemoryScreen
import com.pocketclaw.app.ui.settings.SettingsScreen
import com.pocketclaw.app.ui.skills.SkillsScreen
import com.pocketclaw.app.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val audioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioPermission.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            PocketClawTheme {
                PocketClawApp(viewModel = viewModel)
            }
        }
    }

    @Composable
    private fun PocketClawApp(viewModel: MainViewModel) {
        val messages by viewModel.messages.collectAsState()
        val isRecording by viewModel.isRecording.collectAsState()
        val isProcessing by viewModel.isProcessing.collectAsState()
        val growthStage by viewModel.growthStage.collectAsState()
        val interactionCount by viewModel.interactionCount.collectAsState()
        val skillUsage by viewModel.skillUsage.collectAsState()
        val customSkills by viewModel.customSkills.collectAsState()
        val scheduledTasks by viewModel.scheduledTasks.collectAsState()
        val isModelLoaded by viewModel.isModelLoaded.collectAsState()
        val llmMode by viewModel.llmMode.collectAsState()
        val llmReady by viewModel.llmReady.collectAsState()
        val inputText by viewModel.inputText.collectAsState()

        var selectedTab by remember { mutableIntStateOf(0) }
        val colors = AppColors

        Scaffold(
            containerColor = colors.surface,
            bottomBar = {
                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> ChatScreen(
                        messages = messages,
                        isRecording = isRecording,
                        isProcessing = isProcessing,
                        growthStage = growthStage,
                        interactionCount = interactionCount,
                        inputText = inputText,
                        onInputTextChange = viewModel::updateInputText,
                        onSendText = viewModel::sendTextMessage,
                        onStartRecording = viewModel::startRecording,
                        onStopRecording = viewModel::stopRecording,
                        onNewTopic = viewModel::newTopic,
                        onDeleteMessage = viewModel::deleteMessage,
                        onSpeakMessage = viewModel::speakMessage,
                    )
                    1 -> MemoryScreen(
                        memories = viewModel.memories.collectAsState().value,
                        onDeleteMemory = viewModel::deleteMemory,
                    )
                    2 -> SkillsScreen(
                        skillUsage = skillUsage,
                        customSkills = customSkills,
                        onDeleteSkill = viewModel::deleteSkill,
                        onToggleSkill = viewModel::toggleSkill,
                    )
                    3 -> SettingsScreen(
                        llmMode = llmMode,
                        llmReady = llmReady,
                        onSwitchLlmMode = viewModel::switchLlmMode,
                        scheduledTasks = scheduledTasks,
                        onDeleteTask = viewModel::deleteTask,
                        onToggleTask = viewModel::toggleTask,
                        onCreateTask = viewModel::createTask,
                        auditEntries = viewModel.auditEntries,
                        onOpenNotificationSettings = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onRequestStoragePermission = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                            }
                        },
                    )
                }
            }
            ToolConfirmDialog()
        }
    }
}

private enum class NavTab(
    val label: String,
    val icon: ImageVector,
) {
    CHAT("Chat", Icons.AutoMirrored.Filled.Chat),
    MEMORY("Memory", Icons.Default.Psychology),
    SKILLS("Skills", Icons.Default.AutoAwesome),
    SETTINGS("Settings", Icons.Default.Settings),
}

@Composable
private fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val colors = AppColors
    NavigationBar(
        containerColor = colors.card,
        contentColor = colors.textPrimary,
    ) {
        NavTab.entries.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CrabOrange,
                    selectedTextColor = CrabOrange,
                    unselectedIconColor = colors.textMuted,
                    unselectedTextColor = colors.textMuted,
                    indicatorColor = colors.elevated,
                ),
            )
        }
    }
}
