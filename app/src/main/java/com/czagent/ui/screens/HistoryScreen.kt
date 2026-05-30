package com.czagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czagent.core.model.RunStatus
import com.czagent.ui.AppState
import com.czagent.ui.SkillRunSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(appState: AppState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("历史记录", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 技能运行历史
        if (appState.skillRuns.isNotEmpty()) {
            item {
                Text(
                    "技能运行",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            items(appState.skillRuns, key = { it.runId }) { run ->
                SkillRunCard(
                    run = run,
                    onDelete = { appState.deleteSkillRun(run.runId) },
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // 任务运行历史
        if (appState.runs.isNotEmpty()) {
            item {
                Text(
                    "任务运行",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            items(appState.runs.size) { index ->
                val run = appState.runs[index]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatRunSummaryLine(run.taskName, run.status, run.failureReason), style = MaterialTheme.typography.titleSmall)
                        Text("开始于 ${formatTimestamp(run.startedAt)}")
                        run.failureReason?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // 空状态
        if (appState.skillRuns.isEmpty() && appState.runs.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.History, null)
                        Text("还没有历史记录", style = MaterialTheme.typography.titleSmall)
                        Text("运行一个技能或任务，结果将出现在这里。")
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillRunCard(
    run: SkillRunSummary,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = statusColor(run.status),
                    )
                    Text(run.skillName, style = MaterialTheme.typography.titleSmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = statusLabel(run.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor(run.status),
                )
                Text(
                    text = formatTimestamp(run.startedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (run.endedAt != null) {
                val durationMs = run.endedAt - run.startedAt
                Text(
                    text = "耗时 ${formatDuration(durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            run.failureReason?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun statusColor(status: RunStatus) = when (status) {
    RunStatus.SUCCEEDED -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
    RunStatus.RUNNING -> androidx.compose.ui.graphics.Color(0xFF2196F3)
    RunStatus.FAILED -> androidx.compose.ui.graphics.Color(0xFFF44336)
    RunStatus.CANCELLED -> androidx.compose.ui.graphics.Color(0xFFFF9800)
    RunStatus.TIMEOUT -> androidx.compose.ui.graphics.Color(0xFFFF5722)
    RunStatus.WAITING_FOR_CONFIRMATION -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
}

private fun statusLabel(status: RunStatus) = when (status) {
    RunStatus.SUCCEEDED -> "成功"
    RunStatus.RUNNING -> "运行中"
    RunStatus.FAILED -> "失败"
    RunStatus.CANCELLED -> "已取消"
    RunStatus.TIMEOUT -> "超时"
    RunStatus.WAITING_FOR_CONFIRMATION -> "等待确认"
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatDuration(millis: Long): String {
    return when {
        millis < 1000 -> "${millis}ms"
        millis < 60_000 -> "${millis / 1000}秒"
        else -> "${millis / 60_000}分${(millis % 60_000) / 1000}秒"
    }
}
