package com.czagent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
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
                    }, enabled = name.isNotBlank()) {
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
                        TagsEditor(
                            tags = tags,
                            onTagsChanged = { tags = it },
                        )
                    }
                }
            }

            // Parameters
            item {
                SectionHeader("参数", Modifier.padding(horizontal = 16.dp))
            }
            itemsIndexed(parameters) { index, param ->
                SkillParameterItem(
                    parameter = param,
                    index = index,
                    onUpdate = { updatedParam ->
                        parameters = parameters.toMutableList().also { list ->
                            list[index] = updatedParam
                        }
                    },
                    onDelete = {
                        parameters = parameters.filterIndexed { i, _ -> i != index }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                Button(
                    onClick = {
                        parameters = parameters + SkillParameter(
                            name = "",
                            displayName = "",
                            type = ParamType.TEXT,
                            defaultValue = null,
                            required = true,
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("添加参数")
                }
            }

            // Steps
            item {
                SectionHeader("步骤", Modifier.padding(horizontal = 16.dp))
            }
            itemsIndexed(steps) { index, step ->
                SkillStepItem(
                    step = step,
                    isFirst = index == 0,
                    isLast = index == steps.lastIndex,
                    onUpdate = { updatedStep ->
                        steps = steps.toMutableList().also { list ->
                            list[index] = updatedStep
                        }
                    },
                    onDelete = {
                        steps = steps.filterIndexed { i, _ -> i != index }.mapIndexed { i, s ->
                            s.copy(orderIndex = i)
                        }
                    },
                    onMoveUp = {
                        if (index > 0) {
                            steps = steps.toMutableList().also { list ->
                                val item = list.removeAt(index)
                                list.add(index - 1, item)
                            }.mapIndexed { i, s -> s.copy(orderIndex = i) }
                        }
                    },
                    onMoveDown = {
                        if (index < steps.lastIndex) {
                            steps = steps.toMutableList().also { list ->
                                val item = list.removeAt(index)
                                list.add(index + 1, item)
                            }.mapIndexed { i, s -> s.copy(orderIndex = i) }
                        }
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
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("添加步骤")
                }
            }

            // Triggers
            item {
                SectionHeader("触发器", Modifier.padding(horizontal = 16.dp))
            }
            item {
                SkillTriggersEditor(
                    triggers = triggers,
                    onTriggersChanged = { triggers = it },
                    modifier = Modifier.padding(16.dp),
                )
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
private fun TagsEditor(
    tags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newTag by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                label = { Text("新标签") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(
                onClick = {
                    if (newTag.isNotBlank() && newTag !in tags) {
                        onTagsChanged(tags + newTag)
                        newTag = ""
                    }
                },
                enabled = newTag.isNotBlank(),
            ) {
                Text("添加")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (tags.isNotEmpty()) {
            WrapFlow(
                modifier = Modifier.fillMaxWidth(),
                horizontalGap = 8.dp,
                verticalGap = 8.dp,
            ) {
                tags.forEach { tag ->
                    AssistChip(
                        onClick = { onTagsChanged(tags - tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除标签",
                                Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        } else {
            Text("还没有标签，添加一些标签来帮助识别技能", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SkillParameterItem(
    parameter: SkillParameter,
    index: Int,
    onUpdate: (SkillParameter) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("参数 #${index + 1}", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除参数",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OutlinedTextField(
                value = parameter.name,
                onValueChange = { onUpdate(parameter.copy(name = it)) },
                label = { Text("参数名 (用于模板中)") },
                placeholder = { Text("例如: contact") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = parameter.displayName,
                onValueChange = { onUpdate(parameter.copy(displayName = it)) },
                label = { Text("显示名称") },
                placeholder = { Text("例如: 联系人") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            ParamTypeSelector(
                selectedType = parameter.type,
                onTypeSelected = { onUpdate(parameter.copy(type = it)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = parameter.defaultValue ?: "",
                onValueChange = { onUpdate(parameter.copy(defaultValue = it.ifBlank { null })) },
                label = { Text("默认值 (可选)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("必填")
                Switch(
                    checked = parameter.required,
                    onCheckedChange = { onUpdate(parameter.copy(required = it)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParamTypeSelector(
    selectedType: ParamType,
    onTypeSelected: (ParamType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedType.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("参数类型") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ParamType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SkillStepItem(
    step: SkillStep,
    isFirst: Boolean,
    isLast: Boolean,
    onUpdate: (SkillStep) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Default.ArrowDropUp,
                            contentDescription = "上移",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "${step.orderIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "下移",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
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
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = if (expanded) "收起编辑" else "编辑步骤",
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除步骤",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (expanded) {
                SkillStepEditor(
                    step = step,
                    onUpdate = onUpdate,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SkillStepEditor(
    step: SkillStep,
    onUpdate: (SkillStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = step.label,
            onValueChange = { onUpdate(step.copy(label = it)) },
            label = { Text("步骤描述") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        StepTypeSelector(
            selectedType = step.type,
            onTypeSelected = { onUpdate(step.copy(type = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        when (step.type) {
            StepType.CLICK_TEXT -> {
                OutlinedTextField(
                    value = step.selectorText ?: "",
                    onValueChange = { onUpdate(step.copy(selectorText = it.ifBlank { null })) },
                    label = { Text("点击的文本") },
                    placeholder = { Text("例如: {contact} 或 搜索") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            StepType.INPUT_TEXT -> {
                OutlinedTextField(
                    value = step.selectorText ?: "",
                    onValueChange = { onUpdate(step.copy(selectorText = it.ifBlank { null })) },
                    label = { Text("目标字段文本 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = step.inputText ?: "",
                    onValueChange = { onUpdate(step.copy(inputText = it.ifBlank { null })) },
                    label = { Text("输入的文本") },
                    placeholder = { Text("例如: {message}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            StepType.CLICK_COORDINATES -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = step.x?.toString() ?: "",
                        onValueChange = { onUpdate(step.copy(x = it.toIntOrNull())) },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = step.y?.toString() ?: "",
                        onValueChange = { onUpdate(step.copy(y = it.toIntOrNull())) },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }
            StepType.WAIT -> {
                OutlinedTextField(
                    value = step.waitMillis?.toString() ?: "",
                    onValueChange = { onUpdate(step.copy(waitMillis = it.toLongOrNull())) },
                    label = { Text("等待毫秒数") },
                    placeholder = { Text("1000") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            StepType.SWIPE -> {
                SwipeDirectionSelector(
                    selectedDirection = step.swipeDirection,
                    onDirectionSelected = { onUpdate(step.copy(swipeDirection = it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            StepType.OPEN_APP, StepType.BACK, StepType.SCREENSHOT, StepType.COMPLETE -> {
                // 这些类型不需要额外参数
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("执行前需要确认")
            Switch(
                checked = step.requiresConfirmation,
                onCheckedChange = { onUpdate(step.copy(requiresConfirmation = it)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepTypeSelector(
    selectedType: StepType,
    onTypeSelected: (StepType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedType.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("步骤类型") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            StepType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDirectionSelector(
    selectedDirection: SwipeDirection?,
    onDirectionSelected: (SwipeDirection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedDirection?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("滑动方向") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SwipeDirection.entries.forEach { direction ->
                DropdownMenuItem(
                    text = { Text(direction.name) },
                    onClick = {
                        onDirectionSelected(direction)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SkillTriggersEditor(
    triggers: List<Trigger>,
    onTriggersChanged: (List<Trigger>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Manual trigger
        TriggerToggle(
            title = "手动触发",
            description = "用户在技能列表中手动点击运行",
            checked = triggers.any { it is Trigger.Manual },
            onToggle = { checked ->
                onTriggersChanged(
                    if (checked) {
                        triggers + Trigger.Manual
                    } else {
                        triggers.filterNot { it is Trigger.Manual }
                    }
                )
            },
        )

        // Notification trigger
        val notificationTrigger = triggers.filterIsInstance<Trigger.Notification>().firstOrNull()
        TriggerToggle(
            title = "通知触发",
            description = "当收到指定应用的通知时触发",
            checked = notificationTrigger != null,
            onToggle = { checked ->
                onTriggersChanged(
                    if (checked) {
                        triggers + Trigger.Notification(packageName = "", keywordFilter = null)
                    } else {
                        triggers.filterNot { it is Trigger.Notification }
                    }
                )
            },
        )
        if (notificationTrigger != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = notificationTrigger.packageName,
                    onValueChange = {
                        onTriggersChanged(triggers.map { t ->
                            if (t is Trigger.Notification) t.copy(packageName = it) else t
                        })
                    },
                    label = { Text("目标应用包名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notificationTrigger.keywordFilter ?: "",
                    onValueChange = {
                        onTriggersChanged(triggers.map { t ->
                            if (t is Trigger.Notification) t.copy(keywordFilter = it.ifBlank { null }) else t
                        })
                    },
                    label = { Text("关键词过滤 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        // App switch trigger
        val appSwitchTrigger = triggers.filterIsInstance<Trigger.AppSwitch>().firstOrNull()
        TriggerToggle(
            title = "应用切换触发",
            description = "当打开或关闭指定应用时触发",
            checked = appSwitchTrigger != null,
            onToggle = { checked ->
                onTriggersChanged(
                    if (checked) {
                        triggers + Trigger.AppSwitch(packageName = "", onEntry = true)
                    } else {
                        triggers.filterNot { it is Trigger.AppSwitch }
                    }
                )
            },
        )
        if (appSwitchTrigger != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = appSwitchTrigger.packageName,
                    onValueChange = {
                        onTriggersChanged(triggers.map { t ->
                            if (t is Trigger.AppSwitch) t.copy(packageName = it) else t
                        })
                    },
                    label = { Text("目标应用包名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("打开应用时触发")
                    Switch(
                        checked = appSwitchTrigger.onEntry,
                        onCheckedChange = {
                            onTriggersChanged(triggers.map { t ->
                                if (t is Trigger.AppSwitch) t.copy(onEntry = it) else t
                            })
                        },
                    )
                }
            }
        }

        // Daily schedule trigger
        val dailyScheduleTrigger = triggers.filterIsInstance<Trigger.DailySchedule>().firstOrNull()
        TriggerToggle(
            title = "定时触发",
            description = "每天在指定时间触发",
            checked = dailyScheduleTrigger != null,
            onToggle = { checked ->
                onTriggersChanged(
                    if (checked) {
                        triggers + Trigger.DailySchedule(localTime = "08:00")
                    } else {
                        triggers.filterNot { it is Trigger.DailySchedule }
                    }
                )
            },
        )
        if (dailyScheduleTrigger != null) {
            OutlinedTextField(
                value = dailyScheduleTrigger.localTime,
                onValueChange = {
                    onTriggersChanged(triggers.map { t ->
                        if (t is Trigger.DailySchedule) t.copy(localTime = it) else t
                    })
                },
                label = { Text("触发时间 (HH:mm)") },
                placeholder = { Text("例如: 08:30") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        // Network change trigger
        val networkChangeTrigger = triggers.filterIsInstance<Trigger.NetworkChange>().firstOrNull()
        TriggerToggle(
            title = "网络变化触发",
            description = "当连接到指定 Wi-Fi 或网络状态变化时触发",
            checked = networkChangeTrigger != null,
            onToggle = { checked ->
                onTriggersChanged(
                    if (checked) {
                        triggers + Trigger.NetworkChange(ssid = null)
                    } else {
                        triggers.filterNot { it is Trigger.NetworkChange }
                    }
                )
            },
        )
        if (networkChangeTrigger != null) {
            OutlinedTextField(
                value = networkChangeTrigger.ssid ?: "",
                onValueChange = {
                    onTriggersChanged(triggers.map { t ->
                        if (t is Trigger.NetworkChange) t.copy(ssid = it.ifBlank { null }) else t
                    })
                },
                label = { Text("目标 Wi-Fi SSID (可选)") },
                placeholder = { Text("不填写则在任何网络变化时触发") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun TriggerToggle(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrapFlow(
    modifier: Modifier = Modifier,
    horizontalGap: androidx.compose.ui.unit.Dp = 0.dp,
    verticalGap: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalGap),
        verticalArrangement = Arrangement.spacedBy(verticalGap),
    ) {
        content()
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
