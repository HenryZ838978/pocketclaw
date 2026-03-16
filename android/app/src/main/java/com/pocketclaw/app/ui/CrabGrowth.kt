package com.pocketclaw.app.ui

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.pocketclaw.app.PocketClawApplication
import com.pocketclaw.app.R
import com.pocketclaw.app.ui.theme.*

data class GrowthStageInfo(
    val stage: Int,
    val name: String,
    val interactionsNeeded: Int,
    val memoriesNeeded: Int,
)

val GROWTH_STAGES = listOf(
    GrowthStageInfo(0, "Egg", 0, 0),
    GrowthStageInfo(1, "Hatchling", 10, 0),
    GrowthStageInfo(2, "Juvenile", 50, 10),
    GrowthStageInfo(3, "Adult", 200, 30),
    GrowthStageInfo(4, "Elder", 500, 50),
)

fun checkAndNotifyEvolution(context: Context, previousStage: Int, newStage: Int) {
    if (newStage > previousStage && newStage > 0) {
        val stageInfo = GROWTH_STAGES.getOrNull(newStage) ?: return
        val manager = context.getSystemService(NotificationManager::class.java)

        val notification = NotificationCompat.Builder(context, PocketClawApplication.CHANNEL_TRIAGE)
            .setSmallIcon(R.drawable.ic_crab)
            .setContentTitle("Your crab evolved!")
            .setContentText("Stage ${stageInfo.stage}: ${stageInfo.name}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your butler has grown to ${stageInfo.name} stage! Keep interacting to unlock more growth."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(9000 + newStage, notification)
        Log.i("CrabGrowth", "Evolution! Stage $previousStage → $newStage (${stageInfo.name})")
    }
}

@Composable
fun AnimatedCrabAvatar(
    stage: Int,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "crab")

    val wobble by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (stage) {
                    0 -> 2000
                    1 -> 800
                    2 -> 1200
                    3 -> 1600
                    else -> 2400
                },
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wobble",
    )

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    val bodyColor = when (stage) {
        0 -> Color(0xFFF5E6D3)
        1 -> CrabOrangeLight
        2 -> CrabOrange
        3 -> CrabOrangeDark
        4 -> Color(0xFFFFD700)
        else -> CrabOrange
    }

    val size = when (stage) {
        0 -> 60.dp
        1 -> 70.dp
        2 -> 80.dp
        3 -> 90.dp
        4 -> 100.dp
        else -> 60.dp
    }

    Canvas(
        modifier = modifier.size(size)
    ) {
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        val radius = this.size.minDimension / 3 * breathe

        if (stage == 4) {
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = glowAlpha * 0.3f),
                radius = radius * 1.6f,
                center = Offset(cx, cy),
            )
        }

        rotate(wobble, pivot = Offset(cx, cy)) {
            if (stage == 0) {
                drawEgg(cx, cy, radius, bodyColor)
            } else {
                drawCrab(cx, cy, radius, bodyColor, stage, glowAlpha)
            }
        }
    }
}

private fun DrawScope.drawEgg(cx: Float, cy: Float, radius: Float, color: Color) {
    drawOval(
        color = color,
        topLeft = Offset(cx - radius * 0.7f, cy - radius),
        size = androidx.compose.ui.geometry.Size(radius * 1.4f, radius * 2f),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.3f),
        topLeft = Offset(cx - radius * 0.3f, cy - radius * 0.7f),
        size = androidx.compose.ui.geometry.Size(radius * 0.4f, radius * 0.5f),
    )
}

private fun DrawScope.drawCrab(
    cx: Float, cy: Float, radius: Float,
    color: Color, stage: Int, glow: Float
) {
    val legColor = color.copy(alpha = 0.8f)
    val legWidth = radius * 0.08f

    val legAngles = listOf(-40f, -20f, 0f, 20f, 40f)
    for (angle in legAngles) {
        val rad = Math.toRadians(angle.toDouble() + 90)
        val startX = cx - radius * 0.8f
        val endX = startX - radius * 0.6f
        val startY = cy + (radius * 0.3f * Math.sin(rad)).toFloat()
        val endY = startY + radius * 0.3f

        drawLine(legColor, Offset(startX, startY), Offset(endX, endY), legWidth)

        val mirrorStartX = cx + radius * 0.8f
        val mirrorEndX = mirrorStartX + radius * 0.6f
        drawLine(legColor, Offset(mirrorStartX, startY), Offset(mirrorEndX, endY), legWidth)
    }

    val clawSize = radius * (0.3f + stage * 0.05f)
    drawCircle(color, clawSize, Offset(cx - radius * 1.3f, cy - radius * 0.3f))
    drawCircle(color, clawSize, Offset(cx + radius * 1.3f, cy - radius * 0.3f))

    drawOval(
        color = color,
        topLeft = Offset(cx - radius, cy - radius * 0.7f),
        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 1.4f),
    )

    drawOval(
        color = Color.White.copy(alpha = 0.3f),
        topLeft = Offset(cx - radius * 0.6f, cy - radius * 0.5f),
        size = androidx.compose.ui.geometry.Size(radius * 0.8f, radius * 0.4f),
    )

    val eyeOffsetY = cy - radius * 0.15f
    val eyeSpacing = radius * 0.35f
    val eyeRadius = radius * 0.15f

    drawCircle(Color.White, eyeRadius, Offset(cx - eyeSpacing, eyeOffsetY))
    drawCircle(Color.White, eyeRadius, Offset(cx + eyeSpacing, eyeOffsetY))
    drawCircle(Color(0xFF0D0D0D), eyeRadius * 0.55f, Offset(cx - eyeSpacing, eyeOffsetY))
    drawCircle(Color(0xFF0D0D0D), eyeRadius * 0.55f, Offset(cx + eyeSpacing, eyeOffsetY))
    drawCircle(Color.White, eyeRadius * 0.2f, Offset(cx - eyeSpacing + eyeRadius * 0.15f, eyeOffsetY - eyeRadius * 0.2f))
    drawCircle(Color.White, eyeRadius * 0.2f, Offset(cx + eyeSpacing + eyeRadius * 0.15f, eyeOffsetY - eyeRadius * 0.2f))

    if (stage >= 3) {
        val smileY = cy + radius * 0.25f
        val path = Path().apply {
            moveTo(cx - radius * 0.2f, smileY)
            quadraticTo(cx, smileY + radius * 0.15f, cx + radius * 0.2f, smileY)
        }
        drawPath(path, Color(0xFF0D0D0D).copy(alpha = 0.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.04f))
    }

    if (stage == 4) {
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = glow * 0.5f),
            radius = radius * 1.2f,
            center = Offset(cx, cy),
        )
    }
}
