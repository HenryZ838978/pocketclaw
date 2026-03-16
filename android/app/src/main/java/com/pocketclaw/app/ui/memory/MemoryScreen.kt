package com.pocketclaw.app.ui.memory

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketclaw.app.data.Memory
import com.pocketclaw.app.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MemoryScreen(
    memories: List<Memory>,
    onDeleteMemory: (Memory) -> Unit,
) {
    val colors = AppColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Text(
            text = "Memories",
            style = MaterialTheme.typography.headlineLarge,
            color = CrabOrange,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
        )
        Text(
            text = "What your butler has learned about you",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 20.dp, bottom = 16.dp),
        )

        if (memories.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No memories yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textMuted,
                    )
                    Text(
                        text = "Chat with your butler to build memories",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(memories, key = { it.id }) { memory ->
                    MemoryCard(memory = memory, onDelete = { onDeleteMemory(memory) })
                }
            }
        }

        if (memories.isNotEmpty()) {
            Text(
                text = "${memories.size} memories stored locally",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun MemoryCard(
    memory: Memory,
    onDelete: () -> Unit,
) {
    val typeColor = when (memory.type) {
        "preference" -> AccentGreen
        "habit" -> AccentBlue
        "relationship" -> AccentPurple
        "fact" -> AccentYellow
        else -> TextSecondary
    }

    val timeFmt = DateTimeFormatter.ofPattern("MMM d, HH:mm")
        .withZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = AppColors.card),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp, 40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(typeColor),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = memory.key,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = memory.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = typeColor,
                    )
                }
                Text(
                    text = memory.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = timeFmt.format(memory.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = AppColors.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
