package com.teum.app.ui.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import com.teum.app.ui.theme.TeumTheme

private val PermissionBlue = Color(0xFF5B5FEA)
private val PermissionMint = Color(0xFF2EC4A6)
private val PermissionOrange = Color(0xFFFF9F43)
private val PermissionBlueContainer = Color(0xFFECEEFF)
private val PermissionMintContainer = Color(0xFFE8F8F4)
private val PermissionOrangeContainer = Color(0xFFFFF3E4)
private val PermissionBorder = Color(0xFFE3E7EF)

@Composable
fun PermissionSetupScreen(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "세션 기록과 알림에 필요한 최소 권한",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(31.dp))

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                PermissionCard(
                    title = "사용 기록 접근",
                    description = "관리 대상 앱의 foreground 사용 구간만 확인",
                    badgeText = "필수",
                    color = PermissionBlue,
                    containerColor = PermissionBlueContainer
                )
                PermissionCard(
                    title = "알림 권한",
                    description = "약속 시간 초과와 결과 점검을 알려줌",
                    badgeText = "필수",
                    color = PermissionMint,
                    containerColor = PermissionMintContainer
                )
                PermissionCard(
                    title = "로컬 저장",
                    description = "Room DB에 세션 메타데이터만 저장",
                    badgeText = "자동",
                    color = PermissionOrange,
                    containerColor = PermissionOrangeContainer
                )
            }

            Spacer(modifier = Modifier.height(33.dp))

            NotCollectedCard()

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "권한 설정 계속하기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(29.dp))
            Text(
                text = "언제든 설정에서 해제할 수 있어요",
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
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PermissionBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 13.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(containerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
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
                    fontSize = 12.sp
                )
            }
            PermissionBadge(
                text = badgeText,
                color = color,
                containerColor = containerColor
            )
        }
    }
}

@Composable
private fun PermissionBadge(
    text: String,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 26.dp)
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
            text = "수집하지 않음",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "메시지 내용 · 영상 내용 · 검색어 · 화면 캡처 · 위치 정보",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PermissionSetupScreenPreview() {
    TeumTheme {
        PermissionSetupScreen(onContinueClick = {})
    }
}
