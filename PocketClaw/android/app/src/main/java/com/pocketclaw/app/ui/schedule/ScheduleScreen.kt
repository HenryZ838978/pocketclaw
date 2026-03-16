package com.pocketclaw.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketclaw.app.data.ScheduledTask
import com.pocketclaw.app.ui.theme.*

@Composable
fun ScheduleScreen(
    tasks: List<ScheduledTask>,
    onDeleteTask: (ScheduledTask) -> Unit,
    onToggleTask: (ScheduledTask) -> Unit,
) {
    val colors = AppColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Text(
            text = "Reminders",
            style = MaterialTheme.typography.headlineLarge,
            color = CrabOrange,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 16.dp),
        )

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No reminders yet",
                        color = colors.textMuted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Ask PocketClaw to set one!",
                        color = colors.textMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(task, onDeleteTask, onToggleTask, colors)
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: ScheduledTask,
    onDelete: (ScheduledTask) -> Unit,
    onToggle: (ScheduledTask) -> Unit,
    colors: AppColors,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.card),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "%02d:%02d".format(task.hour, task.minute),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (task.enabled) CrabOrange else colors.textMuted,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    task.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
                if (task.repeating) {
                    Text(
                        "Daily",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
            Switch(
                checked = task.enabled,
                onCheckedChange = { onToggle(task) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CrabOrange,
                    checkedTrackColor = CrabOrangeDark,
                ),
            )
            IconButton(onClick = { onDelete(task) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = colors.textMuted)
            }
        }
    }
}
