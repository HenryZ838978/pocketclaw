@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.pocketclaw.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketclaw.app.R
import com.pocketclaw.app.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Instant = Instant.now(),
    val sourceApp: String? = null,
)

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isRecording: Boolean,
    isProcessing: Boolean,
    growthStage: Int,
    interactionCount: Int,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendText: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onNewTopic: () -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onSpeakMessage: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val colors = AppColors

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        LobsterAvatar(
            stage = growthStage,
            interactionCount = interactionCount,
            onNewTopic = onNewTopic,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Say something to your butler...\nor tap + to start a new topic",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    onLongClick = { onDeleteMessage(message) },
                    onSpeak = if (!message.isUser) {{ onSpeakMessage(message.text) }} else null,
                )
            }
        }

        AnimatedVisibility(visible = isProcessing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = CrabOrange,
                trackColor = colors.card,
            )
        }

        ChatInputBar(
            text = inputText,
            onTextChange = onInputTextChange,
            isRecording = isRecording,
            onSend = {
                if (inputText.isNotBlank()) {
                    onSendText(inputText.trim())
                    onInputTextChange("")
                }
            },
            onMicPress = {
                if (isRecording) onStopRecording() else onStartRecording()
            },
        )
    }
}

@Composable
private fun LobsterAvatar(
    stage: Int,
    interactionCount: Int,
    onNewTopic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lobster")

    val wobble by infiniteTransition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "wobble",
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    val stageNames = listOf("Larva", "Hatchling", "Juvenile", "Adult", "Elder")
    val stageName = stageNames.getOrElse(stage) { "Larva" }
    val colors = AppColors

    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.lobster_avatar),
            contentDescription = "Lobster avatar",
            modifier = Modifier
                .size(64.dp)
                .scale(breathe)
                .rotate(wobble)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stageName,
                style = MaterialTheme.typography.titleMedium,
                color = CrabOrange,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$interactionCount interactions",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        FilledTonalButton(
            onClick = onNewTopic,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = colors.elevated,
                contentColor = colors.textPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("New Topic", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onLongClick: () -> Unit,
    onSpeak: (() -> Unit)?,
) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val colors = AppColors
    val bgColor = if (message.isUser) colors.userBubble else colors.card
    val textColor = if (message.isUser) colors.userBubbleText else colors.textPrimary
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (message.isUser) 16.dp else 4.dp,
        bottomEnd = if (message.isUser) 4.dp else 16.dp,
    )

    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick,
                )
                .background(bgColor)
                .padding(12.dp)
        ) {
            message.sourceApp?.let { app ->
                Text(
                    text = "via $app",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onSpeak != null) {
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Text(
                    text = timeFmt.format(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isRecording: Boolean,
    onSend: () -> Unit,
    onMicPress: () -> Unit,
) {
    val colors = AppColors
    Surface(
        color = colors.card,
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FloatingActionButton(
                onClick = onMicPress,
                containerColor = if (isRecording) AccentRed else CrabOrange,
                contentColor = DarkTextPrimary,
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop recording" else "Start recording",
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Message your butler...", color = colors.textMuted)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrabOrange,
                    unfocusedBorderColor = colors.textMuted,
                    cursorColor = CrabOrange,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) CrabOrange else colors.textMuted,
                )
            }
        }
    }
}
