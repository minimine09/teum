package com.teum.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.teum.app.core.model.PermissionStatus
import com.teum.app.core.util.PermissionUtils
import com.teum.app.data.repository.TargetAppRepository
import com.teum.app.dashboard.AppDisplayNameResolver
import com.teum.app.dashboard.DashboardScreen
import com.teum.app.dashboard.DashboardViewModel
import com.teum.app.ui.theme.TeumTheme

class MainActivity : ComponentActivity() {
    private val targetAppRepository by lazy {
        TargetAppRepository(this)
    }
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val appDisplayNameResolver by lazy { AppDisplayNameResolver(this) }

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
                val dashboardUiState by dashboardViewModel.uiState.collectAsState()
                val displayedPackages = targetPackages +
                    dashboardUiState.availablePackages +
                    dashboardUiState.recentSessions.map { it.packageName }
                val appDisplayNames = displayedPackages.associateWith(appDisplayNameResolver::resolve)

                DashboardScreen(
                    permissionStatus = permissionStatus,
                    targetPackages = targetPackages,
                    appDisplayNames = appDisplayNames,
                    dashboardStats = dashboardUiState.dashboardStats,
                    recentSessions = dashboardUiState.recentSessions,
                    timeSlotStats = dashboardUiState.timeSlotStats,
                    weeklyReportStats = dashboardUiState.weeklyReportStats,
                    availablePackages = dashboardUiState.availablePackages,
                    selectedPackageName = dashboardUiState.selectedPackageName,
                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                    onOpenOverlaySettings = ::openOverlaySettings,
                    onAddTargetPackage = ::addTargetPackage,
                    onRemoveTargetPackage = ::removeTargetPackage,
                    onDeleteAllSessionLogs = dashboardViewModel::deleteAllSessionLogs,
                    onSelectPackage = dashboardViewModel::selectPackage
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
