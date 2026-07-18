package com.teum.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.teum.app.core.model.PermissionStatus
import com.teum.app.core.util.PermissionUtils
import com.teum.app.data.repository.SessionLogRepository
import com.teum.app.data.repository.TargetAppRepository
import com.teum.app.dashboard.DashboardStats
import com.teum.app.dashboard.DashboardScreen
import com.teum.app.dashboard.VulnerabilityAnalyzer
import com.teum.app.ui.theme.TeumTheme

class MainActivity : ComponentActivity() {
    private val targetAppRepository by lazy {
        TargetAppRepository(this)
    }
    private val sessionLogRepository by lazy {
        SessionLogRepository(this)
    }

    private var permissionStatus by mutableStateOf(
        PermissionStatus(
            isAccessibilityEnabled = false,
            canDrawOverlays = false
        )
    )
    private var targetPackages by mutableStateOf(emptySet<String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionStatus()
        refreshTargetPackages()

        setContent {
            TeumTheme {
                val recentSessions by sessionLogRepository.observeRecentSessions()
                    .collectAsState(initial = emptyList())
                val todaySessionCount by sessionLogRepository.observeTodaySessionCount()
                    .collectAsState(initial = 0)
                val todayOverrunCount by sessionLogRepository.observeTodayOverrunCount()
                    .collectAsState(initial = 0)
                val todayFastReopenCount by sessionLogRepository.observeTodayFastReopenCount()
                    .collectAsState(initial = 0)
                val todayPurposeDriftCount by sessionLogRepository.observeTodayPurposeDriftCount()
                    .collectAsState(initial = 0)
                val lastSevenDaysSessions by sessionLogRepository.observeSessionsForLastSevenDays()
                    .collectAsState(initial = emptyList())
                val timeSlotStats = remember(lastSevenDaysSessions) {
                    VulnerabilityAnalyzer.calculateTimeSlotStats(lastSevenDaysSessions)
                }

                DashboardScreen(
                    permissionStatus = permissionStatus,
                    targetPackages = targetPackages,
                    dashboardStats = DashboardStats(
                        todaySessionCount = todaySessionCount,
                        todayOverrunCount = todayOverrunCount,
                        todayFastReopenCount = todayFastReopenCount,
                        todayPurposeDriftCount = todayPurposeDriftCount
                    ),
                    recentSessions = recentSessions,
                    timeSlotStats = timeSlotStats,
                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                    onOpenOverlaySettings = ::openOverlaySettings,
                    onAddTargetPackage = ::addTargetPackage,
                    onRemoveTargetPackage = ::removeTargetPackage
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
        refreshTargetPackages()
    }

    private fun refreshPermissionStatus() {
        permissionStatus = PermissionStatus(
            isAccessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(this),
            canDrawOverlays = PermissionUtils.canDrawOverlays(this)
        )
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun addTargetPackage(packageName: String) {
        targetAppRepository.addTargetPackage(packageName)
        refreshTargetPackages()
    }

    private fun removeTargetPackage(packageName: String) {
        targetAppRepository.removeTargetPackage(packageName)
        refreshTargetPackages()
    }

    private fun refreshTargetPackages() {
        targetPackages = targetAppRepository.getTargetPackages()
    }
}
