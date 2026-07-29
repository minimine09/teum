package com.teum.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.teum.app.core.model.InterventionMode
import com.teum.app.core.model.PermissionStatus
import com.teum.app.core.util.PermissionUtils
import com.teum.app.data.repository.TargetAppRepository
import com.teum.app.data.repository.UserSettingsRepository
import com.teum.app.dashboard.AppDisplayNameResolver
import com.teum.app.dashboard.DashboardScreen
import com.teum.app.dashboard.DashboardViewModel
import com.teum.app.ui.onboarding.OnboardingScreen
import com.teum.app.ui.permission.PermissionSetupScreen
import com.teum.app.ui.setup.InterventionModeSetupScreen
import com.teum.app.ui.target.TargetAppInstalledApp
import com.teum.app.ui.target.TargetAppSelectionScreen
import com.teum.app.ui.theme.TeumTheme

private enum class LaunchFlowStep {
    Onboarding,
    PermissionSetup,
    TargetAppSelection,
    InterventionModeSetup,
    Dashboard
}

private enum class PermissionSetupEntryPoint {
    InitialSetup,
    DashboardRecovery
}

private fun PermissionStatus.hasRequiredPermissions(): Boolean {
    return isAccessibilityEnabled && canDrawOverlays
}

class MainActivity : ComponentActivity() {
    private val targetAppRepository by lazy {
        TargetAppRepository(this)
    }
    private val userSettingsRepository by lazy {
        UserSettingsRepository(this)
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
    private var installedLauncherApps by mutableStateOf(emptyList<TargetAppInstalledApp>())
    private var forceVulnerableNowForDebug by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        forceVulnerableNowForDebug = userSettingsRepository.getForceVulnerableNowForDebug()

        refreshPermissionStatus()
        refreshTargetPackages()
        refreshInstalledLauncherApps()

        setContent {
            TeumTheme {
                val setupCompleted = userSettingsRepository.isSetupCompleted()
                var launchFlowStep by remember {
                    mutableStateOf(
                        if (!setupCompleted) {
                            LaunchFlowStep.Onboarding
                        } else if (permissionStatus.hasRequiredPermissions()) {
                            LaunchFlowStep.Dashboard
                        } else {
                            LaunchFlowStep.PermissionSetup
                        }
                    )
                }
                var permissionSetupEntryPoint by remember {
                    mutableStateOf(
                        if (setupCompleted && !permissionStatus.hasRequiredPermissions()) {
                            PermissionSetupEntryPoint.DashboardRecovery
                        } else {
                            PermissionSetupEntryPoint.InitialSetup
                        }
                    )
                }
                var selectedInterventionMode by remember {
                    mutableStateOf(userSettingsRepository.getInterventionMode())
                }
                val dashboardUiState by dashboardViewModel.uiState.collectAsState()
                val displayedPackages = targetPackages +
                    dashboardUiState.availablePackages +
                    dashboardUiState.recentSessions.map { it.packageName }
                val appDisplayNames = displayedPackages.associateWith(appDisplayNameResolver::resolve)

                LaunchedEffect(permissionStatus, launchFlowStep) {
                    if (
                        launchFlowStep != LaunchFlowStep.Onboarding &&
                        launchFlowStep != LaunchFlowStep.PermissionSetup &&
                        !permissionStatus.hasRequiredPermissions()
                    ) {
                        permissionSetupEntryPoint = if (userSettingsRepository.isSetupCompleted()) {
                            PermissionSetupEntryPoint.DashboardRecovery
                        } else {
                            PermissionSetupEntryPoint.InitialSetup
                        }
                        launchFlowStep = LaunchFlowStep.PermissionSetup
                    }
                }

                when (launchFlowStep) {
                    LaunchFlowStep.Onboarding -> {
                        OnboardingScreen(
                            onStartClick = {
                                permissionSetupEntryPoint = PermissionSetupEntryPoint.InitialSetup
                                launchFlowStep = LaunchFlowStep.PermissionSetup
                            }
                        )
                    }

                    LaunchFlowStep.PermissionSetup -> {
                        PermissionSetupScreen(
                            permissionStatus = permissionStatus,
                            onOpenAccessibilitySettings = ::openAccessibilitySettings,
                            onOpenOverlaySettings = ::openOverlaySettings,
                            onContinueClick = {
                                launchFlowStep = when (permissionSetupEntryPoint) {
                                    PermissionSetupEntryPoint.InitialSetup -> LaunchFlowStep.TargetAppSelection
                                    PermissionSetupEntryPoint.DashboardRecovery -> LaunchFlowStep.Dashboard
                                }
                            },
                            onLaterClick = null
                        )
                    }

                    LaunchFlowStep.TargetAppSelection -> {
                        TargetAppSelectionScreen(
                            installedApps = installedLauncherApps,
                            onCompleteClick = { results ->
                                results.forEach { result ->
                                    if (result.enabled) {
                                        targetAppRepository.addTargetPackage(result.packageName)
                                    } else {
                                        targetAppRepository.removeTargetPackage(result.packageName)
                                    }
                                }
                                refreshTargetPackages()
                                launchFlowStep = LaunchFlowStep.InterventionModeSetup
                            }
                        )
                    }

                    LaunchFlowStep.InterventionModeSetup -> {
                        InterventionModeSetupScreen(
                            selectedMode = selectedInterventionMode,
                            onModeSelected = { selectedInterventionMode = it },
                            onCompleteClick = {
                                selectedInterventionMode = it
                                userSettingsRepository.setInterventionMode(it)
                                userSettingsRepository.setSetupCompleted(true)
                                launchFlowStep = LaunchFlowStep.Dashboard
                            }
                        )
                    }

                    LaunchFlowStep.Dashboard -> {
                        DashboardScreen(
                            permissionStatus = permissionStatus,
                            targetPackages = targetPackages,
                            appDisplayNames = appDisplayNames,
                            dashboardStats = dashboardUiState.dashboardStats,
                            recentSessions = dashboardUiState.recentSessions,
                            sessionRecentSessions = dashboardUiState.sessionRecentSessions,
                            timeSlotStats = dashboardUiState.timeSlotStats,
                            weeklyReportStats = dashboardUiState.weeklyReportStats,
                            availablePackages = dashboardUiState.availablePackages,
                            installedApps = installedLauncherApps,
                            selectedPackageName = dashboardUiState.selectedPackageName,
                            onOpenAccessibilitySettings = ::openAccessibilitySettings,
                            onOpenOverlaySettings = ::openOverlaySettings,
                            onRecoverPermissionsClick = {
                                permissionSetupEntryPoint = PermissionSetupEntryPoint.DashboardRecovery
                                launchFlowStep = LaunchFlowStep.PermissionSetup
                            },
                            onAddTargetPackage = ::addTargetPackage,
                            onRemoveTargetPackage = ::removeTargetPackage,
                            onDeleteAllSessionLogs = dashboardViewModel::deleteAllSessionLogs,
                            onSelectPackage = dashboardViewModel::selectPackage,
                            selectedInterventionMode = selectedInterventionMode,
                            onInterventionModeChange = {
                                selectedInterventionMode = it
                                userSettingsRepository.setInterventionMode(it)
                            },
                            showVulnerableDebugOverride = isDebuggableBuild(),
                            forceVulnerableNowForDebug = forceVulnerableNowForDebug,
                            onForceVulnerableNowForDebugChange = {
                                forceVulnerableNowForDebug = it
                                userSettingsRepository.setForceVulnerableNowForDebug(it)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
        refreshTargetPackages()
        refreshInstalledLauncherApps()
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

    private fun refreshInstalledLauncherApps() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        installedLauncherApps = packageManager.queryIntentActivitiesCompat(launcherIntent)
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName.orEmpty()
                if (packageName.isBlank() || packageName == this.packageName) {
                    return@mapNotNull null
                }
                TargetAppInstalledApp(
                    packageName = packageName,
                    appName = resolveInfo.loadLabel(packageManager)?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?: packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    private fun isDebuggableBuild(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    @Suppress("DEPRECATION")
    private fun android.content.pm.PackageManager.queryIntentActivitiesCompat(
        intent: Intent
    ): List<ResolveInfo> {
        return queryIntentActivities(intent, 0)
    }
}
