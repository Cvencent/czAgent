package com.czagent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czagent.core.skill.Skill
import com.czagent.core.skill.Trigger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillListScreen(
    skills: List<Skill>,
    onAddSkill: () -> Unit,
    onEditSkill: (Skill) -> Unit,
    onRunSkill: (Skill) -> Unit,
    onToggleSkill: (Skill, Boolean) -> Unit,
    onImportSkill: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("技能库") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSkill) {
                Icon(Icons.Default.Add, contentDescription = "添加技能")
            }
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search / natural language input
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("输入你想要做的事...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                    )
                },
            )

            // Skills list
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(onClick = onImportSkill) {
                            Text("📥 导入技能")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(skills) { skill ->
                    SkillCard(
                        skill = skill,
                        onEdit = { onEditSkill(skill) },
                        onRun = { onRunSkill(skill) },
                        onToggle = { enabled -> onToggleSkill(skill, enabled) },
                    )
                }
            }
        }
    }
}

@Composable
fun SkillCard(
    skill: Skill,
    onEdit: () -> Unit,
    onRun: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = skill.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (skill.description.isNotBlank()) {
                        Text(
                            text = skill.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = skill.enabled,
                    onCheckedChange = onToggle,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trigger summary
            val triggerText = when {
                skill.triggers.isEmpty() -> "无触发"
                skill.triggers.size == 1 -> triggerLabel(skill.triggers.first())
                else -> "${skill.triggers.size} 个触发"
            }
            Text(
                text = "触发: $triggerText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("编辑")
                }
                Button(
                    onClick = onRun,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("运行")
                }
            }
        }
    }
}

private fun triggerLabel(trigger: Trigger): String = when (trigger) {
    Trigger.Manual -> "手动"
    is Trigger.Notification -> "通知到达"
    is Trigger.AppSwitch -> "应用切换"
    is Trigger.DailySchedule -> "定时 ${trigger.localTime}"
    is Trigger.NetworkChange -> "网络变化"
}
