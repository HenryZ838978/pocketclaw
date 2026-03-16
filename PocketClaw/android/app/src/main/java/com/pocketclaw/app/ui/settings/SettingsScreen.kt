package com.pocketclaw.app.ui.settings

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketclaw.app.data.Preferences
import com.pocketclaw.app.data.ScheduledTask
import com.pocketclaw.app.ui.theme.*
import com.pocketclaw.claw.security.AuditLog
import com.pocketclaw.app.service.ScreenControlService
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    llmMode: String,
    llmReady: Boolean,
    onSwitchLlmMode: (String) -> Unit,
    scheduledTasks: List<ScheduledTask>,
    onDeleteTask: (ScheduledTask) -> Unit,
    onToggleTask: (ScheduledTask) -> Unit,
    onCreateTask: (String, Int, Int, Boolean) -> Unit,
    auditEntries: List<AuditLog.Entry>,
    onOpenNotificationSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestStoragePermission: () -> Unit,
) {
    var sttEnabled by remember { mutableStateOf(Preferences.sttEnabled) }
    var ttsEnabled by remember { mutableStateOf(Preferences.ttsEnabled) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showMessagingDialog by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showAuditLog by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(Preferences.themeMode) }
    val colors = AppColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = CrabOrange,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 20.dp),
        )

        SettingsSection("Appearance", colors) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Palette, null, tint = CrabOrange, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Theme", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for ((mode, label) in listOf("dark" to "Dark", "light" to "Light", "system" to "Auto")) {
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = {
                            themeMode = mode
                            Preferences.themeMode = mode
                        },
                        label = { Text(label) },
                        leadingIcon = {
                            if (themeMode == mode) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrabOrangeDark,
                            selectedLabelColor = DarkTextPrimary,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("AI Brain", colors) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Memory, null, tint = CrabOrange, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("LLM Mode", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(
                        if (llmReady) "Ready" else "Not ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (llmReady) AccentGreen else AccentRed,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = llmMode == "local", onClick = { onSwitchLlmMode("local") },
                    label = { Text("Local (Qwen3)") },
                    leadingIcon = { if (llmMode == "local") Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CrabOrangeDark, selectedLabelColor = DarkTextPrimary),
                )
                FilterChip(
                    selected = llmMode == "api", onClick = { onSwitchLlmMode("api") },
                    label = { Text("Cloud API") },
                    leadingIcon = { if (llmMode == "api") Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CrabOrangeDark, selectedLabelColor = DarkTextPrimary),
                )
            }
            if (llmMode == "api") {
                HorizontalDivider(color = colors.surface, thickness = 1.dp)
                SettingsItem(
                    icon = Icons.Default.Key, title = "API Key",
                    subtitle = if (Preferences.dashScopeApiKey.isNotBlank()) "••••${Preferences.dashScopeApiKey.takeLast(6)}" else "Not set",
                    onClick = { showApiKeyDialog = true }, colors = colors,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("Voice", colors) {
            SettingsToggle(
                icon = Icons.Default.Mic, title = "Speech-to-Text",
                subtitle = "Android Speech (on-device)", checked = sttEnabled,
                onCheckedChange = { sttEnabled = it; Preferences.sttEnabled = it },
                colors = colors,
            )
            HorizontalDivider(color = colors.surface, thickness = 1.dp)
            SettingsToggle(
                icon = Icons.AutoMirrored.Filled.VolumeUp, title = "Text-to-Speech",
                subtitle = "Kokoro / System TTS", checked = ttsEnabled,
                onCheckedChange = { ttsEnabled = it; Preferences.ttsEnabled = it },
                colors = colors,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("Permissions", colors) {
            val accessibilityOn = ScreenControlService.isEnabled
            SettingsItem(
                icon = Icons.Default.Accessibility,
                title = "Screen Control",
                subtitle = if (accessibilityOn) "Enabled" else "Tap to enable in system settings",
                onClick = onOpenAccessibilitySettings,
                colors = colors,
            )
            HorizontalDivider(color = colors.surface, thickness = 1.dp)
            val storageGranted = Environment.isExternalStorageManager()
            SettingsItem(
                icon = Icons.Default.Folder,
                title = "File Access",
                subtitle = if (storageGranted) "Full access granted" else "Tap to grant storage access",
                onClick = onRequestStoragePermission,
                colors = colors,
            )
            HorizontalDivider(color = colors.surface, thickness = 1.dp)
            SettingsItem(
                icon = Icons.Default.Notifications, title = "Notification Access",
                subtitle = "Manage which apps to monitor",
                onClick = onOpenNotificationSettings, colors = colors,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("Reminders (${scheduledTasks.size})", colors) {
            if (scheduledTasks.isEmpty()) {
                Text(
                    "No reminders. Ask PocketClaw or create one below.",
                    color = colors.textMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                for (task in scheduledTasks.take(5)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "%02d:%02d".format(task.hour, task.minute),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (task.enabled) CrabOrange else colors.textMuted,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(task.name, modifier = Modifier.weight(1f), color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = task.enabled, onCheckedChange = { onToggleTask(task) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CrabOrange, checkedTrackColor = CrabOrangeDark),
                            modifier = Modifier.height(24.dp),
                        )
                        IconButton(onClick = { onDeleteTask(task) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            HorizontalDivider(color = colors.surface, thickness = 1.dp)
            Surface(onClick = { showCreateTaskDialog = true }, color = colors.card) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, null, tint = CrabOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Reminder", color = CrabOrange, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("Messaging", colors) {
            SettingsItem(
                icon = Icons.Default.Send,
                title = "Bot Tokens",
                subtitle = "Configure Telegram / Discord / Feishu / Slack",
                onClick = { showMessagingDialog = true },
                colors = colors,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("Security", colors) {
            SettingsItem(
                icon = Icons.Default.Security,
                title = "Audit Log",
                subtitle = "${auditEntries.size} recent tool executions",
                onClick = { showAuditLog = !showAuditLog },
                colors = colors,
            )
            if (showAuditLog && auditEntries.isNotEmpty()) {
                HorizontalDivider(color = colors.surface, thickness = 1.dp)
                val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                for (entry in auditEntries.take(15)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            fmt.format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${if (entry.success) "✓" else "✗"} ${entry.toolId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (entry.success) AccentGreen else AccentRed,
                            )
                            if (entry.args.isNotBlank()) {
                                Text(
                                    entry.args.take(60),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("About", colors) {
            SettingsItem(
                icon = Icons.Default.Info, title = "PocketClaw",
                subtitle = "v0.4.0 — Your pocket butler, no server needed",
                onClick = {}, colors = colors,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            onDismiss = { showApiKeyDialog = false },
            onSave = { Preferences.dashScopeApiKey = it; showApiKeyDialog = false },
        )
    }

    if (showMessagingDialog) {
        MessagingTokensDialog(onDismiss = { showMessagingDialog = false })
    }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateTaskDialog = false },
            onCreate = { name, hour, minute, repeat ->
                onCreateTask(name, hour, minute, repeat)
                showCreateTaskDialog = false
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String, colors: AppColors, content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title.uppercase(), style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.card),
            shape = RoundedCornerShape(16.dp),
        ) { Column(content = content) }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, colors: AppColors,
) {
    Surface(onClick = onClick, color = colors.card) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = CrabOrange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
            }
            Icon(Icons.Default.ChevronRight, null, tint = colors.textMuted)
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit, colors: AppColors,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = CrabOrange, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CrabOrange, checkedTrackColor = CrabOrangeDark),
        )
    }
}

@Composable
private fun ApiKeyDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf(Preferences.dashScopeApiKey) }
    val colors = AppColors
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = colors.card,
        title = { Text("DashScope API Key", color = colors.textPrimary) },
        text = {
            OutlinedTextField(
                value = key, onValueChange = { key = it },
                label = { Text("API Key") }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrabOrange, unfocusedBorderColor = colors.textMuted,
                    cursorColor = CrabOrange, focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedLabelColor = CrabOrange, unfocusedLabelColor = colors.textSecondary,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(key) }) { Text("Save", color = CrabOrange) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) } },
    )
}

@Composable
private fun MessagingTokensDialog(onDismiss: () -> Unit) {
    var tgToken by remember { mutableStateOf(Preferences.telegramToken) }
    var discordToken by remember { mutableStateOf(Preferences.discordToken) }
    val colors = AppColors

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = colors.card,
        title = { Text("Messaging Tokens", color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tgToken, onValueChange = { tgToken = it },
                    label = { Text("Telegram Bot Token") }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrabOrange, unfocusedBorderColor = colors.textMuted,
                        cursorColor = CrabOrange, focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                    ),
                )
                OutlinedTextField(
                    value = discordToken, onValueChange = { discordToken = it },
                    label = { Text("Discord Bot Token") }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrabOrange, unfocusedBorderColor = colors.textMuted,
                        cursorColor = CrabOrange, focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Preferences.telegramToken = tgToken
                Preferences.discordToken = discordToken
                onDismiss()
            }) { Text("Save", color = CrabOrange) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) } },
    )
}

@Composable
private fun CreateTaskDialog(onDismiss: () -> Unit, onCreate: (String, Int, Int, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("9") }
    var minute by remember { mutableStateOf("0") }
    var repeat by remember { mutableStateOf(true) }
    val colors = AppColors

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = colors.card,
        title = { Text("New Reminder", color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("What to remind?") }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrabOrange, unfocusedBorderColor = colors.textMuted,
                        cursorColor = CrabOrange, focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour, onValueChange = { hour = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Hour") }, singleLine = true, modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrabOrange, unfocusedBorderColor = colors.textMuted,
                            cursorColor = CrabOrange, focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                        ),
                    )
                    OutlinedTextField(
                        value = minute, onValueChange = { minute = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Min") }, singleLine = true, modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrabOrange, unfocusedBorderColor = colors.textMuted,
                            cursorColor = CrabOrange, focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                        ),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = repeat, onCheckedChange = { repeat = it }, colors = CheckboxDefaults.colors(checkedColor = CrabOrange))
                    Text("Repeat daily", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name, hour.toIntOrNull() ?: 9, minute.toIntOrNull() ?: 0, repeat) },
                enabled = name.isNotBlank(),
            ) { Text("Create", color = CrabOrange) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) } },
    )
}
