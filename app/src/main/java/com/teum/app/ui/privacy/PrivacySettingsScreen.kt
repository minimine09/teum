package com.teum.app.ui.privacy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.teum.app.core.model.InterventionMode
import com.teum.app.ui.theme.TeumTheme

private val PrivacyBorder = Color(0xFFE3E7EF)
private val PrivacyDark = Color(0xFF121622)
private val PrivacyDarkText = Color(0xFFC3CADB)
private val PrivacyDanger = Color(0xFFF05D5E)
private val PrivacyDangerContainer = Color(0xFFFDEDEE)
private val PrivacyPill = Color(0xFFF1F3F7)

@Composable
fun PrivacySettingsScreen(
    selectedMode: InterventionMode,
    onModeChange: (InterventionMode) -> Unit,
    onManageTargetAppsClick: () -> Unit = {},
    onDeleteAllClick: () -> Unit,
    showBottomNav: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val bottomPadding = if (showBottomNav) 24.dp else 96.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 50.dp, bottom = bottomPadding)
        ) {
            Header()
            Spacer(modifier = Modifier.height(20.dp))

            ServerCard()
            Spacer(modifier = Modifier.height(18.dp))
            InfoCard(
                title = "수집 데이터",
                body = "앱 이름 · 시작/종료 시각 · 사용 시간\n목적 선택 · 사용 후 선택 · 다시 열기까지 걸린 시간"
            )
            Spacer(modifier = Modifier.height(16.dp))
            InfoCard(
                title = "수집하지 않는 데이터",
                body = "메시지 내용 · 영상 내용 · 검색어 · 화면 캡처\n키보드 입력 · 연락처 · 위치 정보"
            )
            Spacer(modifier = Modifier.height(15.dp))
            ModeCard(
                selectedMode = selectedMode,
                onModeChange = onModeChange
            )
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

            if (showBottomNav) {
                Spacer(modifier = Modifier.height(24.dp))
                PrivacyBottomNav()
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
                    text = "관리할 앱을 다시 고를 수 있어요.",
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
            text = "개인정보와 알림 방식을 관리해요",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ServerCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(114.dp)
            .background(PrivacyDark, RoundedCornerShape(26.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Text(
            text = "서버 전송 없음",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "앱 사용 기록의 기본 정보만 기기에 저장하고\n기기 내부에서 통계 지표를 계산합니다.",
            color = PrivacyDarkText,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PrivacyBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun ModeCard(
    selectedMode: InterventionMode,
    onModeChange: (InterventionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(119.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PrivacyBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "알림 강도",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ⓘ",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InterventionMode.entries.forEach { mode ->
                    ModePill(
                        text = mode.label,
                        selected = selectedMode == mode,
                        onClick = { onModeChange(mode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 66.dp, height = 34.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else PrivacyPill,
                shape = RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PrivacyBottomNav(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp))
            .padding(horizontal = 26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(icon = "⌂", label = "홈")
        NavItem(icon = "•", label = "기록")
        NavItem(icon = "▤", label = "리포트")
        NavItem(icon = "⚙", label = "설정", selected = true)
    }
}

@Composable
private fun NavItem(
    icon: String,
    label: String,
    selected: Boolean = false
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PrivacySettingsScreenPreview() {
    TeumTheme {
        PrivacySettingsScreen(
            selectedMode = InterventionMode.NORMAL,
            onModeChange = {},
            onDeleteAllClick = {}
        )
    }
}
