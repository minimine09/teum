package com.teum.app.ui.permission

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teum.app.core.model.PermissionStatus
import com.teum.app.ui.theme.TeumTheme

private val PermissionBlue = Color(0xFF5B5FEA)
private val PermissionMint = Color(0xFF2EC4A6)
private val PermissionOrange = Color(0xFFFF9F43)
private val PermissionBlueContainer = Color(0xFFECEEFF)
private val PermissionMintContainer = Color(0xFFE8F8F4)
private val PermissionOrangeContainer = Color(0xFFFFF3E4)
private val PermissionBorder = Color(0xFFE3E7EF)
private val PermissionReady = Color(0xFF2EC4A6)
private val PermissionMissing = Color(0xFFF05D5E)
private val PermissionMissingContainer = Color(0xFFFDEDEE)

@Composable
fun PermissionSetupScreen(
    permissionStatus: PermissionStatus,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val requiredPermissionsReady = permissionStatus.isAccessibilityEnabled &&
        permissionStatus.canDrawOverlays

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 50.dp, bottom = 42.dp)
        ) {
            Text(
                text = "설정 준비",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "앱 사용을 확인하고 안내를 띄우기 위해 필요해요",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(31.dp))

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                PermissionCard(
                    title = "사용 기록 접근",
                    description = "관리 앱을 언제 열고 닫는지 확인해요.",
                    badgeText = "필수",
                    statusText = if (permissionStatus.isAccessibilityEnabled) "완료" else "설정 필요",
                    granted = permissionStatus.isAccessibilityEnabled,
                    color = PermissionBlue,
                    containerColor = PermissionBlueContainer,
                    onClick = onOpenAccessibilitySettings
                )
                PermissionCard(
                    title = "화면 위 표시 권한",
                    description = "앱 위에 짧은 확인 화면을 띄워요.",
                    badgeText = "필수",
                    statusText = if (permissionStatus.canDrawOverlays) "완료" else "설정 필요",
                    granted = permissionStatus.canDrawOverlays,
                    color = PermissionMint,
                    containerColor = PermissionMintContainer,
                    onClick = onOpenOverlaySettings
                )
                PermissionCard(
                    title = "로컬 저장",
                    description = "사용 기록의 기본 정보만 기기 안에 저장해요.",
                    badgeText = "자동",
                    statusText = "준비됨",
                    granted = true,
                    color = PermissionOrange,
                    containerColor = PermissionOrangeContainer,
                    onClick = null
                )
            }

            Spacer(modifier = Modifier.height(33.dp))

            NotCollectedCard()

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (requiredPermissionsReady) {
                    "필수 권한이 준비됐어요. 다음 단계로 이동할 수 있어요."
                } else {
                    "필수 권한을 모두 설정한 뒤 다음 단계로 이동할 수 있어요."
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = if (requiredPermissionsReady) PermissionReady else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = if (requiredPermissionsReady) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onContinueClick,
                enabled = requiredPermissionsReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = Color(0xFFCDD2E0),
                    disabledContentColor = Color.White
                )
            ) {
                Text(
                    text = "권한 설정 완료",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(29.dp))
            Text(
                text = "권한은 언제든 설정에서 바꿀 수 있어요.",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    badgeText: String,
    statusText: String,
    granted: Boolean,
    color: Color,
    containerColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (granted) containerColor else PermissionBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 13.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(containerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (granted) "✓" else "!",
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionBadge(
                    text = badgeText,
                    color = color,
                    containerColor = containerColor
                )
                PermissionBadge(
                    text = statusText,
                    color = if (granted) PermissionReady else PermissionMissing,
                    containerColor = if (granted) PermissionMintContainer else PermissionMissingContainer,
                    wide = true
                )
            }
        }
    }
}

@Composable
private fun PermissionBadge(
    text: String,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    wide: Boolean = false
) {
    Box(
        modifier = modifier
            .size(width = if (wide) 62.dp else 48.dp, height = 26.dp)
            .background(containerColor, RoundedCornerShape(13.dp)),
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
private fun NotCollectedCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(111.dp)
            .background(PermissionBlueContainer, RoundedCornerShape(22.dp))
            .padding(horizontal = 24.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "수집하지 않는 데이터",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "메시지 내용 · 영상 내용 · 검색어 · 화면 캡처 · 연락처 · 위치 정보",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PermissionSetupScreenPreview() {
    TeumTheme {
        PermissionSetupScreen(
            permissionStatus = PermissionStatus(
                isAccessibilityEnabled = false,
                canDrawOverlays = true
            ),
            onOpenAccessibilitySettings = {},
            onOpenOverlaySettings = {},
            onContinueClick = {}
        )
    }
}
