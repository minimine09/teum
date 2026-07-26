package com.teum.app.ui.privacy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teum.app.core.model.InterventionMode
import com.teum.app.core.model.PermissionStatus
import com.teum.app.ui.theme.TeumTheme

private val PrivacyBorder = Color(0xFFE3E7EF)
private val PrivacyDanger = Color(0xFFF05D5E)
private val PrivacyDangerContainer = Color(0xFFFDEDEE)
private val PrivacyPill = Color(0xFFF1F3F7)
private val PrivacyCare = Color(0xFFFF9F43)
private val PrivacyCareContainer = Color(0xFFFFF3E4)
private val PrivacyNormalContainer = Color(0xFFECEEFF)

@Composable
fun PrivacySettingsScreen(
    selectedMode: InterventionMode,
    onModeChange: (InterventionMode) -> Unit,
    showVulnerableDebugOverride: Boolean = false,
    forceVulnerableNowForDebug: Boolean = false,
    onForceVulnerableNowForDebugChange: (Boolean) -> Unit = {},
    permissionStatus: PermissionStatus,
    onPermissionSettingsClick: () -> Unit,
    onManageTargetAppsClick: () -> Unit = {},
    onDeleteAllClick: () -> Unit,
    showBottomNav: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val bottomPadding = if (showBottomNav) 96.dp else 24.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 50.dp, bottom = bottomPadding)
        ) {
            Header()
            Spacer(modifier = Modifier.height(20.dp))
            ModeCard(
                selectedMode = selectedMode,
                onModeChange = onModeChange
            )
            Spacer(modifier = Modifier.height(16.dp))
            PermissionSettingsCard(
                permissionStatus = permissionStatus,
                onClick = onPermissionSettingsClick
            )
            if (showVulnerableDebugOverride) {
                Spacer(modifier = Modifier.height(16.dp))
                VulnerableDebugOverrideCard(
                    enabled = forceVulnerableNowForDebug,
                    onEnabledChange = onForceVulnerableNowForDebugChange
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            ManageTargetAppsCard(onClick = onManageTargetAppsClick)
            Spacer(modifier = Modifier.height(23.dp))
            Button(
                onClick = onDeleteAllClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrivacyDangerContainer,
                    contentColor = PrivacyDanger
                )
            ) {
                Text(
                    text = "기록 전체 삭제",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionSettingsCard(
    permissionStatus: PermissionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PrivacyBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "권한 설정",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "앱 사용 감지와 확인 화면에 필요한 권한을 관리해요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            PermissionStatusLine(
                label = "접근성 권한",
                ready = permissionStatus.isAccessibilityEnabled
            )
            PermissionStatusLine(
                label = "화면 위 표시 권한",
                ready = permissionStatus.canDrawOverlays
            )
            Text(
                text = "권한 확인 및 변경",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PermissionStatusLine(
    label: String,
    ready: Boolean
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
            text = if (ready) "완료" else "설정 필요",
            color = if (ready) PrivacyCare else PrivacyDanger,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ManageTargetAppsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PrivacyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "관리 앱 선택",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "틈이 확인할 앱을 다시 고를 수 있어요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "변경",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VulnerableDebugOverrideCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PrivacyCareContainer),
        border = BorderStroke(1.dp, PrivacyCare)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEnabledChange(!enabled) }
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "시연용 취약 시간대",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (enabled) {
                        "현재 시간을 취약 시간대로 강제 적용 중"
                    } else {
                        "켜면 현재 시간이 취약 시간대로 판정돼요"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrivacyCare,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = PrivacyPill
                )
            )
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "설정",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "개인정보와 사용 모드를 관리해요",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ModeCard(
    selectedMode: InterventionMode,
    onModeChange: (InterventionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PrivacyBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Text(
                text = "사용 모드",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InterventionMode.entries.forEach { mode ->
                    ModeOptionRow(
                        mode = mode,
                        selected = selectedMode == mode,
                        onClick = { onModeChange(mode) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "언제든지 변경할 수 있어요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ModeOptionRow(
    mode: InterventionMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (mode.isIntervention) PrivacyCare else MaterialTheme.colorScheme.primary
    val container = if (mode.isIntervention) PrivacyCareContainer else PrivacyNormalContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (selected) container else PrivacyPill,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (selected) accent else Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = mode.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PrivacySettingsNormalPreview() {
    TeumTheme {
        PrivacySettingsScreen(
            selectedMode = InterventionMode.NORMAL,
            onModeChange = {},
            permissionStatus = PermissionStatus(
                isAccessibilityEnabled = true,
                canDrawOverlays = true
            ),
            onPermissionSettingsClick = {},
            onDeleteAllClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PrivacySettingsCarePreview() {
    TeumTheme {
        PrivacySettingsScreen(
            selectedMode = InterventionMode.INTERVENTION,
            onModeChange = {},
            permissionStatus = PermissionStatus(
                isAccessibilityEnabled = false,
                canDrawOverlays = true
            ),
            onPermissionSettingsClick = {},
            onDeleteAllClick = {}
        )
    }
}
