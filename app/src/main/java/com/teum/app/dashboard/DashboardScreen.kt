package com.teum.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teum.app.core.model.PermissionStatus
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.ui.privacy.PrivacySettingsScreen
import com.teum.app.ui.theme.TeumTheme
import kotlin.math.roundToInt

private val DashboardBorder = Color(0xFFE3E7EF)
private val DashboardDark = Color(0xFF121622)
private val DashboardDarkChip = Color(0xFF232A3A)
private val DashboardMutedBar = Color(0xFFD8DDF0)
private val DashboardPill = Color(0xFFF1F3F7)
private val DashboardSuccess = Color(0xFF2EC4A6)
private val DashboardDanger = Color(0xFFF05D5E)
private val DashboardWarning = Color(0xFFFF9F43)

data class DashboardStats(
    val todaySessionCount: Int = 0,
    val todayOverrunCount: Int = 0,
    val todayFastReopenCount: Int = 0,
    val todayPurposeDriftCount: Int = 0
)

private enum class DashboardTab {
    Home,
    Session,
    Report,
    Settings
}

@Composable
fun DashboardScreen(
    permissionStatus: PermissionStatus,
    targetPackages: Set<String>,
    appDisplayNames: Map<String, String>,
    dashboardStats: DashboardStats,
    recentSessions: List<SessionLogEntity>,
    timeSlotStats: List<TimeSlotStat>,
    weeklyReportStats: WeeklyReportStats,
    availablePackages: Set<String>,
    selectedPackageName: String?,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onAddTargetPackage: (String) -> Unit,
    onRemoveTargetPackage: (String) -> Unit,
    onDeleteAllSessionLogs: () -> Unit,
    onSelectPackage: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.Home) }
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            DashboardBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (
                        tab == DashboardTab.Home ||
                        tab == DashboardTab.Session ||
                        tab == DashboardTab.Report ||
                        tab == DashboardTab.Settings
                    ) {
                        selectedTab = tab
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                DashboardTab.Home -> HomeDashboardContent(
                    permissionStatus = permissionStatus,
                    targetPackages = targetPackages,
                    appDisplayNames = appDisplayNames,
                    dashboardStats = dashboardStats,
                    recentSessions = recentSessions,
                    timeSlotStats = timeSlotStats,
                    weeklyReportStats = weeklyReportStats,
                    availablePackages = availablePackages,
                    selectedPackageName = selectedPackageName,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onAddTargetPackage = onAddTargetPackage,
                    onRemoveTargetPackage = onRemoveTargetPackage,
                    onSelectPackage = onSelectPackage
                )

                DashboardTab.Session -> SessionHistoryContent(
                    targetPackages = targetPackages,
                    appDisplayNames = appDisplayNames,
                    recentSessions = recentSessions,
                    timeSlotStats = timeSlotStats,
                    availablePackages = availablePackages,
                    selectedPackageName = selectedPackageName,
                    onSelectPackage = onSelectPackage
                )

                DashboardTab.Report -> WeeklyReportContent(
                    stats = weeklyReportStats,
                    onDeleteAllSessionLogs = { showDeleteConfirmation = true }
                )

                DashboardTab.Settings -> PrivacySettingsScreen(
                    onDeleteAllClick = { showDeleteConfirmation = true },
                    showBottomNav = false
                )
            }
        }
    }
}

@Composable
private fun HomeDashboardContent(
    permissionStatus: PermissionStatus,
    targetPackages: Set<String>,
    appDisplayNames: Map<String, String>,
    dashboardStats: DashboardStats,
    recentSessions: List<SessionLogEntity>,
    timeSlotStats: List<TimeSlotStat>,
    weeklyReportStats: WeeklyReportStats,
    availablePackages: Set<String>,
    selectedPackageName: String?,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onAddTargetPackage: (String) -> Unit,
    onRemoveTargetPackage: (String) -> Unit,
    onSelectPackage: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 50.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        DashboardHeader(
            title = "오늘의 루프",
            subtitle = "기기 안에서 계산한 오늘의 사용 흐름",
            chip = "로컬"
        )
        HomeMainStatCard(dashboardStats)
        HomeSmallStatsRow(dashboardStats, weeklyReportStats)
        HomeWeakTimeCard(timeSlotStats)
        RecentSessionsCard(
            recentSessions = recentSessions,
            appDisplayNames = appDisplayNames,
            maxItems = 3
        )
        VulnerabilityPatternDetailCard(timeSlotStats)
        AppStatisticsFilterCard(
            packages = targetPackages + availablePackages,
            selectedPackageName = selectedPackageName,
            appDisplayNames = appDisplayNames,
            onSelectPackage = onSelectPackage
        )
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
    }
}

@Composable
private fun SessionHistoryContent(
    targetPackages: Set<String>,
    appDisplayNames: Map<String, String>,
    recentSessions: List<SessionLogEntity>,
    timeSlotStats: List<TimeSlotStat>,
    availablePackages: Set<String>,
    selectedPackageName: String?,
    onSelectPackage: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 50.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        DashboardHeader(
            title = "세션 기록",
            subtitle = "최근 사용 흐름과 앱별 통계를 확인하세요",
            chip = "로컬"
        )
        AppStatisticsFilterCard(
            packages = targetPackages + availablePackages,
            selectedPackageName = selectedPackageName,
            appDisplayNames = appDisplayNames,
            onSelectPackage = onSelectPackage
        )
        RecentSessionsCard(
            recentSessions = recentSessions,
            appDisplayNames = appDisplayNames,
            maxItems = 10
        )
        VulnerabilityPatternDetailCard(timeSlotStats)
    }
}

@Composable
private fun WeeklyReportContent(
    stats: WeeklyReportStats,
    onDeleteAllSessionLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 50.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DashboardHeader(
            title = "이번 주 리포트",
            subtitle = "목적 이탈과 취약 시간대 분석",
            chip = "7일"
        )
        ReportVulnerableTimeCard(stats)
        WeeklyOverrunBars(stats.dailyOverrunStats)
        ReportMetricCard(
            title = "목적 이탈률",
            value = formatPercent(stats.purposeDriftRate),
            description = "릴스·추천 피드 이동 응답 기준",
            color = DashboardDanger
        )
        ReportMetricCard(
            title = "평균 gap time",
            value = stats.averageReopenGapMillis?.let(::formatDuration) ?: "기록 없음",
            description = "빠른 재진입 기준보다 짧은지 확인",
            color = DashboardWarning
        )
        ReportMetricCard(
            title = "개입 후 닫기",
            value = "${stats.closedAfterInterventionCount}회",
            description = "무의식 실행을 중단한 횟수",
            color = DashboardSuccess
        )
        WeeklyReportDetailCard(stats)
        TextButton(
            onClick = onDeleteAllSessionLogs,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "기록 전체 삭제",
                color = DashboardDanger,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    title: String,
    subtitle: String,
    chip: String
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = 56.dp, height = 30.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chip,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HomeMainStatCard(stats: DashboardStats) {
    val keptCount = (stats.todaySessionCount - stats.todayPurposeDriftCount).coerceAtLeast(0)
    val overrunRate = if (stats.todaySessionCount == 0) {
        0
    } else {
        ((stats.todayOverrunCount.toFloat() / stats.todaySessionCount) * 100).roundToInt()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(131.dp)
            .background(DashboardDark, RoundedCornerShape(28.dp))
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column {
            Text(
                text = "목적을 지킨 세션",
                color = Color(0xFFAAB1C3),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${keptCount}회",
                    color = Color.White,
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "오늘 ${stats.todaySessionCount}회 중",
                    color = DashboardSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(width = 82.dp, height = 62.dp)
                .background(DashboardDarkChip, RoundedCornerShape(18.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "초과율",
                color = Color(0xFFAAB1C3),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${overrunRate}%",
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HomeSmallStatsRow(
    dashboardStats: DashboardStats,
    weeklyReportStats: WeeklyReportStats
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        HomeSmallStatCard(
            label = "목적 이탈",
            value = "${dashboardStats.todayPurposeDriftCount}회",
            color = DashboardDanger,
            modifier = Modifier.weight(1f)
        )
        HomeSmallStatCard(
            label = "빠른 재진입",
            value = "${dashboardStats.todayFastReopenCount}회",
            color = DashboardWarning,
            modifier = Modifier.weight(1f)
        )
        HomeSmallStatCard(
            label = "개입 후 닫기",
            value = "${weeklyReportStats.closedAfterInterventionCount}회",
            color = DashboardSuccess,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeSmallStatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = color,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HomeWeakTimeCard(timeSlotStats: List<TimeSlotStat>) {
    val displayHours = listOf(9, 12, 15, 18, 21, 23)
    val scoreByHour = displayHours.associateWith { hour ->
        timeSlotStats.firstOrNull { it.hourSlot == hour }?.vulnerabilityScore ?: 0.0
    }
    val highlightedHour = scoreByHour.maxByOrNull { it.value }?.takeIf { it.value > 0.0 }?.key
        ?: displayHours.last()
    val maxScore = scoreByHour.values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp)
        ) {
            Text(
                text = "주의분산 취약 시간",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${highlightedHour}시대에 초과 세션이 집중됨",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            SimpleBarChart(
                labels = displayHours.map { it.toString().padStart(2, '0') },
                values = displayHours.map { (scoreByHour[it] ?: 0.0) / maxScore },
                highlightedIndex = displayHours.indexOf(highlightedHour),
                barWidth = 40
            )
        }
    }
}

@Composable
private fun VulnerabilityPatternDetailCard(timeSlotStats: List<TimeSlotStat>) {
    val activeStats = timeSlotStats.filter { it.openCount > 0 || it.sessionCount > 0 }
    val topStats = activeStats
        .sortedWith(
            compareByDescending<TimeSlotStat> { it.vulnerabilityScore }
                .thenByDescending { it.openCount }
        )
        .take(3)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "시간대별 취약 패턴",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            if (activeStats.isEmpty()) {
                Text(
                    text = "아직 분석할 세션 기록이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                return@Column
            }

            Text(
                text = "가장 취약한 시간대 Top 3",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            topStats.forEach { stat ->
                DetailLine(
                    text = "${stat.hourSlot}시: 취약도 ${formatScore(stat.vulnerabilityScore)} / 초과율 ${formatPercent(stat.overrunRate)} / 빠른 재진입 ${stat.fastReopenCount}회${lowDataSuffix(stat)}"
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "시간대별 간단 목록",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            activeStats.sortedBy { it.hourSlot }.forEach { stat ->
                DetailLine(
                    text = "${stat.hourSlot}시: 실행 ${stat.openCount}회 / 초과율 ${formatPercent(stat.overrunRate)} / 연장 ${stat.extensionCount}회 / 빠른 재진입 ${stat.fastReopenCount}회 / 목적 이탈 ${stat.purposeDriftCount}회 / 취약도 ${formatScore(stat.vulnerabilityScore)}${lowDataSuffix(stat)}"
                )
            }
        }
    }
}

@Composable
private fun RecentSessionsCard(
    recentSessions: List<SessionLogEntity>,
    appDisplayNames: Map<String, String>,
    maxItems: Int
) {
    val displayedSessions = recentSessions.take(maxItems)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = if (recentSessions.isEmpty()) {
                    "최근 세션"
                } else {
                    "최근 세션 · 최근 ${displayedSessions.size}개"
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (recentSessions.isEmpty()) {
                Text(
                    text = "아직 저장된 세션이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            } else {
                displayedSessions.forEach { session ->
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
private fun RecentSessionItem(
    session: SessionLogEntity,
    appDisplayName: String
) {
    val overrunMillis = (session.durationMillis - session.targetDurationMillis).coerceAtLeast(0L)
    val labelColor = when {
        session.purposeDrifted == true -> DashboardDanger
        session.closedAfterIntervention == true -> MaterialTheme.colorScheme.primary
        session.overrun -> DashboardWarning
        else -> DashboardSuccess
    }
    val pillText = when {
        session.purposeDrifted == true -> "목적 이탈"
        session.closedAfterIntervention == true -> "성공"
        session.overrun -> "초과"
        else -> "필요 사용"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(labelColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appDisplayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${SessionDisplayText.intent(session.intentChoice)} → ${formatDuration(session.durationMillis)} 사용",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            StatusPill(text = pillText, color = labelColor)
        }
        Column(
            modifier = Modifier.padding(start = 26.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DetailLine(
                text = "${formatDuration(session.durationMillis)} 사용 · 목표 ${formatDuration(session.targetDurationMillis)}",
                fontSize = 11
            )
            DetailLine(
                text = "사용 목적: ${SessionDisplayText.intent(session.intentChoice)}",
                fontSize = 11
            )
            DetailLine(
                text = "사용 결과: ${SessionDisplayText.outcome(session.outcomeType, session.outcomeAchieved, session.purposeDrifted)}",
                fontSize = 11
            )
            DetailLine(
                text = if (session.overrun) {
                    "목표 시간을 ${formatDuration(overrunMillis)} 초과했어요"
                } else {
                    "목표 시간 안에 종료했어요"
                },
                fontSize = 11
            )
            if (session.extensionCount > 0) {
                DetailLine(
                    text = "사용 시간을 ${session.extensionCount}회 연장했어요",
                    fontSize = 11
                )
            }
            DetailLine(
                text = if (session.isFastReopen) {
                    session.reopenGapMillis?.let { "${formatDuration(it)} 만에 다시 열었어요" }
                        ?: "앱을 빠르게 다시 열었어요"
                } else {
                    "빠른 재진입 없이 시작했어요"
                },
                fontSize = 11
            )
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 70.dp, height = 26.dp)
            .background(DashboardPill, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReportVulnerableTimeCard(stats: WeeklyReportStats) {
    val hourSlot = stats.mostVulnerableHourSlot
    val timeRange = hourSlot?.let(::formatHourRange) ?: "데이터 없음"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 27.dp)
        ) {
            Text(
                text = "주의분산 취약 시간대",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = timeRange,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "초과율 ${formatPercent(stats.overrunRate)} · 빠른 재진입 ${stats.fastReopenCount}회",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun WeeklyOverrunBars(dailyStats: List<DailyOverrunStat>) {
    val labels = listOf("월", "화", "수", "목", "금", "토", "일")
    val statsByLabel = dailyStats.associateBy { it.label }
    val values = labels.map { label -> statsByLabel[label]?.overrunCount ?: 0 }
    val maxValue = values.maxOrNull()?.takeIf { it > 0 } ?: 1
    val highlightedIndex = values.indexOf(values.maxOrNull() ?: 0).coerceAtLeast(0)

    Column {
        Text(
            text = "요일별 초과 세션",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        SimpleBarChart(
            labels = labels,
            values = values.map { it.toDouble() / maxValue },
            highlightedIndex = highlightedIndex,
            barWidth = 33
        )
    }
}

@Composable
private fun SimpleBarChart(
    labels: List<String>,
    values: List<Double>,
    highlightedIndex: Int,
    barWidth: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        labels.forEachIndexed { index, label ->
            val normalizedValue = values.getOrNull(index)?.coerceIn(0.0, 1.0) ?: 0.0
            val barHeight = (8 + normalizedValue * 47).dp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(width = barWidth.dp, height = barHeight)
                        .background(
                            color = if (index == highlightedIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                DashboardMutedBar
                            },
                            shape = RoundedCornerShape(5.dp)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    value: String,
    description: String,
    color: Color
) {
    val valueFontSize = if (value.length > 5) 18.sp else 22.sp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 86.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.11f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(color, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(
                modifier = Modifier.weight(0.95f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    color = color,
                    fontSize = valueFontSize,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeeklyReportDetailCard(stats: WeeklyReportStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = "주간 상세 요약",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            DetailLine(
                text = "세션 ${stats.totalSessionCount}회 / 초과 ${stats.overrunCount}회 (${formatPercent(stats.overrunRate)})"
            )
            DetailLine(
                text = "연장 ${stats.extensionCount}회 / 빠른 재진입 ${stats.fastReopenCount}회 / 목적 이탈 ${formatPercent(stats.purposeDriftRate)} (응답 ${stats.outcomeResponseCount}회)"
            )
            DetailLine(
                text = "개입 후 실제 종료 ${stats.closedAfterInterventionCount}회"
            )
            DetailLine(
                text = stats.averageReopenGapMillis?.let { gapMillis ->
                    "평균 재진입 간격 ${formatDuration(gapMillis)}"
                } ?: "평균 재진입 간격: 데이터 없음"
            )
            DetailLine(
                text = stats.mostVulnerableHourSlot?.let { hourSlot ->
                    "가장 취약한 시간대: ${hourSlot}시"
                } ?: "가장 취약한 시간대: 데이터 없음"
            )
            val activeDays = stats.dailyOverrunStats.filter { it.sessionCount > 0 }
            if (activeDays.isNotEmpty()) {
                DetailLine(
                    text = activeDays.joinToString(separator = " · ") { day ->
                        "${day.label} ${day.overrunCount}/${day.sessionCount}"
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailLine(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 12
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 5).sp,
        modifier = modifier
    )
}

@Composable
private fun DashboardBottomNavigation(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp))
                .padding(horizontal = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardNavItem("⌂", "홈", selectedTab == DashboardTab.Home) {
                onTabSelected(DashboardTab.Home)
            }
            DashboardNavItem("●", "세션", selectedTab == DashboardTab.Session) {
                onTabSelected(DashboardTab.Session)
            }
            DashboardNavItem("▤", "리포트", selectedTab == DashboardTab.Report) {
                onTabSelected(DashboardTab.Report)
            }
            DashboardNavItem("⚙", "설정", selectedTab == DashboardTab.Settings) {
                onTabSelected(DashboardTab.Settings)
            }
        }
    }
}

@Composable
private fun DashboardNavItem(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .width(48.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "권한 상태",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            PermissionStatusRow(
                name = "접근성 서비스",
                status = if (permissionStatus.isAccessibilityEnabled) "활성화됨" else "비활성화 필요",
                buttonText = "접근성 설정 열기",
                onClick = onOpenAccessibilitySettings
            )
            PermissionStatusRow(
                name = "오버레이 권한",
                status = if (permissionStatus.canDrawOverlays) "허용됨" else "허용 필요",
                buttonText = "다른 앱 위 표시 설정 열기",
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = status,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "관리 앱 빠른 등록",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (!canDetectTargetApps) {
                Text(
                    text = "앱 진입 감지는 두 권한이 켜져야 동작합니다.",
                    fontSize = 12.sp,
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
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "등록")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onAddTargetPackage("com.google.android.youtube") },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "YouTube")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onAddTargetPackage("com.instagram.android") },
                    shape = RoundedCornerShape(16.dp)
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        if (targetPackages.isEmpty()) {
            Text(
                text = "아직 등록된 패키지가 없습니다.",
                fontSize = 12.sp,
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
                        fontSize = 13.sp
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
private fun AppStatisticsFilterCard(
    packages: Set<String>,
    selectedPackageName: String?,
    appDisplayNames: Map<String, String>,
    onSelectPackage: (String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "통계 앱 선택",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "선택한 앱의 오늘 통계와 최근 7일 분석을 보여드려요.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPackageName == null,
                    onClick = { onSelectPackage(null) },
                    label = { Text("전체") }
                )
                packages.sortedBy { appDisplayNames[it] ?: it }.forEach { packageName ->
                    FilterChip(
                        selected = selectedPackageName == packageName,
                        onClick = { onSelectPackage(packageName) },
                        label = { Text(appDisplayNames[packageName] ?: packageName) }
                    )
                }
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
    return "${(rate * 100.0).roundToInt()}%"
}

private fun formatScore(score: Double): String {
    return String.format("%.2f", score)
}

private fun formatHourRange(hourSlot: Int): String {
    val start = hourSlot.mod(24).toString().padStart(2, '0')
    val end = (hourSlot + 2).mod(24).toString().padStart(2, '0')
    return "$start:00 - $end:00"
}

private fun lowDataSuffix(stat: TimeSlotStat): String {
    return if (stat.hasLowData) {
        " / 데이터 적음"
    } else {
        ""
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
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
            dashboardStats = DashboardStats(
                todaySessionCount = 10,
                todayOverrunCount = 2,
                todayFastReopenCount = 1,
                todayPurposeDriftCount = 2
            ),
            recentSessions = emptyList(),
            timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(emptyList()),
            weeklyReportStats = WeeklyReportStats(
                totalSessionCount = 24,
                overrunCount = 8,
                overrunRate = 0.62,
                fastReopenCount = 8,
                purposeDriftRate = 0.31,
                closedAfterInterventionCount = 12,
                averageReopenGapMillis = 124_000L,
                mostVulnerableHourSlot = 23,
                dailyOverrunStats = listOf(
                    DailyOverrunStat(1, "월", 3, 2),
                    DailyOverrunStat(2, "화", 2, 1),
                    DailyOverrunStat(3, "수", 4, 3),
                    DailyOverrunStat(4, "목", 3, 2),
                    DailyOverrunStat(5, "금", 5, 4),
                    DailyOverrunStat(6, "토", 6, 5),
                    DailyOverrunStat(7, "일", 4, 3)
                )
            ),
            availablePackages = setOf("com.google.android.youtube"),
            selectedPackageName = null,
            onOpenAccessibilitySettings = {},
            onOpenOverlaySettings = {},
            onAddTargetPackage = {},
            onRemoveTargetPackage = {},
            onDeleteAllSessionLogs = {},
            onSelectPackage = {}
        )
    }
}
