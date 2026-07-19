package com.teum.app.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teum.app.core.model.PermissionStatus
import com.teum.app.core.util.TeumConstants
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.ui.theme.TeumTheme

data class DashboardStats(
    val todaySessionCount: Int = 0,
    val todayOverrunCount: Int = 0,
    val todayFastReopenCount: Int = 0,
    val todayPurposeDriftCount: Int = 0
)

@Composable
fun DashboardScreen(
    permissionStatus: PermissionStatus,
    targetPackages: Set<String>,
    appDisplayNames: Map<String, String>,
    dashboardStats: DashboardStats,
    recentSessions: List<SessionLogEntity>,
    timeSlotStats: List<TimeSlotStat>,
    weeklyReportStats: WeeklyReportStats,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onAddTargetPackage: (String) -> Unit,
    onRemoveTargetPackage: (String) -> Unit,
    onDeleteAllSessionLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("사용 기록을 삭제할까요?") },
            text = { Text("저장된 세션과 대시보드 통계가 모두 삭제되며 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteAllSessionLogs()
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                HeaderSection()
                PermissionStatusCard(
                    permissionStatus = permissionStatus,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenOverlaySettings = onOpenOverlaySettings
                )
                TargetAppCard(
                    permissionStatus = permissionStatus,
                    targetPackages = targetPackages,
                    appDisplayNames = appDisplayNames,
                    onAddTargetPackage = onAddTargetPackage,
                    onRemoveTargetPackage = onRemoveTargetPackage
                )
                TodayStatsCard(dashboardStats)
                WeeklyReportCard(
                    stats = weeklyReportStats,
                    onDeleteAllSessionLogs = { showDeleteConfirmation = true }
                )
                VulnerabilityPatternCard(timeSlotStats)
                RecentSessionsCard(recentSessions, appDisplayNames)
                MvpFlowCard(TeumConstants.mvpFlow)
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "틈",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "무의식적인 앱 사용 사이에 작은 선택의 틈을 만듭니다.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionStatusCard(
    permissionStatus: PermissionStatus,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "권한 상태",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            PermissionStatusRow(
                name = "AccessibilityService",
                status = if (permissionStatus.isAccessibilityEnabled) "활성화됨" else "비활성화됨",
                buttonText = "접근성 설정 열기",
                onClick = onOpenAccessibilitySettings
            )
            PermissionStatusRow(
                name = "Overlay Window",
                status = if (permissionStatus.canDrawOverlays) "허용됨" else "허용되지 않음",
                buttonText = "다른 앱 위에 표시 권한 열기",
                onClick = onOpenOverlaySettings
            )
        }
    }
}

@Composable
private fun PermissionStatusRow(
    name: String,
    status: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Text(text = buttonText)
        }
    }
}

@Composable
private fun TargetAppCard(
    permissionStatus: PermissionStatus,
    targetPackages: Set<String>,
    appDisplayNames: Map<String, String>,
    onAddTargetPackage: (String) -> Unit,
    onRemoveTargetPackage: (String) -> Unit
) {
    var packageNameInput by remember { mutableStateOf("") }
    val normalizedInput = packageNameInput.trim()
    val canRegister = normalizedInput.isNotBlank() && !targetPackages.contains(normalizedInput)
    val canDetectTargetApps = permissionStatus.isAccessibilityEnabled && permissionStatus.canDrawOverlays

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "대상 앱 등록",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!canDetectTargetApps) {
                Text(
                    text = "앱 진입 감지는 두 권한이 켜져야 동작합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = packageNameInput,
                onValueChange = { packageNameInput = it },
                label = { Text("패키지명") },
                singleLine = true,
                supportingText = {
                    Text(
                        text = if (normalizedInput in targetPackages) {
                            "이미 등록된 패키지입니다."
                        } else {
                            "예: com.google.android.youtube"
                        }
                    )
                }
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canRegister,
                onClick = {
                    onAddTargetPackage(normalizedInput)
                    packageNameInput = ""
                }
            ) {
                Text(text = "등록")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onAddTargetPackage("com.google.android.youtube") }
                ) {
                    Text(text = "YouTube")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onAddTargetPackage("com.instagram.android") }
                ) {
                    Text(text = "Instagram")
                }
            }
            TargetPackageList(
                targetPackages = targetPackages,
                appDisplayNames = appDisplayNames,
                onRemoveTargetPackage = onRemoveTargetPackage
            )
        }
    }
}

@Composable
private fun TargetPackageList(
    targetPackages: Set<String>,
    appDisplayNames: Map<String, String>,
    onRemoveTargetPackage: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "등록된 대상",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        if (targetPackages.isEmpty()) {
            Text(
                text = "아직 등록된 패키지가 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            targetPackages.sorted().forEach { packageName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = appDisplayNames[packageName] ?: packageName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { onRemoveTargetPackage(packageName) }) {
                        Text(text = "삭제")
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayStatsCard(stats: DashboardStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "오늘 기록",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            StatRow(label = "세션", value = stats.todaySessionCount.toString())
            StatRow(label = "초과 세션", value = stats.todayOverrunCount.toString())
            StatRow(label = "Fast Reopen", value = stats.todayFastReopenCount.toString())
            StatRow(label = "목적 이탈", value = stats.todayPurposeDriftCount.toString())
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RecentSessionsCard(
    recentSessions: List<SessionLogEntity>,
    appDisplayNames: Map<String, String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "최근 세션",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (recentSessions.isEmpty()) {
                Text(
                    text = "아직 저장된 세션이 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                recentSessions.forEach { session ->
                    RecentSessionItem(
                        session = session,
                        appDisplayName = appDisplayNames[session.packageName] ?: session.packageName
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyReportCard(
    stats: WeeklyReportStats,
    onDeleteAllSessionLogs: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "최근 7일 리포트",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "세션 ${stats.totalSessionCount}회 / 초과 ${stats.overrunCount}회 (${formatPercent(stats.overrunRate)})",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "연장 ${stats.extensionCount}회 / 빠른 재진입 ${stats.fastReopenCount}회 / 목적 이탈 ${formatPercent(stats.purposeDriftRate)} (응답 ${stats.outcomeResponseCount}회)",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "개입 후 실제 종료 ${stats.closedAfterInterventionCount}회",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stats.averageReopenGapMillis?.let { gapMillis ->
                    "평균 재진입 간격 ${formatDuration(gapMillis)}"
                } ?: "평균 재진입 간격: 데이터 없음",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stats.mostVulnerableHourSlot?.let { hourSlot ->
                    "가장 취약한 시간대: ${hourSlot}시"
                } ?: "가장 취약한 시간대: 데이터 없음",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val activeDays = stats.dailyOverrunStats.filter { it.sessionCount > 0 }
            if (activeDays.isNotEmpty()) {
                Text(
                    text = activeDays.joinToString(separator = " · ") { day ->
                        "${day.label} ${day.overrunCount}/${day.sessionCount}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDeleteAllSessionLogs) {
                Text("전체 사용 기록 삭제")
            }
        }
    }
}

@Composable
private fun VulnerabilityPatternCard(timeSlotStats: List<TimeSlotStat>) {
    val activeStats = timeSlotStats.filter { it.openCount > 0 || it.sessionCount > 0 }
    val topStats = activeStats
        .sortedWith(
            compareByDescending<TimeSlotStat> { it.vulnerabilityScore }
                .thenByDescending { it.openCount }
        )
        .take(3)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "\uC2DC\uAC04\uB300\uBCC4 \uCDE8\uC57D \uD328\uD134",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (activeStats.isEmpty()) {
                Text(
                    text = "\uC544\uC9C1 \uBD84\uC11D\uD560 \uC138\uC158 \uAE30\uB85D\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Text(
                text = "\uAC00\uC7A5 \uCDE8\uC57D\uD55C \uC2DC\uAC04\uB300 Top 3",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            topStats.forEach { stat ->
                Text(
                    text = "${stat.hourSlot}\uC2DC: score ${formatScore(stat.vulnerabilityScore)} / \uCD08\uACFC\uC728 ${formatPercent(stat.overrunRate)} / \uC7AC\uC9C4\uC785 ${stat.fastReopenCount}\uD68C${lowDataSuffix(stat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "\uC2DC\uAC04\uB300\uBCC4 \uAC04\uB2E8 \uBAA9\uB85D",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            activeStats.sortedBy { it.hourSlot }.forEach { stat ->
                Text(
                    text = "${stat.hourSlot}\uC2DC: open=${stat.openCount}, overrun=${formatPercent(stat.overrunRate)}, extension=${stat.extensionCount}, reopen=${stat.fastReopenCount}, drift=${stat.purposeDriftCount}, score=${formatScore(stat.vulnerabilityScore)}${lowDataSuffix(stat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentSessionItem(
    session: SessionLogEntity,
    appDisplayName: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = appDisplayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "사용 ${formatDuration(session.durationMillis)} / 목표 ${formatDuration(session.targetDurationMillis)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "intent=${session.intentChoice} outcome=${session.outcomeType ?: "-"} overrun=${session.overrun}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val reopenText = if (session.reopenGapMillis != null) {
            "fastReopen=${session.isFastReopen} gap=${formatDuration(session.reopenGapMillis)}"
        } else {
            "fastReopen=${session.isFastReopen}"
        }
        Text(
            text = reopenText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MvpFlowCard(flowItems: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "MVP 흐름",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            flowItems.forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. $item",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) {
        "${minutes}분 ${seconds}초"
    } else {
        "${seconds}초"
    }
}

private fun formatPercent(rate: Double): String {
    return "${(rate * 100.0).toInt()}%"
}

private fun formatScore(score: Double): String {
    return String.format("%.2f", score)
}

private fun lowDataSuffix(stat: TimeSlotStat): String {
    return if (stat.hasLowData) {
        " / \uB370\uC774\uD130 \uC801\uC74C"
    } else {
        ""
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    TeumTheme {
        DashboardScreen(
            permissionStatus = PermissionStatus(
                isAccessibilityEnabled = false,
                canDrawOverlays = false
            ),
            targetPackages = setOf("com.google.android.youtube"),
            appDisplayNames = mapOf("com.google.android.youtube" to "YouTube"),
            dashboardStats = DashboardStats(),
            recentSessions = emptyList(),
            timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(emptyList()),
            weeklyReportStats = WeeklyReportStats(),
            onOpenAccessibilitySettings = {},
            onOpenOverlaySettings = {},
            onAddTargetPackage = {},
            onRemoveTargetPackage = {},
            onDeleteAllSessionLogs = {}
        )
    }
}
