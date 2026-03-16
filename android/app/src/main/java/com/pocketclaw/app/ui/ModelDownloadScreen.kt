package com.pocketclaw.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketclaw.app.model.ModelDownloadManager
import com.pocketclaw.app.model.OverallDownloadState
import com.pocketclaw.app.ui.theme.*

@Composable
fun ModelDownloadScreen(
    downloadState: OverallDownloadState,
    onStartDownload: () -> Unit,
    onSkip: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "egg")
    val wobble by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wobble",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedCrabAvatar(
            stage = 0,
            modifier = Modifier.padding(16.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your butler is hatching...",
            style = MaterialTheme.typography.headlineMedium,
            color = CrabOrange,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Download voice models for on-device\nspeech recognition & synthesis (~150MB)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (downloadState.isDownloading) {
            DownloadProgressView(downloadState)
        } else if (downloadState.error != null) {
            Text(
                text = downloadState.error!!,
                color = AccentRed,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStartDownload,
                colors = ButtonDefaults.buttonColors(containerColor = CrabOrange),
            ) {
                Text("Retry Download")
            }
        } else {
            Button(
                onClick = onStartDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrabOrange),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    "Download Voice Models",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onSkip) {
                Text(
                    "Skip for now (text only)",
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressView(state: OverallDownloadState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.currentModel,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        state.currentProgress?.let { progress ->
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CrabOrange,
                trackColor = SurfaceCard,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "%.1f / %.1f MB".format(
                    progress.megabytesDownloaded,
                    progress.megabytesTotal,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${state.modelsComplete} of ${state.modelsTotal} models",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
    }
}
