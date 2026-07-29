package com.teum.app.ui.intervention

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teum.app.overlay.IntentChoice
import com.teum.app.overlay.TargetDurationChoice
import com.teum.app.session.OutcomeType
import com.teum.app.ui.theme.TeumTheme
import kotlin.math.roundToInt

private val InterventionBackground = Color(0xFFF8F9FC)
private val CareBackground = Color(0xFFFFFCF7)
private val CareAccent = Color(0xFFF2A02D)
private val BrakeBackground = Color(0xFFFFF8F2)
private val BorderSoft = Color(0xFFE5E7EC)
private val PurpleChoice = Color(0xFFF2EBFF)
private val MintChoice = Color(0xFFF5F6F8)
private val NeutralChoice = Color(0xFFF5F6F8)
private val BlueChoice = Color(0xFFF5F6F8)
private val OrangeChoice = Color(0xFFF5F6F8)
private val DangerChoice = Color(0xFFFDEDEE)
private val Success = Color(0xFF34C6A8)
private val NeutralDot = Color(0xFF9B9B9B)
private val Danger = Color(0xFFF05D5E)
private val Warning = Color(0xFFFF9F43)
private val TextPrimary = Color(0xFF1F2430)
private val TextSecondary = Color(0xFF778092)
private val TrackInactive = Color(0xFFE3E5EA)

@Composable
fun IntentCheckScreen(
    packageName: String? = null,
    appName: String = "Instagram",
    recentOpenCountText: String? = null,
    interventionActive: Boolean = false,
    selectedIntent: IntentChoice?,
    selectedDuration: TargetDurationChoice?,
    availableDurations: List<TargetDurationChoice> = DefaultDurationOptions,
    onIntentSelected: (IntentChoice) -> Unit,
    onDurationSelected: (TargetDurationChoice) -> Unit,
    onStartClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (interventionActive) CareAccent else MaterialTheme.colorScheme.primary
    val descriptionText = if (interventionActive) {
        "이 시간대에는 사용이 길어지기 쉬웠어요.\n오늘은 조금 짧게 틈을 만들어볼까요?"
    } else {
        recentOpenCountText ?: "최근 24시간 내 $appName 실행을 감지했어요."
    }

    InterventionLayout(
        packageName = packageName,
        appName = appName,
        title = "Intent Check",
        subtitle = "열기 전에 잠깐 확인해요",
        interventionActive = interventionActive,
        backgroundColor = if (interventionActive) CareBackground else InterventionBackground,
        modifier = modifier
    ) {
        CheckModal(
            title = "이 앱을 왜 열었나요?",
            description = descriptionText,
            options = listOf(
                IntentOptionUi(IntentChoice.CLEAR_PURPOSE, "명확한 목적", PurpleChoice, accentColor),
                IntentOptionUi(IntentChoice.MINDFUL_REST, "인지된 휴식", MintChoice, Success),
                IntentOptionUi(IntentChoice.UNCONSCIOUS_OPEN, "무의식 실행", NeutralChoice, NeutralDot)
            ),
            selectedIntent = selectedIntent,
            selectedDuration = selectedDuration,
            availableDurations = availableDurations,
            onIntentSelected = onIntentSelected,
            onDurationSelected = onDurationSelected,
            onStartClick = onStartClick,
            onCloseClick = onCloseClick,
            accentColor = accentColor
        )
    }
}

@Composable
fun ReopenCheckScreen(
    packageName: String? = null,
    appName: String = "Instagram",
    reopenGapMillis: Long? = null,
    interventionActive: Boolean = false,
    selectedIntent: IntentChoice?,
    selectedDuration: TargetDurationChoice?,
    availableDurations: List<TargetDurationChoice> = DefaultDurationOptions,
    onIntentSelected: (IntentChoice) -> Unit,
    onDurationSelected: (TargetDurationChoice) -> Unit,
    onStartClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (interventionActive) CareAccent else MaterialTheme.colorScheme.primary

    InterventionLayout(
        packageName = packageName,
        appName = appName,
        title = "Reopen Check",
        subtitle = "방금 다시 열었어요",
        interventionActive = interventionActive,
        backgroundColor = if (interventionActive) CareBackground else InterventionBackground,
        modifier = modifier
    ) {
        CheckModal(
            title = "왜 다시 들어왔나요?",
            description = reopenGapMillis?.let { "마지막 실행: ${formatDurationMillis(it)} 전" } ?: "마지막 실행: 짧은 시간 전",
            options = listOf(
                IntentOptionUi(IntentChoice.CLEAR_PURPOSE, "명확한 목적으로 계속", PurpleChoice, accentColor),
                IntentOptionUi(IntentChoice.MINDFUL_REST, "인지된 휴식", MintChoice, Success),
                IntentOptionUi(IntentChoice.UNCONSCIOUS_OPEN, "무의식 실행", NeutralChoice, NeutralDot)
            ),
            selectedIntent = selectedIntent,
            selectedDuration = selectedDuration,
            availableDurations = availableDurations,
            onIntentSelected = onIntentSelected,
            onDurationSelected = onDurationSelected,
            onStartClick = onStartClick,
            onCloseClick = onCloseClick,
            accentColor = accentColor
        )
    }
}

@Composable
fun SessionBrakeScreen(
    appName: String = "YouTube",
    elapsedMillis: Long? = null,
    targetDurationMillis: Long? = null,
    interventionActive: Boolean = false,
    extensionLimitReached: Boolean = false,
    availableExtensionDurations: List<TargetDurationChoice> = DefaultDurationOptions,
    onEndClick: () -> Unit,
    onExtendClick: (TargetDurationChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        val horizontalPadding = if (maxWidth < 360.dp) 16.dp else 24.dp
        val verticalPadding = if (maxHeight < 560.dp) 16.dp else 36.dp
        val maxContentHeight = (maxHeight - verticalPadding * 2).coerceAtLeast(360.dp)

        SessionBrakeContent(
            appName = appName,
            elapsedMillis = elapsedMillis,
            targetDurationMillis = targetDurationMillis,
            interventionActive = interventionActive,
            extensionLimitReached = extensionLimitReached,
            availableExtensionDurations = availableExtensionDurations,
            onEndClick = onEndClick,
            onExtendClick = onExtendClick,
            modifier = Modifier
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .fillMaxWidth()
                .heightIn(max = maxContentHeight)
        )
    }
}

@Composable
fun SessionBrakeContent(
    appName: String = "YouTube",
    elapsedMillis: Long? = null,
    targetDurationMillis: Long? = null,
    interventionActive: Boolean = false,
    extensionLimitReached: Boolean = false,
    availableExtensionDurations: List<TargetDurationChoice> = DefaultDurationOptions,
    onEndClick: () -> Unit,
    onExtendClick: (TargetDurationChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExtensionExpanded by remember { mutableStateOf(false) }
    var selectedExtensionDuration by remember(availableExtensionDurations) {
        mutableStateOf(
            availableExtensionDurations.firstOrNull {
                it == TargetDurationChoice.THREE_MINUTES
            } ?: availableExtensionDurations.firstOrNull()
            ?: TargetDurationChoice.THREE_MINUTES
        )
    }
    val overrunMillis = if (elapsedMillis != null && targetDurationMillis != null) {
        (elapsedMillis - targetDurationMillis).coerceAtLeast(0L)
    } else {
        null
    }
    val accentColor = if (interventionActive) CareAccent else MaterialTheme.colorScheme.primary
    val brakeGuidanceText = when {
        interventionActive && extensionLimitReached ->
            "오늘 이 시간대의 연장은 여기까지예요.\n처음 목적을 마무리했다면 나와볼까요?"
        interventionActive ->
            "조심 모드가 켜져 있어요.\n취약 시간대에는 연장이 3회까지만 가능해요."
        else ->
            "조금 더 사용할지, 여기서 멈출지 짧게 확인해요."
    }

    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(22.dp))
            .then(
                if (interventionActive) {
                    Modifier.border(1.dp, CareAccent.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
                } else {
                    Modifier
                }
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (interventionActive) {
            CautionModeBadge()
            Spacer(modifier = Modifier.height(16.dp))
        }
        AlertBubble(
            symbol = "!",
            size = 44,
            color = accentColor,
            containerColor = accentColor.copy(alpha = 0.12f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "목표 시간에 도달했어요",
            color = TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "잠시 쉬어가거나 목적을 확인해 주세요.",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSoft)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = appName,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BrakeTimeMetric(
                label = "현재 사용",
                value = elapsedMillis?.let(::formatDurationMillis) ?: "-"
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(BorderSoft)
            )
            BrakeTimeMetric(
                label = "목표",
                value = targetDurationMillis?.let(::formatDurationMillis) ?: "-"
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSoft)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = brakeGuidanceText,
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (isExtensionExpanded) {
            DurationChoiceSlider(
                selectedDuration = selectedExtensionDuration,
                availableDurations = availableExtensionDurations,
                onDurationSelected = { selectedExtensionDuration = it },
                accentColor = accentColor,
                label = "연장 시간 선택"
            )
            Spacer(modifier = Modifier.height(20.dp))
            TeumFilledButton(
                text = "${selectedExtensionDuration.label} 연장하기",
                onClick = { onExtendClick(selectedExtensionDuration) },
                color = accentColor,
                enabled = !extensionLimitReached
            )
            Spacer(modifier = Modifier.height(8.dp))
            TeumOutlinedActionButton(text = "종료하기", onClick = onEndClick)
        } else {
            TeumFilledButton("종료하기", onEndClick, accentColor)
            Spacer(modifier = Modifier.height(8.dp))
            TeumOutlinedActionButton(
                text = if (extensionLimitReached) "연장 한도에 도달했어요" else "연장하기",
                onClick = { isExtensionExpanded = true },
                enabled = !extensionLimitReached
            )
        }
    }
}

@Composable
private fun BrakeTimeMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InterventionLayout(
    packageName: String?,
    appName: String,
    title: String,
    subtitle: String?,
    interventionActive: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 24.dp)
        ) {
            if (interventionActive) {
                CautionModeBadge()
                Spacer(modifier = Modifier.height(16.dp))
            }
            AppIdentityRow(
                packageName = packageName,
                appName = appName,
                accentColor = if (interventionActive) {
                    CareAccent
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                color = if (interventionActive) CareAccent else MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }
    }
}

@Composable
private fun CautionModeBadge(modifier: Modifier = Modifier) {
    Text(
        text = "⚠ 조심 모드 활성",
        modifier = modifier
            .background(Color(0xFFFFF1D8), RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = CareAccent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun AppIdentityRow(
    packageName: String?,
    appName: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appIcon = remember(packageName) {
        packageName?.let { loadApplicationIcon(context, it) }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = "$appName 앱 아이콘",
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appName.take(1).uppercase(),
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = appName,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ScreenHeader(
    title: String,
    subtitle: String?,
    onBackClick: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(verticalAlignment = Alignment.Top) {
        val backModifier = if (onBackClick != null) {
            Modifier
                .size(30.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable(onClick = onBackClick)
        } else {
            Modifier
                .size(30.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        }
        Box(
            modifier = backModifier,
            contentAlignment = Alignment.Center
        ) {
            BackChevron()
        }
        Spacer(modifier = Modifier.width(9.dp))
        Column {
            Text(
                text = title,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
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
    title: String,
    description: String,
    options: List<IntentOptionUi>,
    selectedIntent: IntentChoice?,
    selectedDuration: TargetDurationChoice?,
    availableDurations: List<TargetDurationChoice> = DefaultDurationOptions,
    onIntentSelected: (IntentChoice) -> Unit,
    onDurationSelected: (TargetDurationChoice) -> Unit,
    onStartClick: () -> Unit,
    onCloseClick: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 500.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            options.forEach { option ->
                ChoiceRow(
                    option = option,
                    selected = selectedIntent == option.choice,
                    accentColor = accentColor,
                    onClick = { onIntentSelected(option.choice) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        DurationChoiceSlider(
            selectedDuration = selectedDuration,
            availableDurations = availableDurations,
            onDurationSelected = onDurationSelected,
            accentColor = accentColor
        )
        Spacer(modifier = Modifier.height(48.dp))
        TeumFilledButton(
            text = "시작",
            onClick = onStartClick,
            color = accentColor,
            height = 48,
            enabled = selectedIntent != null &&
                selectedIntent != IntentChoice.CLOSE_NOW &&
                selectedDuration != null
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                onIntentSelected(IntentChoice.CLOSE_NOW)
                onCloseClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, BorderSoft)
        ) {
            Text(
                text = "지금은 닫기",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AlertBubble(
    symbol: String,
    size: Int,
    color: Color = Color(0xFF8491FF),
    containerColor: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = color,
            fontSize = if (size > 80) 48.sp else 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChoiceRow(
    option: IntentOptionUi,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val containerColor = if (selected) accentColor.copy(alpha = 0.09f) else Color.White
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (selected) 1.4.dp else 1.dp, if (selected) accentColor else BorderSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionDot(
                color = accentColor,
                selected = selected,
                size = 18
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = option.text,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DurationChoiceSlider(
    selectedDuration: TargetDurationChoice?,
    onDurationSelected: (TargetDurationChoice) -> Unit,
    availableDurations: List<TargetDurationChoice> = DefaultDurationOptions,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    label: String = "예상 사용 시간",
    modifier: Modifier = Modifier
) {
    val durationOptions = availableDurations.ifEmpty { DefaultDurationOptions }
    val selectedIndex = durationOptions
        .indexOf(selectedDuration)
        .takeIf { it >= 0 }
        ?: 0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = durationOptions[selectedIndex].label,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
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
                .height(20.dp),
            valueRange = 0f..durationOptions.lastIndex.toFloat(),
            steps = (durationOptions.size - 2).coerceAtLeast(0),
            accentColor = accentColor
        )
    }
}

private val DefaultDurationOptions = listOf(
    TargetDurationChoice.TEST_FIVE_SECONDS,
    TargetDurationChoice.ONE_MINUTE,
    TargetDurationChoice.THREE_MINUTES,
    TargetDurationChoice.FIVE_MINUTES,
    TargetDurationChoice.TEN_MINUTES,
    TargetDurationChoice.FIFTEEN_MINUTES,
    TargetDurationChoice.THIRTY_MINUTES,
    TargetDurationChoice.ONE_HOUR
)

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
    steps: Int = 58,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var sliderSize by remember { mutableStateOf(IntSize.Zero) }
    val start = valueRange.start
    val end = valueRange.endInclusive
    val progress = ((value - start) / (end - start)).coerceIn(0f, 1f)
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
            val trackHeight = 4.dp.toPx()
            val trackWidth = size.width
            val centerY = size.height / 2f
            val corner = trackHeight / 2f
            val thumbRadius = 7.dp.toPx()
            val progressWidth = trackWidth * progress

            drawRoundRect(
                color = TrackInactive,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(progressWidth, trackHeight),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawCircle(
                color = accentColor,
                radius = thumbRadius,
                center = Offset(
                    x = progressWidth.coerceIn(thumbRadius, trackWidth - thumbRadius),
                    y = centerY
                )
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
private fun TeumOutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, BorderSoft),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TextPrimary,
            disabledContentColor = TextSecondary.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OutcomeOption(
    option: OutcomeOptionUi,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val optionBackground = if (selected) accentColor.copy(alpha = 0.09f) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = optionBackground),
        border = BorderStroke(if (selected) 1.4.dp else 1.dp, if (selected) accentColor else BorderSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutcomeSelectionDot(
                color = accentColor,
                selected = selected
            )
            Spacer(modifier = Modifier.width(11.dp))
            Text(
                text = option.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OutcomeSelectionDot(
    color: Color,
    selected: Boolean
) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(if (selected) color else Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun OutcomeCheckScreen(
    sessionData: OutcomeSessionUi,
    onOutcomeSelected: (OutcomeType) -> Unit,
    onDismissClick: () -> Unit,
    interventionActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedOutcomeType by remember { mutableStateOf<OutcomeType?>(null) }
    val outcomeAccent = if (interventionActive) CareAccent else MaterialTheme.colorScheme.primary
    val outcomeBackground = if (interventionActive) Color(0xFFFFFBF5) else Color(0xFFF7F9FC)
    val outcomeOptions = listOf(
        OutcomeSelectionOptionUi(
            ui = OutcomeOptionUi(
                title = "목적 달성",
                description = "처음 목적을 완료함",
                containerColor = MintChoice,
                dotColor = Success
            ),
            outcomeType = OutcomeType.PURPOSE_ACHIEVED
        ),
        OutcomeSelectionOptionUi(
            ui = OutcomeOptionUi(
                title = "필요한 사용",
                description = "처음 목적과 달라졌지만 의식적으로 필요한 사용",
                containerColor = BlueChoice,
                dotColor = outcomeAccent
            ),
            outcomeType = OutcomeType.NECESSARY_USE
        ),
        OutcomeSelectionOptionUi(
            ui = OutcomeOptionUi(
                title = "목적 이탈",
                description = "처음 목적과 무관한 자극으로 이동",
                containerColor = DangerChoice,
                dotColor = Danger
            ),
            outcomeType = OutcomeType.PURPOSE_DRIFT
        ),
        OutcomeSelectionOptionUi(
            ui = OutcomeOptionUi(
                title = "무의식 사용",
                description = "어느 순간 무의식적으로 계속 사용",
                containerColor = OrangeChoice,
                dotColor = Warning
            ),
            outcomeType = OutcomeType.CONTINUED_SCROLLING
        )
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = outcomeBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 24.dp)
        ) {
            if (interventionActive) {
                CautionModeBadge()
                Spacer(modifier = Modifier.height(14.dp))
            }
            ScreenHeader(
                title = "Outcome Check",
                subtitle = "사용을 마무리합니다",
                onBackClick = onDismissClick,
                accentColor = outcomeAccent
            )
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                OutcomeSessionSummaryCard(sessionData = sessionData)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "스스로 평가",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "결과 응답은 다음 리포트와 개입 강도에 반영됩니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    outcomeOptions.forEach { option ->
                        OutcomeOption(
                            option = option.ui,
                            selected = selectedOutcomeType == option.outcomeType,
                            accentColor = outcomeAccent,
                            onClick = { selectedOutcomeType = option.outcomeType }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
            TeumFilledButton(
                text = "기록 저장",
                onClick = { selectedOutcomeType?.let(onOutcomeSelected) },
                color = outcomeAccent,
                height = 49,
                enabled = selectedOutcomeType != null
            )
        }
    }
}

@Composable
private fun OutcomeSessionSummaryCard(
    sessionData: OutcomeSessionUi,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 17.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "이번 사용 요약",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            OutcomeSummaryLine(label = "앱", value = sessionData.appName)
            OutcomeSummaryLine(label = "처음 목적", value = sessionData.intentText)
            OutcomeSummaryLine(label = "실제 사용", value = formatKoreanDuration(sessionData.actualUsageMillis))
            OutcomeSummaryLine(label = "목표 시간", value = formatKoreanDuration(sessionData.targetDurationMillis))
            OutcomeSummaryLine(label = "연장 횟수", value = "${sessionData.extensionCount}회")
        }
    }
}

@Composable
private fun OutcomeSummaryLine(
    label: String,
    value: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

data class OutcomeSessionUi(
    val appName: String,
    val intentText: String,
    val actualUsageMillis: Long,
    val targetDurationMillis: Long,
    val extensionCount: Int
)

private fun loadApplicationIcon(
    context: Context,
    packageName: String
): ImageBitmap? {
    return runCatching {
        context.packageManager.getApplicationIcon(packageName).toImageBitmap()
    }.getOrNull()
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return bitmap.asImageBitmap()
    }

    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

private data class OutcomeSelectionOptionUi(
    val ui: OutcomeOptionUi,
    val outcomeType: OutcomeType
)

private fun formatKoreanDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return when {
        minutes > 0L && seconds > 0L -> "${minutes}분 ${seconds}초"
        minutes > 0L -> "${minutes}분"
        else -> "${seconds}초"
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
        "$base\n점검까지 지난 시간: ${formatDurationMillis(overrunMillis)}"
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
private fun IntentCheckCareModePreview() {
    TeumTheme {
        var selectedIntent by remember { mutableStateOf<IntentChoice?>(IntentChoice.CLEAR_PURPOSE) }
        var selectedDuration by remember { mutableStateOf<TargetDurationChoice?>(TargetDurationChoice.ONE_MINUTE) }
        IntentCheckScreen(
            interventionActive = true,
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
private fun SessionBrakeCareModePreview() {
    TeumTheme {
        SessionBrakeScreen(
            elapsedMillis = 74_000L,
            targetDurationMillis = 60_000L,
            interventionActive = true,
            extensionLimitReached = true,
            onEndClick = {},
            onExtendClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OutcomeCheckScreenPreview() {
    TeumTheme {
        OutcomeCheckScreen(
            sessionData = OutcomeSessionUi(
                appName = "Instagram",
                intentText = "인지된 휴식",
                actualUsageMillis = 860_000L,
                targetDurationMillis = 300_000L,
                extensionCount = 2
            ),
            onOutcomeSelected = {},
            onDismissClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OutcomeCheckCareModePreview() {
    TeumTheme {
        OutcomeCheckScreen(
            sessionData = OutcomeSessionUi(
                appName = "Instagram",
                intentText = "명확한 목적",
                actualUsageMillis = 420_000L,
                targetDurationMillis = 300_000L,
                extensionCount = 1
            ),
            interventionActive = true,
            onOutcomeSelected = {},
            onDismissClick = {}
        )
    }
}
