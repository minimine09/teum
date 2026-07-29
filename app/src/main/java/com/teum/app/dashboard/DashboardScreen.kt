package com.teum.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teum.app.core.model.InterventionMode
import com.teum.app.core.model.PermissionStatus
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.ui.privacy.PrivacySettingsScreen
import com.teum.app.ui.target.TargetAppSelectionScreen
import com.teum.app.ui.target.TargetAppInstalledApp
import com.teum.app.ui.theme.TeumTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val DashboardBorder = Color(0xFFE3E7EF)
private val DashboardDark = Color(0xFF121622)
private val DashboardDarkChip = Color(0xFF232A3A)
private val DashboardMutedBar = Color(0xFFD8DDF0)
private val DashboardPill = Color(0xFFF1F3F7)
private val DashboardSuccess = Color(0xFF2EC4A6)
private val DashboardDanger = Color(0xFFF05D5E)
private val DashboardWarning = Color(0xFFFF9F43)
private val WeeklyChartCardHeight = 220.dp

data class DashboardStats(
    val todaySessionCount: Int = 0,
    val todayOverrunCount: Int = 0,
    val todayFastReopenCount: Int = 0,
    val todayPurposeKeptCount: Int = 0,
    val todayPurposeDriftCount: Int = 0,
    val todayClosedAfterInterventionCount: Int = 0
)

private enum class DashboardTab {
    Home,
    Session,
    Report,
    Settings,
    TargetApps
}

private enum class RecentSessionDisplayMode {
    Compact,
    Detailed
}

@Composable
fun DashboardScreen(
    permissionStatus: PermissionStatus,
    targetPackages: Set<String>,
    appDisplayNames: Map<String, String>,
    dashboardStats: DashboardStats,
    recentSessions: List<SessionLogEntity>,
    sessionRecentSessions: List<SessionLogEntity>,
    timeSlotStats: List<TimeSlotStat>,
    weeklyReportStats: WeeklyReportStats,
    availablePackages: Set<String>,
    installedApps: List<TargetAppInstalledApp> = emptyList(),
    selectedPackageName: String?,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRecoverPermissionsClick: () -> Unit,
    onAddTargetPackage: (String) -> Unit,
    onRemoveTargetPackage: (String) -> Unit,
    onDeleteAllSessionLogs: () -> Unit,
    onSelectPackage: (String?) -> Unit,
    selectedInterventionMode: InterventionMode = InterventionMode.NORMAL,
    onInterventionModeChange: (InterventionMode) -> Unit = {},
    showVulnerableDebugOverride: Boolean = false,
    forceVulnerableNowForDebug: Boolean = false,
    onForceVulnerableNowForDebugChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.Home) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPackageName, targetPackages) {
        if (selectedPackageName != null && selectedPackageName !in targetPackages) {
            onSelectPackage(null)
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("사용 기록을 삭제할까요?") },
            text = { Text("저장된 사용 기록과 통계가 모두 삭제되며 복구할 수 없습니다.") },
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
                    appDisplayNames = appDisplayNames,
                    dashboardStats = dashboardStats,
                    recentSessions = recentSessions,
                    timeSlotStats = timeSlotStats,
                    weeklyReportStats = weeklyReportStats,
                    onRecoverPermissionsClick = onRecoverPermissionsClick
                )

                DashboardTab.Session -> SessionHistoryContent(
                    targetPackages = targetPackages,
                    appDisplayNames = appDisplayNames,
                    recentSessions = sessionRecentSessions,
                    availablePackages = availablePackages,
                    selectedPackageName = selectedPackageName,
                    onSelectPackage = onSelectPackage
                )

                DashboardTab.Report -> WeeklyReportContent(
                    stats = weeklyReportStats,
                    appDisplayNames = appDisplayNames
                )

                DashboardTab.Settings -> PrivacySettingsScreen(
                    selectedMode = selectedInterventionMode,
                    onModeChange = onInterventionModeChange,
                    showVulnerableDebugOverride = showVulnerableDebugOverride,
                    forceVulnerableNowForDebug = forceVulnerableNowForDebug,
                    onForceVulnerableNowForDebugChange = onForceVulnerableNowForDebugChange,
                    permissionStatus = permissionStatus,
                    onPermissionSettingsClick = onRecoverPermissionsClick,
                    onManageTargetAppsClick = { selectedTab = DashboardTab.TargetApps },
                    onDeleteAllClick = { showDeleteConfirmation = true },
                    showBottomNav = false
                )

                DashboardTab.TargetApps -> TargetAppSelectionScreen(
                    initialSelectedPackages = targetPackages,
                    installedApps = installedApps,
                    compactForBottomNav = true,
                    onCompleteClick = { results ->
                        results.forEach { result ->
                            if (result.enabled) {
                                onAddTargetPackage(result.packageName)
                            } else {
                                onRemoveTargetPackage(result.packageName)
                            }
                        }
                        selectedTab = DashboardTab.Settings
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeDashboardContent(
    permissionStatus: PermissionStatus,
    appDisplayNames: Map<String, String>,
    dashboardStats: DashboardStats,
    recentSessions: List<SessionLogEntity>,
    timeSlotStats: List<TimeSlotStat>,
    weeklyReportStats: WeeklyReportStats,
    onRecoverPermissionsClick: () -> Unit
) {
    val hasRequiredPermissions = permissionStatus.isAccessibilityEnabled &&
        permissionStatus.canDrawOverlays

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 50.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        DashboardHeader(
            title = "오늘의 사용",
            subtitle = "오늘의 앱 사용 흐름을 확인하세요"
        )
        if (!hasRequiredPermissions) {
            PermissionRecoveryCard(onClick = onRecoverPermissionsClick)
        }
        HomeMainStatCard(dashboardStats)
        HomeSmallStatsRow(dashboardStats)
        RecentSessionsCard(
            recentSessions = recentSessions,
            appDisplayNames = appDisplayNames,
            maxItems = 3,
            displayMode = RecentSessionDisplayMode.Compact
        )
    }
}

@Composable
private fun PermissionRecoveryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E4)),
        border = BorderStroke(1.dp, DashboardWarning.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "틈이 현재 일시 중지되어 있어요",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "필수 권한이 꺼져 있어 앱 사용 감지와 확인 화면이 동작하지 않습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DashboardWarning)
            ) {
                Text(
                    text = "권한 다시 설정",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SessionHistoryContent(
    targetPackages: Set<String>,
    appDisplayNames: Map<String, String>,
    recentSessions: List<SessionLogEntity>,
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
            title = "사용 기록",
            subtitle = "앱별 사용 내역을 확인하세요"
        )
        AppStatisticsFilterCard(
            packages = targetPackages,
            selectedPackageName = selectedPackageName,
            appDisplayNames = appDisplayNames,
            onSelectPackage = onSelectPackage
        )
        SessionHistorySummaryCard(
            recentSessions = recentSessions,
            selectedPackageName = selectedPackageName,
            appDisplayNames = appDisplayNames
        )
        RecentSessionsCard(
            recentSessions = recentSessions,
            appDisplayNames = appDisplayNames,
            maxItems = 10,
            displayMode = RecentSessionDisplayMode.Detailed
        )
    }
}

@Composable
private fun WeeklyReportContent(
    stats: WeeklyReportStats,
    appDisplayNames: Map<String, String>
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
            subtitle = "이번 주 사용 패턴을 정리했어요"
        )
        ReportVulnerableTimeCard(stats)
        WeeklyReportCharts(
            stats = stats,
            appDisplayNames = appDisplayNames
        )
        ReportMetricCard(
            title = "목적 이탈률",
            value = formatPercent(stats.purposeDriftRate),
            description = "처음 목적과 달라진 비율",
            color = DashboardDanger
        )
        ReportMetricCard(
            title = "총 사용 시간",
            value = formatDuration(stats.dailyOverrunStats.sumOf { it.usageMillis }),
            description = "이번 주 관리 앱을 사용한 시간",
            color = DashboardWarning
        )
        ReportMetricCard(
            title = "총 연장 횟수",
            value = "${stats.extensionCount}회",
            description = "목표 시간 이후 더 사용한 횟수",
            color = DashboardSuccess
        )
        WeeklyReportDetailCard(stats)
    }
}

@Composable
private fun SessionHistorySummaryCard(
    recentSessions: List<SessionLogEntity>,
    selectedPackageName: String?,
    appDisplayNames: Map<String, String>
) {
    val filteredLabel = selectedPackageName?.let { appDisplayNames[it] ?: it } ?: "전체 앱"
    val overrunCount = recentSessions.count { SessionMetricsResolver.resolve(it).isOverrun }
    val driftCount = recentSessions.count { it.purposeDrifted == true }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = filteredLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "최근 최대 10개 기록 요약",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SessionSummaryMiniStat(
                    label = "사용 세션",
                    value = "${recentSessions.size}회",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                SessionSummaryMiniStat(
                    label = "목표 초과",
                    value = "${overrunCount}회",
                    color = DashboardWarning,
                    modifier = Modifier.weight(1f)
                )
                SessionSummaryMiniStat(
                    label = "목적 이탈",
                    value = "${driftCount}회",
                    color = DashboardDanger,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SessionSummaryMiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DashboardHeader(
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
}

@Composable
private fun HomeMainStatCard(stats: DashboardStats) {
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
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                text = "목적대로 사용한 횟수",
                color = Color(0xFFAAB1C3),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${stats.todayPurposeKeptCount}회",
                    color = Color.White,
                    fontSize = 36.sp,
                    lineHeight = 44.sp,
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
    dashboardStats: DashboardStats
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
            label = "개입 후 종료",
            value = "${dashboardStats.todayClosedAfterInterventionCount}회",
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
        modifier = modifier.heightIn(min = 82.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = color,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    val highlightedHour = VulnerableTimeSelector.rankVulnerableSlots(timeSlotStats)
        .firstOrNull { stat -> stat.hourSlot in displayHours }
        ?.hourSlot
        ?: displayHours.last()
    val maxScore = scoreByHour.values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 162.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp)
        ) {
            Text(
                text = "자주 흔들린 시간",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${highlightedHour}시쯤 사용이 길어졌어요",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            SimpleBarChart(
                labels = displayHours.map { it.toString().padStart(2, '0') },
                values = displayHours.map { (scoreByHour[it] ?: 0.0) / maxScore },
                highlightedIndices = displayHours.indexOf(highlightedHour)
                    .takeIf { it >= 0 }
                    ?.let(::setOf)
                    .orEmpty(),
                barWidth = 40
            )
        }
    }
}

@Composable
private fun VulnerabilityPatternDetailCard(timeSlotStats: List<TimeSlotStat>) {
    val activeStats = timeSlotStats.filter { it.openCount > 0 || it.sessionCount > 0 }
    val topStats = VulnerableTimeSelector.rankVulnerableSlots(timeSlotStats).take(3)

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
                text = "자주 흔들린 시간",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            if (activeStats.isEmpty()) {
                Text(
                    text = "아직 분석할 사용 기록이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                return@Column
            }

            Text(
                text = "가장 자주 흔들린 시간 Top 3",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            topStats.forEach { stat ->
                DetailLine(
                    text = "${stat.hourSlot}시: 초과율 ${formatPercent(stat.overrunRate)} / 빠른 재진입 ${stat.fastReopenCount}회${lowDataSuffix(stat)}"
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
                    text = "${stat.hourSlot}시: 실행 ${stat.openCount}회 / 초과율 ${formatPercent(stat.overrunRate)} / 연장 ${stat.extensionCount}회 / 빠른 재진입 ${stat.fastReopenCount}회 / 목적 이탈 ${stat.purposeDriftCount}회${lowDataSuffix(stat)}"
                )
            }
        }
    }
}

@Composable
private fun RecentSessionsCard(
    recentSessions: List<SessionLogEntity>,
    appDisplayNames: Map<String, String>,
    maxItems: Int,
    displayMode: RecentSessionDisplayMode
) {
    val displayedSessions = recentSessions.take(maxItems)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "최근 사용",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (recentSessions.isNotEmpty()) {
                    Text(
                        text = "최근 ${displayedSessions.size}개",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (recentSessions.isEmpty()) {
                Text(
                    text = "아직 저장된 사용 기록이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            } else {
                displayedSessions.forEach { session ->
                    RecentSessionItem(
                        session = session,
                        appDisplayName = session.appDisplayName
                            ?.takeIf { it.isNotBlank() }
                            ?: appDisplayNames[session.packageName]
                            ?: session.packageName,
                        displayMode = displayMode
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSessionItem(
    session: SessionLogEntity,
    appDisplayName: String,
    displayMode: RecentSessionDisplayMode
) {
    val metrics = SessionMetricsResolver.resolve(session)
    val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    val sessionTimeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    val intentColor = when (session.intentChoice) {
        "CLEAR_PURPOSE" -> MaterialTheme.colorScheme.primary
        "MINDFUL_REST" -> DashboardSuccess
        "UNCONSCIOUS_OPEN" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> secondaryTextColor
    }
    val outcomeStatus = if (session.intentChoice == "CLEAR_PURPOSE") {
        when {
            session.outcomeType == "PURPOSE_ACHIEVED" || session.outcomeAchieved == true ->
                "목적 달성" to DashboardSuccess
            session.outcomeType == "NECESSARY_USE" ->
                "필요한 사용" to MaterialTheme.colorScheme.primary
            session.outcomeType == "CONTINUED_SCROLLING" ->
                "무의식 사용 지속" to DashboardWarning
            session.outcomeType == "PURPOSE_DRIFT" || session.purposeDrifted == true ->
                "목적 이탈" to DashboardDanger
            displayMode == RecentSessionDisplayMode.Detailed ->
                "결과 미확인" to secondaryTextColor
            else ->
                null
        }
    } else {
        null
    }
    val overrunStatus = if (metrics.isOverrun) {
        SessionDisplayText.compactOverrun(metrics.overrunMillis) to DashboardWarning
    } else {
        null
    }
    val reopenStatus = if (session.isFastReopen) {
        val text = session.reopenGapMillis?.let {
            "${formatDuration(it)} 만에 빠른 재진입"
        } ?: "빠른 재진입"
        text to MaterialTheme.colorScheme.primary
    } else {
        null
    }
    val interventionPolicyStatus = SessionPolicyDisplayText.status(
        modeAtStart = session.modeAtStart,
        interventionAppliedAtStart = session.interventionAppliedAtStart
    )?.takeIf {
        session.interventionAppliedAtStart
    }?.let {
        "조심 모드 개입 적용" to DashboardWarning
    }
    val sessionStatuses = buildList {
        outcomeStatus?.let(::add)
        overrunStatus?.let(::add)
        reopenStatus?.let(::add)
        interventionPolicyStatus?.let(::add)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SessionIntentDot(intentColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    RecentSessionAppName(
                        appDisplayName = appDisplayName,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = formatSessionStartedAt(session.startedAtMillis),
                        color = sessionTimeColor,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                RecentSessionIntentText(
                    text = SessionDisplayText.intent(session.intentChoice),
                    color = intentColor,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatDuration(metrics.usageMillis)} 사용",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "목표 ${formatDuration(metrics.targetMillis)}",
                    color = secondaryTextColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (sessionStatuses.isNotEmpty()) {
                Text(
                    text = buildAnnotatedString {
                        sessionStatuses.forEachIndexed { index, (text, color) ->
                            if (index > 0) {
                                withStyle(SpanStyle(color = secondaryTextColor)) {
                                    append(" · ")
                                }
                            }
                            withStyle(
                                SpanStyle(
                                    color = color,
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                append(text)
                            }
                        }
                    },
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    maxLines = if (displayMode == RecentSessionDisplayMode.Compact) 2 else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (displayMode == RecentSessionDisplayMode.Detailed && session.closedAfterIntervention == true) {
                Text(
                    text = "자기점검 후 앱을 종료했어요",
                    color = secondaryTextColor,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            if (displayMode == RecentSessionDisplayMode.Detailed && metrics.extensionCount > 0) {
                Text(
                    text = "사용 시간을 ${metrics.extensionCount}회 연장했어요",
                    color = secondaryTextColor,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SessionIntentDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun RecentSessionAppName(
    appDisplayName: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = appDisplayName,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RecentSessionIntentText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Start
    )
}

@Composable
private fun ReportVulnerableTimeCard(stats: WeeklyReportStats) {
    val timeSlotStat = stats.mostVulnerableTimeSlotStat
    val timeRange = timeSlotStat?.hourSlot?.let(VulnerableTimeDisplayText::hourRange)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Text(
                text = "자주 흔들린 시간",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (timeSlotStat == null || timeRange == null) {
                Text(
                    text = if (stats.hasEnoughVulnerableTimeData) {
                        "뚜렷한 취약 시간대가 없어요"
                    } else {
                        "아직 분석할 기록이 부족해요"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "사용 기록이 쌓이면 자주 흔들리는 시간을 알려드릴게요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            } else {
                Text(
                    text = timeRange,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "초과율 ${formatPercent(timeSlotStat.overrunRate)} · " +
                        "빠른 재진입 ${timeSlotStat.fastReopenCount}회",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "이 시간대에는 사용 시간을 조금 짧게 잡아보세요.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WeeklyReportCharts(
    stats: WeeklyReportStats,
    appDisplayNames: Map<String, String>
) {
    val dayStats = stats.dailyOverrunStats
    val maxOpenCount = dayStats.maxOfOrNull { it.openCount }?.takeIf { it > 0 } ?: 1
    val maxExtensionCount = dayStats.maxOfOrNull { it.extensionCount }?.takeIf { it > 0 } ?: 1
    val maxUsageMillis = dayStats.maxOfOrNull { it.usageMillis }?.takeIf { it > 0L } ?: 1L
    val pagerState = rememberPagerState(pageCount = { 4 })

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 0.dp
        ) { page ->
            when (page) {
                0 -> WeeklyBarChartCard(
                    title = "요일별 앱 사용 횟수",
                    labels = dayStats.map { it.label },
                    values = dayStats.map { it.openCount.toDouble() / maxOpenCount },
                    valueLabels = dayStats.map { stat ->
                        stat.openCount.takeIf { it > 0 }?.let(::formatCompactCount).orEmpty()
                    },
                    highlightedIndices = dayStats.indicesOfMaxInt { it.openCount },
                    barColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )

                1 -> WeeklyBarChartCard(
                    title = "요일별 연장 횟수",
                    labels = dayStats.map { it.label },
                    values = dayStats.map { it.extensionCount.toDouble() / maxExtensionCount },
                    valueLabels = dayStats.map { stat ->
                        stat.extensionCount.takeIf { it > 0 }?.let(::formatCompactCount).orEmpty()
                    },
                    highlightedIndices = dayStats.indicesOfMaxInt { it.extensionCount },
                    barColor = DashboardWarning,
                    modifier = Modifier.fillMaxWidth()
                )

                2 -> WeeklyBarChartCard(
                    title = "요일별 사용 시간",
                    labels = dayStats.map { it.label },
                    values = dayStats.map { it.usageMillis.toDouble() / maxUsageMillis.toDouble() },
                    valueLabels = dayStats.map { stat ->
                        stat.usageMillis.takeIf { it > 0L }?.let(::formatCompactChartDuration).orEmpty()
                    },
                    highlightedIndices = dayStats.indicesOfMaxLong { it.usageMillis },
                    barColor = DashboardSuccess,
                    modifier = Modifier.fillMaxWidth()
                )

                3 -> WeeklyAppUsageChartCard(
                    appUsageStats = stats.appUsageStats,
                    appDisplayNames = appDisplayNames,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        ChartPageIndicator(
            pageCount = 4,
            activePage = pagerState.currentPage
        )
    }
}

@Composable
private fun WeeklyBarChartCard(
    title: String,
    labels: List<String>,
    values: List<Double>,
    valueLabels: List<String> = emptyList(),
    highlightedIndices: Set<Int>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(WeeklyChartCardHeight),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
            SimpleBarChart(
                labels = labels,
                values = values,
                valueLabels = valueLabels,
                highlightedIndices = highlightedIndices,
                barWidth = 28,
                highlightedColor = barColor
            )
        }
    }
}

@Composable
private fun WeeklyAppUsageChartCard(
    appUsageStats: List<AppUsageStat>,
    appDisplayNames: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val usageSlices = remember(appUsageStats, appDisplayNames) {
        buildAppUsageSlices(
            appUsageStats = appUsageStats,
            appDisplayNames = appDisplayNames
        )
    }
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        DashboardSuccess,
        DashboardWarning,
        DashboardDanger,
        Color(0xFF7C83F7),
        Color(0xFF9AA3B8)
    )

    Card(
        modifier = modifier.height(WeeklyChartCardHeight),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "앱별 사용시간",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (usageSlices.isEmpty()) {
                        Text(
                            text = "앱별 기록이 아직 없어요.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    } else {
                        usageSlices.forEachIndexed { index, slice ->
                            ChartLegendRow(
                                color = colors[index % colors.size],
                                label = slice.label,
                                value = "${formatCompactChartDuration(slice.usageMillis)} · ${formatPercent(slice.ratio)}"
                            )
                            if (index != usageSlices.lastIndex) {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                PieChart(
                    values = usageSlices.map { it.usageMillis.toFloat() },
                    colors = colors,
                    modifier = Modifier.size(96.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartPageIndicator(
    pageCount: Int,
    activePage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(
                        width = if (index == activePage) 16.dp else 6.dp,
                        height = 6.dp
                    )
                    .background(
                        color = if (index == activePage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            DashboardMutedBar
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ChartLegendRow(
    color: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1.05f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PieChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = values.sum().takeIf { it > 0f } ?: 1f
    Canvas(modifier = modifier) {
        var startAngle = -90f
        values.forEachIndexed { index, value ->
            val sweepAngle = 360f * (value / total)
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += sweepAngle
        }
        if (values.isEmpty()) {
            drawCircle(
                color = DashboardMutedBar,
            )
        }
    }
}

private data class AppUsageSlice(
    val label: String,
    val usageMillis: Long,
    val ratio: Double
)

private fun buildAppUsageSlices(
    appUsageStats: List<AppUsageStat>,
    appDisplayNames: Map<String, String>
): List<AppUsageSlice> {
    val sortedStats = appUsageStats
        .filter { it.usageMillis > 0L }
        .sortedByDescending { it.usageMillis }
    val totalUsageMillis = sortedStats.sumOf { it.usageMillis }
    if (totalUsageMillis <= 0L) return emptyList()

    val topStats = sortedStats.take(4)
    val otherUsageMillis = sortedStats.drop(4).sumOf { it.usageMillis }
    val displayStats = topStats.map { stat ->
        (
            stat.appDisplayName
                ?.takeIf { it.isNotBlank() }
                ?: appDisplayNames[stat.packageName].orEmpty()
                    .ifBlank { stat.packageName }
            ) to stat.usageMillis
    } + if (otherUsageMillis > 0L) {
        listOf("기타" to otherUsageMillis)
    } else {
        emptyList()
    }

    return displayStats.map { (label, usageMillis) ->
        AppUsageSlice(
            label = label,
            usageMillis = usageMillis,
            ratio = usageMillis.toDouble() / totalUsageMillis.toDouble()
        )
    }
}

@Composable
private fun SimpleBarChart(
    labels: List<String>,
    values: List<Double>,
    valueLabels: List<String> = emptyList(),
    highlightedIndices: Set<Int>,
    barWidth: Int,
    highlightedColor: Color = MaterialTheme.colorScheme.primary
) {
    val showValueLabels = valueLabels.any { it.isNotBlank() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        labels.forEachIndexed { index, label ->
            val normalizedValue = values.getOrNull(index)?.coerceIn(0.0, 1.0) ?: 0.0
            val barHeight = if (normalizedValue <= 0.0) 0.dp else (8 + normalizedValue * 72).dp
            val isHighlighted = index in highlightedIndices
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showValueLabels) {
                    Text(
                        text = valueLabels.getOrNull(index).orEmpty(),
                        color = if (isHighlighted) highlightedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }
                Box(
                    modifier = Modifier
                        .size(width = barWidth.dp, height = barHeight)
                        .background(
                            color = if (isHighlighted) {
                                highlightedColor
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
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun List<DailyOverrunStat>.indicesOfMaxInt(
    selector: (DailyOverrunStat) -> Int
): Set<Int> {
    val values = map(selector)
    val maxValue = values.maxOrNull()?.takeIf { it > 0 } ?: return emptySet()
    return values.mapIndexedNotNull { index, value ->
        index.takeIf { value == maxValue }
    }.toSet()
}

private fun List<DailyOverrunStat>.indicesOfMaxLong(
    selector: (DailyOverrunStat) -> Long
): Set<Int> {
    val values = map(selector)
    val maxValue = values.maxOrNull()?.takeIf { it > 0L } ?: return emptySet()
    return values.mapIndexedNotNull { index, value ->
        index.takeIf { value == maxValue }
    }.toSet()
}

private fun formatCompactChartDuration(millis: Long): String {
    val seconds = millis / 1_000L
    if (seconds <= 0L) return ""

    val minutes = seconds / 60L
    if (minutes <= 0L) return "${seconds}초"

    val hours = minutes / 60L
    if (hours <= 0L) return "${minutes}분"

    val days = hours / 24L
    return if (days > 0L) {
        "${days}일+"
    } else {
        "${hours}시간"
    }
}

private fun formatCompactCount(count: Int): String {
    if (count <= 0) return ""
    return if (count >= 1_000) {
        "${count / 1_000}천+"
    } else {
        "${count}회"
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    value: String,
    description: String,
    color: Color
) {
    val valueFontSize = if (value.length >= 5) 18.sp else 24.sp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DashboardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.11f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        color = color,
                        fontSize = valueFontSize,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = description,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "주간 상세 요약",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            WeeklySummaryRow(
                label = "목표 시간 초과",
                value = "${stats.overrunCount}회",
                subValue = "전체 사용 ${stats.totalSessionCount}회 중 ${formatPercent(stats.overrunRate)}"
            )
            WeeklySummaryRow(
                label = "필요한 사용",
                value = "${stats.necessaryUseCount}회",
                subValue = "목적과 달랐지만 필요하다고 판단한 횟수"
            )
            WeeklySummaryRow(
                label = "빠른 재진입",
                value = "${stats.fastReopenCount}회",
                subValue = "앱 종료 후 짧은 시간 안에 다시 들어간 횟수"
            )
            WeeklySummaryRow(
                label = "개입 후 종료",
                value = "${stats.closedAfterInterventionCount}회",
                subValue = "자기점검 화면 이후 앱을 닫은 횟수"
            )
        }
    }
}

@Composable
private fun WeeklySummaryRow(
    label: String,
    value: String,
    subValue: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashboardPill, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subValue,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
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
            .navigationBarsPadding()
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
            DashboardNavItem("●", "기록", selectedTab == DashboardTab.Session) {
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
                text = "앱별로 보기",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
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
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        days > 0L && hours > 0L -> "${days}일 ${hours}시간"
        days > 0L -> "${days}일"
        hours > 0L && minutes > 0L -> "${hours}시간 ${minutes}분"
        hours > 0L -> "${hours}시간"
        minutes > 0L && seconds > 0L -> "${minutes}분 ${seconds}초"
        minutes > 0L -> "${minutes}분"
        else -> "${seconds}초"
    }
}

private fun formatSessionStartedAt(startedAtMillis: Long): String {
    val started = Calendar.getInstance().apply {
        timeInMillis = startedAtMillis
    }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val timeText = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(startedAtMillis))

    return when {
        started.isSameDay(today) -> "오늘 $timeText"
        started.isSameDay(yesterday) -> "어제 $timeText"
        else -> SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(Date(startedAtMillis))
    }
}

private fun Calendar.isSameDay(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

private fun formatPercent(rate: Double): String {
    return "${(rate * 100.0).roundToInt()}%"
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
                todayPurposeKeptCount = 5,
                todayPurposeDriftCount = 2
            ),
            recentSessions = emptyList(),
            sessionRecentSessions = emptyList(),
            timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(emptyList()),
            weeklyReportStats = WeeklyReportStats(
                totalSessionCount = 24,
                overrunCount = 8,
                overrunRate = 0.62,
                fastReopenCount = 8,
                purposeDriftRate = 0.31,
                closedAfterInterventionCount = 12,
                averageReopenGapMillis = 124_000L,
                mostVulnerableTimeSlotStat = TimeSlotStat(
                    hourSlot = 23,
                    openCount = 4,
                    sessionCount = 4,
                    overrunCount = 3,
                    extensionCount = 1,
                    fastReopenCount = 2,
                    purposeDriftCount = 1,
                    purposeOutcomeResponseCount = 3,
                    overrunRate = 0.75,
                    fastReopenRate = 0.5,
                    extensionScore = 0.25,
                    openScore = 0.8,
                    purposeDriftRate = 1.0 / 3.0,
                    vulnerabilityScore = 0.5875
                ),
                hasEnoughVulnerableTimeData = true,
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
            onRecoverPermissionsClick = {},
            onAddTargetPackage = {},
            onRemoveTargetPackage = {},
            onDeleteAllSessionLogs = {},
            onSelectPackage = {}
        )
    }
}
