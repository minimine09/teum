package com.teum.app.ui.intervention

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teum.app.overlay.IntentChoice
import com.teum.app.overlay.TargetDurationChoice
import com.teum.app.ui.theme.TeumTheme
import kotlin.math.roundToInt

private val InterventionBackground = Color(0xFFECEEFF)
private val BrakeBackground = Color(0xFFFFF8F2)
private val BorderSoft = Color(0xFFE3E7EF)
private val PurpleChoice = Color(0xFFFBE5FF)
private val MintChoice = Color(0xFFE8F8F4)
private val NeutralChoice = Color(0xFFE8E8E8)
private val BlueChoice = Color(0xFFEAF4FF)
private val OrangeChoice = Color(0xFFFFF3E4)
private val DangerChoice = Color(0xFFFDEDEE)
private val Success = Color(0xFF34C6A8)
private val NeutralDot = Color(0xFF9B9B9B)
private val Danger = Color(0xFFF05D5E)
private val Warning = Color(0xFFFF9F43)

@Composable
fun IntentCheckScreen(
    appName: String = "Instagram",
    recentOpenCountText: String? = null,
    selectedIntent: IntentChoice?,
    selectedDuration: TargetDurationChoice?,
    onIntentSelected: (IntentChoice) -> Unit,
    onDurationSelected: (TargetDurationChoice) -> Unit,
    onStartClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InterventionLayout(
        title = "Intent Check",
        subtitle = "앱 실행 전 5초 자기점검",
        backgroundColor = InterventionBackground,
        modifier = modifier
    ) {
        CheckModal(
            symbol = "?",
            title = "${appName}을 왜 열었나요?",
            description = recentOpenCountText ?: "최근 24시간 내 $appName 실행을 감지했어요.",
            options = listOf(
                IntentOptionUi(IntentChoice.CLEAR_PURPOSE, "명확한 목적", PurpleChoice, MaterialTheme.colorScheme.primary),
                IntentOptionUi(IntentChoice.MINDFUL_REST, "인지된 휴식", MintChoice, Success),
                IntentOptionUi(IntentChoice.UNCONSCIOUS_OPEN, "무의식 실행", NeutralChoice, NeutralDot)
            ),
            selectedIntent = selectedIntent,
            selectedDuration = selectedDuration,
            onIntentSelected = onIntentSelected,
            onDurationSelected = onDurationSelected,
            onStartClick = onStartClick,
            onCloseClick = onCloseClick
        )
    }
}

@Composable
fun ReopenCheckScreen(
    appName: String = "Instagram",
    reopenGapMillis: Long? = null,
    selectedIntent: IntentChoice?,
    selectedDuration: TargetDurationChoice?,
    onIntentSelected: (IntentChoice) -> Unit,
    onDurationSelected: (TargetDurationChoice) -> Unit,
    onStartClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InterventionLayout(
        title = "Reopen Check",
        subtitle = null,
        backgroundColor = InterventionBackground,
        modifier = modifier
    ) {
        CheckModal(
            symbol = "!",
            title = "방금 다시 열었어요",
            description = reopenGapMillis?.let { "마지막 실행: ${formatDurationMillis(it)} 전" } ?: "마지막 실행: 짧은 시간 전",
            options = listOf(
                IntentOptionUi(IntentChoice.CLEAR_PURPOSE, "명확한 목적으로 계속", PurpleChoice, MaterialTheme.colorScheme.primary),
                IntentOptionUi(IntentChoice.MINDFUL_REST, "인지된 휴식", MintChoice, Success),
                IntentOptionUi(IntentChoice.UNCONSCIOUS_OPEN, "무의식 실행", NeutralChoice, NeutralDot)
            ),
            selectedIntent = selectedIntent,
            selectedDuration = selectedDuration,
            onIntentSelected = onIntentSelected,
            onDurationSelected = onDurationSelected,
            onStartClick = onStartClick,
            onCloseClick = onCloseClick
        )
    }
}

@Composable
fun SessionBrakeScreen(
    appName: String = "YouTube",
    elapsedMillis: Long? = null,
    targetDurationMillis: Long? = null,
    onEndClick: () -> Unit,
    onExtendClick: (TargetDurationChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        SessionBrakeContent(
            appName = appName,
            elapsedMillis = elapsedMillis,
            targetDurationMillis = targetDurationMillis,
            onEndClick = onEndClick,
            onExtendClick = onExtendClick,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 36.dp)
                .fillMaxWidth()
                .heightIn(max = 620.dp)
        )
    }
}

@Composable
fun SessionBrakeContent(
    appName: String = "YouTube",
    elapsedMillis: Long? = null,
    targetDurationMillis: Long? = null,
    onEndClick: () -> Unit,
    onExtendClick: (TargetDurationChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExtensionExpanded by remember { mutableStateOf(false) }
    var selectedExtensionDuration by remember {
        mutableStateOf(TargetDurationChoice.THREE_MINUTES)
    }
    val overrunMillis = if (elapsedMillis != null && targetDurationMillis != null) {
        (elapsedMillis - targetDurationMillis).coerceAtLeast(0L)
    } else {
        null
    }

    Column(
        modifier = modifier
            .background(InterventionBackground, RoundedCornerShape(34.dp))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 31.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlertBubble(symbol = "!", size = 90)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "예상 시간을 초과했어요",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "설정한 사용 시간을 넘겼어요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = buildSessionBrakeSummary(
                appName = appName,
                elapsedMillis = elapsedMillis,
                targetDurationMillis = targetDurationMillis,
                overrunMillis = overrunMillis
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "지금 계속 사용할 이유가 있나요?",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "조금 더 사용할지, 여기서 멈출지 짧게 확인해요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        TeumFilledButton("종료하기", onEndClick, MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(18.dp))
        TeumFilledButton(
            text = if (isExtensionExpanded) {
                "선택한 시간만큼 연장하기"
            } else {
                "연장하기"
            },
            onClick = {
                if (isExtensionExpanded) {
                    onExtendClick(selectedExtensionDuration)
                } else {
                    isExtensionExpanded = true
                }
            },
            color = MaterialTheme.colorScheme.primary
        )
        if (isExtensionExpanded) {
            Spacer(modifier = Modifier.height(24.dp))
            DurationChoiceSlider(
                selectedDuration = selectedExtensionDuration,
                onDurationSelected = { selectedExtensionDuration = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "연장 시간을 고른 뒤 다시 연장하기를 눌러주세요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OutcomeCheckScreen(
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOutcomeIndex by remember { mutableStateOf<Int?>(null) }
    val outcomeOptions = listOf(
        OutcomeOptionUi("목적 달성함", "필요한 사용 또는 계획된 휴식", MintChoice, Success),
        OutcomeOptionUi("목적과 다른 사용으로 이어짐", "릴스·추천 피드 등으로 이동", DangerChoice, Danger),
        OutcomeOptionUi("시간을 초과했지만 필요했음", "자료 확인, 연락 등 예외 처리", BlueChoice, MaterialTheme.colorScheme.primary),
        OutcomeOptionUi("계속 스크롤하게 됨", "세션 과몰입으로 기록", OrangeChoice, Warning)
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 50.dp, bottom = 79.dp)
        ) {
            ScreenHeader(title = "Outcome Check", subtitle = "목적과 실제 결과 연결")
            Spacer(modifier = Modifier.height(23.dp))
            SessionSummaryCard()
            Spacer(modifier = Modifier.height(38.dp))
            Text(
                text = "처음 목적을 달성했나요?",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "결과 응답은 다음 리포트와 개입 강도에 반영됩니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(39.dp))
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                outcomeOptions.forEachIndexed { index, option ->
                    OutcomeOption(
                        option = option,
                        selected = selectedOutcomeIndex == index,
                        onClick = { selectedOutcomeIndex = index }
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            TeumFilledButton(
                text = "기록 저장",
                onClick = onSaveClick,
                color = MaterialTheme.colorScheme.primary,
                height = 49,
                enabled = selectedOutcomeIndex != null
            )
        }
    }
}

@Composable
private fun InterventionLayout(
    title: String,
    subtitle: String?,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 23.dp)
                .padding(top = 43.dp)
        ) {
            ScreenHeader(title = title, subtitle = subtitle)
            Spacer(modifier = Modifier.height(31.dp))
            content()
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String?) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            BackChevron()
        }
        Spacer(modifier = Modifier.width(9.dp))
        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun BackChevron(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.size(width = 8.dp, height = 14.dp)) {
        drawLine(
            color = color,
            start = Offset(size.width, 0f),
            end = Offset(0f, size.height / 2f),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun CheckModal(
    symbol: String,
    title: String,
    description: String,
    options: List<IntentOptionUi>,
    selectedIntent: IntentChoice?,
    selectedDuration: TargetDurationChoice?,
    onIntentSelected: (IntentChoice) -> Unit,
    onDurationSelected: (TargetDurationChoice) -> Unit,
    onStartClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(618.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
            .padding(horizontal = 24.dp, vertical = 21.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlertBubble(symbol = symbol, size = 70)
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Column(verticalArrangement = Arrangement.spacedBy(23.dp)) {
            options.forEach { option ->
                ChoiceRow(
                    option = option,
                    selected = selectedIntent == option.choice,
                    onClick = { onIntentSelected(option.choice) }
                )
            }
        }
        Spacer(modifier = Modifier.height(35.dp))
        DurationChoiceSlider(
            selectedDuration = selectedDuration,
            onDurationSelected = onDurationSelected
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TeumFilledButton(
                text = "시작",
                onClick = onStartClick,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                height = 44,
                enabled = selectedIntent != null &&
                    selectedIntent != IntentChoice.CLOSE_NOW &&
                    selectedDuration != null
            )
            OutlinedButton(
                onClick = {
                    onIntentSelected(IntentChoice.CLOSE_NOW)
                    onCloseClick()
                },
                modifier = Modifier
                    .weight(0.64f)
                    .height(44.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Text(
                    text = "지금은 닫기",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AlertBubble(symbol: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = Color(0xFF8491FF),
            fontSize = if (size > 80) 48.sp else 36.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChoiceRow(
    option: IntentOptionUi,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(61.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = option.containerColor),
        border = if (selected) BorderStroke(1.4.dp, option.dotColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionDot(
                color = option.dotColor,
                selected = selected,
                size = 20
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = option.text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DurationChoiceSlider(
    selectedDuration: TargetDurationChoice?,
    onDurationSelected: (TargetDurationChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationOptions = listOf(
        TargetDurationChoice.TEST_FIVE_SECONDS,
        TargetDurationChoice.ONE_MINUTE,
        TargetDurationChoice.THREE_MINUTES,
        TargetDurationChoice.FIVE_MINUTES,
        TargetDurationChoice.TEN_MINUTES
    )
    val selectedIndex = durationOptions
        .indexOf(selectedDuration)
        .takeIf { it >= 0 }
        ?: 0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "예상 사용 시간",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatDurationChoice(durationOptions[selectedIndex]),
                color = Color.Black,
                fontSize = 12.sp
            )
        }
        TeumDurationSlider(
            value = selectedIndex.toFloat(),
            onValueChange = { rawValue ->
                val nextIndex = rawValue.roundToInt().coerceIn(0, durationOptions.lastIndex)
                onDurationSelected(durationOptions[nextIndex])
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(23.dp),
            valueRange = 0f..durationOptions.lastIndex.toFloat(),
            steps = durationOptions.size - 2
        )
    }
}

@Composable
private fun SelectionDot(
    color: Color,
    selected: Boolean,
    size: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(if (selected) color else Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size((size * 0.36f).dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun DurationSelector(
    durationMinutes: Float,
    onDurationChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "예상 사용 시간",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatDuration(durationMinutes),
                color = Color.Black,
                fontSize = 12.sp
            )
        }
        TeumDurationSlider(
            value = durationMinutes,
            onValueChange = onDurationChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(23.dp),
            valueRange = 0.5f..30f,
            steps = 58
        )
    }
}

@Composable
private fun TeumDurationSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0.5f..30f,
    steps: Int = 58
) {
    var sliderSize by remember { mutableStateOf(IntSize.Zero) }
    val start = valueRange.start
    val end = valueRange.endInclusive
    val progress = ((value - start) / (end - start)).coerceIn(0f, 1f)
    val primaryColor = MaterialTheme.colorScheme.primary

    fun updateValue(positionX: Float) {
        val width = sliderSize.width.toFloat().coerceAtLeast(1f)
        val rawProgress = (positionX / width).coerceIn(0f, 1f)
        val rawValue = start + (end - start) * rawProgress
        val stepSize = (end - start) / (steps + 1)
        val steppedValue = start + ((rawValue - start) / stepSize).roundToInt() * stepSize
        onValueChange(steppedValue.coerceIn(start, end))
    }

    Box(
        modifier = modifier
            .onSizeChanged { sliderSize = it }
            .pointerInput(valueRange, steps) {
                detectDragGestures(
                    onDragStart = { offset -> updateValue(offset.x) },
                    onDrag = { change, _ -> updateValue(change.position.x) }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = size.height
            val trackWidth = size.width
            val corner = 5.dp.toPx()
            val handleWidth = 11.dp.toPx()
            val progressWidth = (trackWidth * progress).coerceAtLeast(handleWidth / 2f)
            val handleLeft = (trackWidth * progress - handleWidth / 2f)
                .coerceIn(0f, trackWidth - handleWidth)

            drawRoundRect(
                color = Color.White,
                topLeft = Offset.Zero,
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                color = Color(0xFFCCCDFF),
                topLeft = Offset.Zero,
                size = Size(progressWidth, trackHeight),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                color = Color(0xFF9FA5FF),
                topLeft = Offset.Zero,
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = 1.dp.toPx())
            )
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(handleLeft, 0f),
                size = Size(handleWidth, trackHeight),
                cornerRadius = CornerRadius(corner, corner)
            )
        }
    }
}

@Composable
private fun TeumFilledButton(
    text: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    height: Int = 50,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFCDD2E0),
            disabledContentColor = Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SessionSummaryCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(127.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 27.dp)
        ) {
            Text(
                text = "이번 세션 요약",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            SummaryRow("처음 목적", "5분만 휴식", MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(10.dp))
            SummaryRow("실제 사용", "14분 20초 · 2회 연장", Danger)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OutcomeOption(
    option: OutcomeOptionUi,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(67.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = option.containerColor),
        border = if (selected) BorderStroke(1.4.dp, option.dotColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionDot(
                color = option.dotColor,
                selected = selected,
                size = 24
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = option.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = option.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private data class IntentOptionUi(
    val choice: IntentChoice,
    val text: String,
    val containerColor: Color,
    val dotColor: Color
)

private data class OutcomeOptionUi(
    val title: String,
    val description: String,
    val containerColor: Color,
    val dotColor: Color
)

private fun formatDuration(minutesValue: Float): String {
    val totalSeconds = (minutesValue * 60).roundToInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatDurationChoice(choice: TargetDurationChoice): String {
    val totalSeconds = (choice.durationMillis / 1_000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatDurationMillis(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) {
        "${minutes}분 ${seconds}초"
    } else {
        "${seconds}초"
    }
}

private fun buildSessionBrakeSummary(
    appName: String,
    elapsedMillis: Long?,
    targetDurationMillis: Long?,
    overrunMillis: Long?
): String {
    if (elapsedMillis == null || targetDurationMillis == null) {
        return "감지된 앱: $appName"
    }

    val base = "감지된 앱: $appName\n사용 시간: ${formatDurationMillis(elapsedMillis)} / 목표 시간: ${formatDurationMillis(targetDurationMillis)}"
    return if (overrunMillis != null && overrunMillis > 0L) {
        "$base\n초과 시간: ${formatDurationMillis(overrunMillis)}"
    } else {
        base
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun IntentCheckScreenPreview() {
    TeumTheme {
        var selectedIntent by remember { mutableStateOf<IntentChoice?>(IntentChoice.CLEAR_PURPOSE) }
        var selectedDuration by remember { mutableStateOf<TargetDurationChoice?>(TargetDurationChoice.TEST_FIVE_SECONDS) }
        IntentCheckScreen(
            selectedIntent = selectedIntent,
            selectedDuration = selectedDuration,
            onIntentSelected = { selectedIntent = it },
            onDurationSelected = { selectedDuration = it },
            onStartClick = {},
            onCloseClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReopenCheckScreenPreview() {
    TeumTheme {
        var selectedIntent by remember { mutableStateOf<IntentChoice?>(IntentChoice.UNCONSCIOUS_OPEN) }
        var selectedDuration by remember { mutableStateOf<TargetDurationChoice?>(TargetDurationChoice.ONE_MINUTE) }
        ReopenCheckScreen(
            reopenGapMillis = 83_000L,
            selectedIntent = selectedIntent,
            selectedDuration = selectedDuration,
            onIntentSelected = { selectedIntent = it },
            onDurationSelected = { selectedDuration = it },
            onStartClick = {},
            onCloseClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun IntentCheckMindfulRestPreview() {
    TeumTheme {
        var selectedIntent by remember { mutableStateOf<IntentChoice?>(IntentChoice.MINDFUL_REST) }
        var selectedDuration by remember { mutableStateOf<TargetDurationChoice?>(TargetDurationChoice.THREE_MINUTES) }
        IntentCheckScreen(
            selectedIntent = selectedIntent,
            selectedDuration = selectedDuration,
            onIntentSelected = { selectedIntent = it },
            onDurationSelected = { selectedDuration = it },
            onStartClick = {},
            onCloseClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SessionBrakeScreenPreview() {
    TeumTheme {
        SessionBrakeScreen(
            elapsedMillis = 74_000L,
            targetDurationMillis = 60_000L,
            onEndClick = {},
            onExtendClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OutcomeCheckScreenPreview() {
    TeumTheme {
        OutcomeCheckScreen(onSaveClick = {})
    }
}
