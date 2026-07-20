package com.teum.app.ui.target

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.teum.app.ui.theme.TeumTheme

private val TargetBorder = Color(0xFFE3E7EF)
private val TargetPill = Color(0xFFF1F3F7)
private val TargetGuide = Color(0xFFE8F8F4)
private val YouTubeTint = Color(0xFFFFEBEE)
private val InstagramTint = Color(0xFFFFF3E4)
private val TikTokTint = Color(0xFFEAF2FF)
private val NeutralTint = Color(0xFFF1F3F7)

@Composable
fun TargetAppSelectionScreen(
    onCompleteClick: () -> Unit,
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
                .padding(top = 50.dp, bottom = 82.dp)
        ) {
            Text(
                text = "관리 앱 선택",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "목적 확인을 적용할 앱을 고르세요",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                TargetAppRow(
                    initial = "Y",
                    name = "YouTube",
                    description = "Shorts 포함 영상 앱",
                    duration = "5분",
                    checked = true,
                    iconColor = Color(0xFFF05D5E),
                    iconContainerColor = YouTubeTint
                )
                TargetAppRow(
                    initial = "I",
                    name = "Instagram",
                    description = "릴스·피드·DM",
                    duration = "5분",
                    checked = true,
                    iconColor = Color(0xFFFF9F43),
                    iconContainerColor = InstagramTint
                )
                TargetAppRow(
                    initial = "T",
                    name = "TikTok",
                    description = "숏폼 추천 피드",
                    duration = "3분",
                    checked = true,
                    iconColor = MaterialTheme.colorScheme.primary,
                    iconContainerColor = TikTokTint
                )
                TargetAppRow(
                    initial = "X",
                    name = "X",
                    description = "피드·알림 확인",
                    duration = "5분",
                    checked = false,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconContainerColor = NeutralTint
                )
                TargetAppRow(
                    initial = "W",
                    name = "웹 브라우저",
                    description = "뉴스·검색·커뮤니티",
                    duration = "10분",
                    checked = false,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconContainerColor = NeutralTint
                )
            }

            Spacer(modifier = Modifier.height(33.dp))

            GuideCard()

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onCompleteClick,
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
                    text = "선택 완료",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TargetAppRow(
    initial: String,
    name: String,
    description: String,
    duration: String,
    checked: Boolean,
    iconColor: Color,
    iconContainerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TargetBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconContainerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = iconColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            DurationPill(duration)
            Spacer(modifier = Modifier.size(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                    uncheckedTrackColor = TargetBorder,
                    uncheckedBorderColor = TargetBorder
                )
            )
        }
    }
}

@Composable
private fun DurationPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 47.dp, height = 26.dp)
            .background(TargetPill, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GuideCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(TargetGuide, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "기본값은 앱별로 바꿀 수 있어요",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "처음에는 3~5분으로 시작하는 것을 권장",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TargetAppSelectionScreenPreview() {
    TeumTheme {
        TargetAppSelectionScreen(onCompleteClick = {})
    }
}
