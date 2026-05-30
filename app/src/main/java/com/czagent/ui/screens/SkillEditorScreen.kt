package com.czagent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czagent.core.model.StepType
import com.czagent.core.model.SwipeDirection
import com.czagent.core.skill.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillEditorScreen(
    initialSkill: Skill? = null,
    onSave: (Skill) -> Unit,
    onDelete: (String) -> Unit,
    onExport: (Skill) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(initialSkill?.name ?: "") }
    var description by remember { mutableStateOf(initialSkill?.description ?: "") }
    var tags by remember { mutableStateOf(initialSkill?.tags ?: emptyList()) }
    var parameters by remember { mutableStateOf(initialSkill?.parameters ?: emptyList()) }
    var steps by remember { mutableStateOf(initialSkill?.steps ?: emptyList()) }
    var triggers by remember { mutableStateOf(initialSkill?.triggers ?: listOf(Trigger.Manual)) }
    val isEditing = initialSkill != null

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text(if (isEditing) "编辑技能" else "新建技能") },
                actions = {
                    if (isEditing) {
                        TextButton(onClick = { onDelete(initialSkill!!.id) }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(onClick = {
                        val skill = Skill(
                            id = initialSkill?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name,
                            description = description,
                            tags = tags,
                            version = (initialSkill?.version ?: 0) + 1,
                            parameters = parameters,
                            steps = steps,
                            triggers = triggers,
                            enabled = initialSkill?.enabled ?: true,
                            createdAt = initialSkill?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                        )
                        onSave(skill)
                    }) {
                        Text("保存")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Basic info
            item {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("技能名称") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )

                    // Tags section
                    Column {
                        Text("标签", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Simplified tags UI for now
                        Text("(标签功能即将推出)")
                    }
                }
            }

            // Parameters
            item {
                SectionHeader("参数", Modifier.padding(horizontal = 16.dp))
                // Simplified parameters UI
                Text(
                    "(参数编辑功能即将推出)",
                    modifier = Modifier.padding(16.dp),
                )
            }

            // Steps
            item {
                SectionHeader("步骤", Modifier.padding(horizontal = 16.dp))
            }

            itemsIndexed(steps) { index, step ->
                SkillStepItem(
                    step = step,
                    onUpdate = { updatedStep ->
                        steps = steps.toMutableList().also { list ->
                            list[index] = updatedStep
                        }
                    },
                    onDelete = {
                        steps = steps.filterIndexed { i, _ -> i != index }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            item {
                Button(
                    onClick = {
                        steps = steps + SkillStep(
                            orderIndex = steps.size,
                            type = StepType.CLICK_TEXT,
                            label = "新步骤",
                        )
                    },
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text("+ 添加步骤")
                }
            }

            // Triggers
            item {
                SectionHeader("触发器", Modifier.padding(horizontal = 16.dp))
                // Simplified triggers UI
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = triggers.any { it is Trigger.Manual },
                            onCheckedChange = { checked ->
                                triggers = if (checked) {
                                    triggers + Trigger.Manual
                                } else {
                                    triggers.filterNot { it is Trigger.Manual }
                                }
                            },
                        )
                        Text("手动触发")
                    }
                    Text("(更多触发器即将推出)")
                }
            }

            // Import/Export
            item {
                SectionHeader("导入/导出", Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onImport,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("📥 导入")
                    }
                    if (isEditing) {
                        Button(
                            onClick = { onExport(initialSkill!!) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("📤 导出")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun SkillStepItem(
    step: SkillStep,
    onUpdate: (SkillStep) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${step.orderIndex + 1}.",
                style = MaterialTheme.typography.titleSmall,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = step.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "删除步骤",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
