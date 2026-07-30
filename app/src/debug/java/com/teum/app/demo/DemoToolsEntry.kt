package com.teum.app.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.teum.app.core.model.PermissionStatus
import kotlinx.coroutines.launch

@Composable
fun DemoToolsEntry(
    permissionStatus: PermissionStatus,
    forceVulnerableNowForDebug: Boolean,
    onForceVulnerableNowForDebugChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val seeder = remember(context) { RoomDemoDataSeeder(context) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<DemoSeedResult?>(null) }

    fun runDemoAction(action: suspend DemoDataSeeder.() -> DemoSeedResult) {
        if (isRunning) return
        scope.launch {
            isRunning = true
            lastResult = seeder.action()
            isRunning = false
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE3E7EF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "시연 준비 도구",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "debug 빌드에서만 최근 7일 시연 데이터를 준비해요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Button(
                onClick = { showConfirmDialog = true },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isRunning) "처리 중" else "시연 데이터 초기화 및 생성",
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = { runDemoAction { verifyCurrentSeed() } },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "현재 Seed 검증", fontWeight = FontWeight.Bold)
            }
            DemoSwitchLine(
                label = "현재 시간을 취약 시간으로 강제",
                checked = forceVulnerableNowForDebug,
                onCheckedChange = onForceVulnerableNowForDebugChange
            )
            DemoStatusLine(
                label = "접근성 권한",
                value = if (permissionStatus.isAccessibilityEnabled) "켜짐" else "꺼짐"
            )
            DemoStatusLine(
                label = "다른 앱 위에 표시",
                value = if (permissionStatus.canDrawOverlays) "켜짐" else "꺼짐"
            )
            lastResult?.let { result ->
                Spacer(modifier = Modifier.height(2.dp))
                DemoResultSummary(result = result)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("시연 데이터를 만들까요?") },
            text = {
                Text(
                    text = "이 기기의 기존 사용 기록을 모두 삭제하고\n" +
                        "시연용 최근 7일 데이터로 교체합니다.\n\n" +
                        "관리 앱은 YouTube, Instagram, Chrome으로 설정되며\n" +
                        "조심 모드가 켜집니다.\n\n" +
                        "접근성 및 다른 앱 위에 표시 권한은 변경하지 않습니다.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        runDemoAction { resetAndSeed() }
                    }
                ) {
                    Text("삭제하고 시연 데이터 만들기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun DemoSwitchLine(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DemoStatusLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DemoResultSummary(result: DemoSeedResult) {
    val statusColor = if (result.success) Color(0xFF2EC4A6) else Color(0xFFF05D5E)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(statusColor.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (result.success) "시연 데이터 생성 완료" else "시연 데이터 확인 필요",
            color = statusColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = listOf(
                "${result.rowCounts["sessionLogs"] ?: 0} sessions",
                "${result.rowCounts["appOpenEvents"] ?: 0} opens",
                "${result.rowCounts["extensionEvents"] ?: 0} extensions",
                "${result.rowCounts["reopenLogs"] ?: 0} fast reopen logs",
                "${result.rowCounts["selfControlEvents"] ?: 0} close-now events"
            ).joinToString("\n"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Text(
            text = "오늘 목표 준수 " +
                "${result.actualValues["home.todayTargetKeptCount"] ?: "-"}" +
                "/${result.actualValues["home.todaySessionCount"] ?: "-"}\n" +
                "주간 목표 초과 " +
                "${result.actualValues["weekly.overrunCount"] ?: "-"}" +
                "/${result.actualValues["weekly.totalSessionCount"] ?: "-"}\n" +
                "취약 시간 ${result.actualValues["vulnerable.topHour"] ?: "-"}:00",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (result.mismatches.isNotEmpty()) {
            Text(
                text = result.mismatches.take(3).joinToString("\n"),
                color = Color(0xFFF05D5E),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        if (result.warnings.isNotEmpty()) {
            Text(
                text = result.warnings.take(2).joinToString("\n"),
                color = Color(0xFFFF9F43),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
