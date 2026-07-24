package com.teum.app.ui.target

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teum.app.ui.theme.TeumTheme

private val TargetBorder = Color(0xFFE3E7EF)
private val TargetGuide = Color(0xFFE8F8F4)
private val YouTubeTint = Color(0xFFFFEBEE)
private val InstagramTint = Color(0xFFFFF3E4)
private val TikTokTint = Color(0xFFEAF2FF)
private val NeutralTint = Color(0xFFF1F3F7)

@Composable
fun TargetAppSelectionScreen(
    onCompleteClick: (List<TargetAppSelectionResult>) -> Unit,
    initialSelectedPackages: Set<String>? = null,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val appItems = remember(primaryColor, mutedColor) {
        defaultTargetApps(primaryColor = primaryColor, mutedColor = mutedColor)
    }
    val checkedStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            appItems.forEach { item ->
                put(
                    item.packageName,
                    initialSelectedPackages?.contains(item.packageName) ?: item.initiallyChecked
                )
            }
        }
    }
    val selectedCount = checkedStates.values.count { it }

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
                text = "사용을 줄이고 싶은 앱을 골라주세요 · ${selectedCount}개 선택됨",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                items(appItems, key = { it.packageName }) { item ->
                    val checked = checkedStates[item.packageName] == true
                    TargetAppRow(
                        item = item,
                        checked = checked,
                        onCheckedChange = { checkedStates[item.packageName] = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(33.dp))

            GuideCard()

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onCompleteClick(
                        appItems.map { item ->
                            TargetAppSelectionResult(
                                packageName = item.packageName,
                                enabled = checkedStates[item.packageName] == true,
                                defaultDurationMillis = item.defaultDurationMillis
                            )
                        }
                    )
                },
                enabled = selectedCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = Color(0xFFCDD2E0),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
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
    item: TargetAppUi,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) MaterialTheme.colorScheme.primary else TargetBorder
        )
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
                    .background(item.iconContainerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.initial,
                    color = item.iconColor,
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
                    text = item.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
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
            text = "목표 시간은 앱을 열 때 정해요.",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "여기서는 관리할 앱만 선택합니다.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

private data class TargetAppUi(
    val packageName: String,
    val initial: String,
    val name: String,
    val description: String,
    val defaultDurationMillis: Long,
    val initiallyChecked: Boolean,
    val iconColor: Color,
    val iconContainerColor: Color
)

private fun defaultTargetApps(
    primaryColor: Color,
    mutedColor: Color
): List<TargetAppUi> = listOf(
    TargetAppUi(
        packageName = "com.google.android.youtube",
        initial = "Y",
        name = "YouTube",
        description = "Shorts 포함 영상 앱",
        defaultDurationMillis = 300_000L,
        initiallyChecked = true,
        iconColor = Color(0xFFF05D5E),
        iconContainerColor = YouTubeTint
    ),
    TargetAppUi(
        packageName = "com.instagram.android",
        initial = "I",
        name = "Instagram",
        description = "릴스 · 피드 · DM",
        defaultDurationMillis = 300_000L,
        initiallyChecked = true,
        iconColor = Color(0xFFFF9F43),
        iconContainerColor = InstagramTint
    ),
    TargetAppUi(
        packageName = "com.zhiliaoapp.musically",
        initial = "T",
        name = "TikTok",
        description = "추천 피드가 길어지기 쉬운 앱",
        defaultDurationMillis = 180_000L,
        initiallyChecked = true,
        iconColor = primaryColor,
        iconContainerColor = TikTokTint
    ),
    TargetAppUi(
        packageName = "com.twitter.android",
        initial = "X",
        name = "X",
        description = "피드 · 알림 확인",
        defaultDurationMillis = 300_000L,
        initiallyChecked = false,
        iconColor = mutedColor,
        iconContainerColor = NeutralTint
    ),
    TargetAppUi(
        packageName = "com.android.chrome",
        initial = "C",
        name = "Chrome",
        description = "뉴스 · 검색 · 커뮤니티",
        defaultDurationMillis = 600_000L,
        initiallyChecked = false,
        iconColor = mutedColor,
        iconContainerColor = NeutralTint
    ),
    TargetAppUi(
        packageName = "com.netflix.mediaclient",
        initial = "N",
        name = "Netflix",
        description = "영상 시청 앱",
        defaultDurationMillis = 600_000L,
        initiallyChecked = false,
        iconColor = Color(0xFFF05D5E),
        iconContainerColor = YouTubeTint
    )
)

data class TargetAppSelectionResult(
    val packageName: String,
    val enabled: Boolean,
    val defaultDurationMillis: Long
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TargetAppSelectionScreenPreview() {
    TeumTheme {
        TargetAppSelectionScreen(onCompleteClick = {})
    }
}
