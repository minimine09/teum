package com.teum.app.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teum.app.core.model.PermissionStatus

@Composable
fun DemoToolsEntry(
    permissionStatus: PermissionStatus,
    forceVulnerableNowForDebug: Boolean,
    onForceVulnerableNowForDebugChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Unit
}
