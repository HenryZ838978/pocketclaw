package com.pocketclaw.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketclaw.app.ui.theme.*
import kotlinx.coroutines.CompletableDeferred

/**
 * Singleton state for the tool-confirmation flow.
 * MainViewModel sets [pending]; the Composable observes and resolves it.
 */
object ToolConfirmState {
    var pending by mutableStateOf<PendingConfirm?>(null)
        private set

    data class PendingConfirm(
        val message: String,
        val deferred: CompletableDeferred<Boolean>,
    )

    fun request(message: String): CompletableDeferred<Boolean> {
        val d = CompletableDeferred<Boolean>()
        pending = PendingConfirm(message, d)
        return d
    }

    fun respond(allow: Boolean) {
        pending?.deferred?.complete(allow)
        pending = null
    }
}

@Composable
fun ToolConfirmDialog() {
    val confirm = ToolConfirmState.pending ?: return
    val colors = AppColors

    AlertDialog(
        onDismissRequest = { ToolConfirmState.respond(false) },
        containerColor = colors.card,
        icon = { Icon(Icons.Default.Warning, null, tint = AccentYellow, modifier = Modifier.size(32.dp)) },
        title = {
            Text("Confirm Action", color = colors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(confirm.message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = { ToolConfirmState.respond(true) },
                colors = ButtonDefaults.buttonColors(containerColor = CrabOrange),
            ) { Text("Allow") }
        },
        dismissButton = {
            OutlinedButton(onClick = { ToolConfirmState.respond(false) }) {
                Text("Deny", color = colors.textSecondary)
            }
        },
    )
}
