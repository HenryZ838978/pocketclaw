@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.pocketclaw.app.ui.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketclaw.app.data.CustomSkill
import com.pocketclaw.app.ui.theme.*

data class SkillInfo(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val category: String,
    val usageCount: Int = 0,
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
)

val BUILTIN_SKILL_UI = listOf(
    SkillInfo("message_triage", "Message\nTriage", Icons.Default.FilterList, "Communication"),
    SkillInfo("schedule_manage", "Schedule\nManager", Icons.Default.CalendarMonth, "Organization"),
    SkillInfo("quick_reply", "Quick\nReply", Icons.AutoMirrored.Filled.Reply, "Communication"),
    SkillInfo("web_search", "Web\nSearch", Icons.Default.Search, "Knowledge"),
    SkillInfo("expense_track", "Expense\nTracker", Icons.Default.AccountBalance, "Organization"),
    SkillInfo("translate", "Translator", Icons.Default.Translate, "Knowledge"),
    SkillInfo("daily_digest", "Daily\nDigest", Icons.Default.Summarize, "Organization"),
    SkillInfo("contact_lookup", "Contact\nLookup", Icons.Default.Contacts, "Communication"),
    SkillInfo("note_capture", "Quick\nNote", Icons.AutoMirrored.Filled.Note, "Organization"),
    SkillInfo("alarm_timer", "Alarm &\nTimer", Icons.Default.Alarm, "Organization"),
    SkillInfo("file_manage", "File\nManager", Icons.Default.Folder, "Organization"),
    SkillInfo("weather_check", "Weather", Icons.Default.WbSunny, "Knowledge"),
)

@Composable
fun SkillsScreen(
    skillUsage: Map<String, Int>,
    customSkills: List<CustomSkill>,
    onDeleteSkill: (CustomSkill) -> Unit,
    onToggleSkill: (CustomSkill) -> Unit,
) {
    val colors = AppColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Text(
            text = "Skills",
            style = MaterialTheme.typography.headlineLarge,
            color = CrabOrange,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
        )
        Text(
            text = "Built-in + custom. Chat \"add a skill for ...\" to create new ones.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (customSkills.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Text(
                        text = "CUSTOM",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentPurple,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
                items(customSkills, key = { "custom_${it.id}" }) { skill ->
                    CustomSkillCard(
                        skill = skill,
                        usageCount = skillUsage[skill.skillId] ?: 0,
                        onLongClick = { onDeleteSkill(skill) },
                        onToggle = { onToggleSkill(skill) },
                    )
                }
                item(span = { GridItemSpan(3) }) {
                    Text(
                        text = "BUILT-IN",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
            }
            items(BUILTIN_SKILL_UI) { skill ->
                BuiltinSkillCard(
                    skill = skill.copy(usageCount = skillUsage[skill.id] ?: 0)
                )
            }
        }
    }
}

@Composable
private fun BuiltinSkillCard(skill: SkillInfo) {
    val isActive = skill.usageCount > 0
    val colors = AppColors
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) colors.elevated else colors.card,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.aspectRatio(0.85f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = skill.icon,
                contentDescription = skill.name,
                tint = if (isActive) CrabOrange else colors.textMuted,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = skill.name,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) colors.textPrimary else colors.textMuted,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (skill.usageCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("${skill.usageCount}x", style = MaterialTheme.typography.bodySmall, color = CrabOrange)
            }
        }
    }
}

@Composable
private fun CustomSkillCard(
    skill: CustomSkill,
    usageCount: Int,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = AppColors
    val alpha = if (skill.enabled) 1f else 0.4f
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.elevated),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .aspectRatio(0.85f)
            .combinedClickable(onClick = onToggle, onLongClick = onLongClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = skill.name,
                tint = AccentPurple.copy(alpha = alpha),
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = skill.name,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textPrimary.copy(alpha = alpha),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (usageCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("${usageCount}x", style = MaterialTheme.typography.bodySmall, color = AccentPurple)
            }
        }
    }
}
