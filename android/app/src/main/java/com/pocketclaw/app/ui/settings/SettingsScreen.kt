package com.pocketclaw.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.pocketclaw.app.ui.theme.*

@Composable
fun SettingsScreen(
    llmMode: String,
    llmReady: Boolean,
    onSwitchLlmMode: (String) -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    var sttEnabled by remember { mutableStateOf(Preferences.sttEnabled) }
    var ttsEnabled by remember { mutableStateOf(Preferences.ttsEnabled) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
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

        // Theme
        SettingsSection("Appearance", colors) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Palette, null, tint = CrabOrange, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Theme", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
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

        // LLM Mode
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
                    selected = llmMode == "local",
                    onClick = { onSwitchLlmMode("local") },
                    label = { Text("Local (Qwen3)") },
                    leadingIcon = {
                        if (llmMode == "local") Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrabOrangeDark, selectedLabelColor = DarkTextPrimary,
                    ),
                )
                FilterChip(
                    selected = llmMode == "api",
                    onClick = { onSwitchLlmMode("api") },
                    label = { Text("Cloud API") },
                    leadingIcon = {
                        if (llmMode == "api") Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrabOrangeDark, selectedLabelColor = DarkTextPrimary,
                    ),
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
                subtitle = "Whisper (on-device)", checked = sttEnabled,
                onCheckedChange = { sttEnabled = it; Preferences.sttEnabled = it },
                colors = colors,
            )
            HorizontalDivider(color = colors.surface, thickness = 1.dp)
            SettingsToggle(
                icon = Icons.AutoMirrored.Filled.VolumeUp, title = "Text-to-Speech",
                subtitle = "Kokoro (on-device)", checked = ttsEnabled,
                onCheckedChange = { ttsEnabled = it; Preferences.ttsEnabled = it },
                colors = colors,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("Notifications", colors) {
            SettingsItem(
                icon = Icons.Default.Notifications, title = "Notification Access",
                subtitle = "Manage which apps to monitor",
                onClick = onOpenNotificationSettings, colors = colors,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("About", colors) {
            SettingsItem(
                icon = Icons.Default.Info, title = "PocketClaw",
                subtitle = "v0.3.0 — Your pocket butler, no server needed",
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
}

@Composable
private fun SettingsSection(
    title: String,
    colors: AppColors,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.card),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector, title: String, subtitle: String,
    onClick: () -> Unit, colors: AppColors,
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
        onDismissRequest = onDismiss,
        containerColor = colors.card,
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
