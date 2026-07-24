package com.teum.app.ui.setup

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teum.app.core.model.InterventionMode
import com.teum.app.ui.theme.TeumTheme

private val ModeBorder = Color(0xFFE3E7EF)
private val ModeSoftPurple = Color(0xFFECEEFF)
private val ModeSoftMint = Color(0xFFE8F8F4)
private val ModeSoftOrange = Color(0xFFFFF3E4)
private val ModePurple = Color(0xFF5B5FEA)
private val ModeMint = Color(0xFF2EC4A6)
private val ModeOrange = Color(0xFFFF9F43)

@Composable
fun InterventionModeSetupScreen(
    selectedMode: InterventionMode,
    onModeSelected: (InterventionMode) -> Unit,
    onCompleteClick: (InterventionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMode by remember(selectedMode) { mutableStateOf(selectedMode) }

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
                text = "알림 강도 선택",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "처음에는 보통으로 시작해도 좋아요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                InterventionMode.entries.forEach { mode ->
                    ModeOptionCard(
                        mode = mode,
                        selected = currentMode == mode,
                        onClick = {
                            currentMode = mode
                            onModeSelected(mode)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            GuideCard()

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onCompleteClick(currentMode) },
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
                    text = "설정 완료",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(29.dp))
            Text(
                text = "선택한 강도는 설정에서 바꿀 수 있어요.",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ModeOptionCard(
    mode: InterventionMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (mode) {
        InterventionMode.LOW -> ModeMint
        InterventionMode.NORMAL -> ModePurple
        InterventionMode.HIGH -> ModeOrange
    }
    val container = when (mode) {
        InterventionMode.LOW -> ModeSoftMint
        InterventionMode.NORMAL -> ModeSoftPurple
        InterventionMode.HIGH -> ModeSoftOrange
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (selected) accent else ModeBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selected) "✓" else "•",
                    color = accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.size(13.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = mode.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mode.setupDescription(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

private fun InterventionMode.setupDescription(): String = when (this) {
    InterventionMode.NORMAL -> "앱을 열 때와 시간이 지났을 때 한 번씩 확인해요."
    else -> description
}

@Composable
private fun GuideCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ModeSoftPurple, RoundedCornerShape(22.dp))
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "추천",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "처음 사용한다면 보통을 추천해요.",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InterventionModeSetupScreenPreview() {
    TeumTheme {
        InterventionModeSetupScreen(
            selectedMode = InterventionMode.NORMAL,
            onModeSelected = {},
            onCompleteClick = {}
        )
    }
}
