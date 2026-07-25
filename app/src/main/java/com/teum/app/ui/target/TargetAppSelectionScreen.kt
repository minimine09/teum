package com.teum.app.ui.target

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teum.app.ui.theme.TeumTheme

private val TargetBorder = Color(0xFFE3E7EF)
private val YouTubeTint = Color(0xFFFFEBEE)
private val InstagramTint = Color(0xFFFFF3E4)
private val TikTokTint = Color(0xFFEAF2FF)
private val NeutralTint = Color(0xFFF1F3F7)

@Composable
fun TargetAppSelectionScreen(
    onCompleteClick: (List<TargetAppSelectionResult>) -> Unit,
    initialSelectedPackages: Set<String>? = null,
    installedApps: List<TargetAppInstalledApp> = emptyList(),
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val initialSelected = initialSelectedPackages.orEmpty()
    val installedByPackage = remember(installedApps) {
        installedApps.distinctBy { it.packageName }.associateBy { it.packageName }
    }
    val recommendedItems = remember(installedApps, primaryColor, mutedColor) {
        recommendedPackageNames.mapNotNull { packageName ->
            installedByPackage[packageName]?.toTargetAppUi(
                primaryColor = primaryColor,
                mutedColor = mutedColor,
                recommended = true
            )
        }
    }
    val selectedItems = remember(initialSelectedPackages, installedApps, primaryColor, mutedColor) {
        initialSelected.map { packageName ->
            installedByPackage[packageName]?.toTargetAppUi(
                primaryColor = primaryColor,
                mutedColor = mutedColor,
                recommended = packageName in recommendedPackageNames
            ) ?: TargetAppUi.uninstalled(packageName, mutedColor)
        }
    }
    val addedItems = remember { mutableStateListOf<TargetAppUi>() }
    val appItems = remember(selectedItems, recommendedItems, addedItems.toList()) {
        (selectedItems + recommendedItems + addedItems).distinctBy { it.packageName }
    }
    val checkedStates = remember(initialSelectedPackages, installedApps) {
        mutableStateMapOf<String, Boolean>().apply {
            appItems.forEach { item ->
                put(item.packageName, initialSelected.contains(item.packageName) || item.initiallyChecked)
            }
        }
    }
    appItems.forEach { item ->
        if (checkedStates[item.packageName] == null) {
            checkedStates[item.packageName] = item.initiallyChecked
        }
    }

    var isPickerOpen by remember { mutableStateOf(false) }
    val selectedCount = checkedStates.values.count { it }

    if (isPickerOpen) {
        InstalledAppPickerScreen(
            installedApps = installedApps,
            selectedPackages = checkedStates.filterValues { it }.keys,
            onBackClick = { isPickerOpen = false },
            onAppSelected = { app ->
                if (app.packageName !in appItems.map { it.packageName }) {
                    addedItems.add(
                        app.toTargetAppUi(
                            primaryColor = primaryColor,
                            mutedColor = mutedColor,
                            recommended = false
                        )
                    )
                }
                checkedStates[app.packageName] = true
                isPickerOpen = false
            },
            modifier = modifier
        )
        return
    }

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
                text = "선택한 앱과 추천 앱만 보여드려요 · ${selectedCount}개 선택됨",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                if (appItems.isEmpty()) {
                    item {
                        EmptyTargetAppCard(
                            text = if (installedApps.isEmpty()) {
                                "표시할 앱을 찾지 못했어요."
                            } else {
                                "추천 앱이 설치되어 있지 않아요."
                            }
                        )
                    }
                }
                items(appItems, key = { it.packageName }) { item ->
                    val checked = checkedStates[item.packageName] == true
                    TargetAppRow(
                        item = item,
                        checked = checked,
                        onCheckedChange = { checkedStates[item.packageName] = it }
                    )
                }
                item {
                    AddAnotherAppRow(onClick = { isPickerOpen = true })
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

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
            .heightIn(min = 82.dp)
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
            AppIcon(item = item)

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
private fun AddAnotherAppRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, TargetBorder)
    ) {
        Text(
            text = "+ 다른 앱 추가",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InstalledAppPickerScreen(
    installedApps: List<TargetAppInstalledApp>,
    selectedPackages: Set<String>,
    onBackClick: () -> Unit,
    onAppSelected: (TargetAppInstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(installedApps, query) {
        val normalizedQuery = query.trim()
        installedApps
            .filter { app ->
                normalizedQuery.isBlank() ||
                    app.appName.contains(normalizedQuery, ignoreCase = true) ||
                    app.packageName.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedBy { it.appName.lowercase() }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 50.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "다른 앱 추가",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "닫기",
                    modifier = Modifier.clickable(onClick = onBackClick),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("앱 이름 또는 패키지명 검색") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (installedApps.isEmpty()) {
                    item {
                        EmptyTargetAppCard(text = "설치 앱 목록을 불러오지 못했어요.")
                    }
                } else if (filteredApps.isEmpty()) {
                    item {
                        EmptyTargetAppCard(text = "검색 결과가 없어요.")
                    }
                } else {
                    items(filteredApps, key = { it.packageName }) { app ->
                        InstalledAppRow(
                            app = app,
                            selected = app.packageName in selectedPackages,
                            onClick = { onAppSelected(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledAppRow(
    app: TargetAppInstalledApp,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else TargetBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                item = app.toTargetAppUi(
                    primaryColor = MaterialTheme.colorScheme.primary,
                    mutedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    recommended = false
                )
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (selected) "선택됨" else "추가",
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyTargetAppCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TargetBorder)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun AppIcon(item: TargetAppUi) {
    val bitmap = remember(item.icon) {
        item.icon?.toBitmapOrNull()
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(item.iconContainerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Text(
                text = item.initial,
                color = item.iconColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
    val iconContainerColor: Color,
    val icon: Drawable?,
    val installed: Boolean
) {
    companion object {
        fun uninstalled(packageName: String, mutedColor: Color): TargetAppUi {
            return TargetAppUi(
                packageName = packageName,
                initial = packageName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                name = packageName,
                description = "설치되지 않음",
                defaultDurationMillis = defaultDurationFor(packageName),
                initiallyChecked = true,
                iconColor = mutedColor,
                iconContainerColor = NeutralTint,
                icon = null,
                installed = false
            )
        }
    }
}

data class TargetAppInstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

data class TargetAppSelectionResult(
    val packageName: String,
    val enabled: Boolean,
    val defaultDurationMillis: Long
)

private fun TargetAppInstalledApp.toTargetAppUi(
    primaryColor: Color,
    mutedColor: Color,
    recommended: Boolean
): TargetAppUi {
    val tint = tintForPackage(packageName)
    return TargetAppUi(
        packageName = packageName,
        initial = appName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        name = appName,
        description = descriptionForPackage(packageName, recommended),
        defaultDurationMillis = defaultDurationFor(packageName),
        initiallyChecked = false,
        iconColor = tint.iconColor ?: if (recommended) primaryColor else mutedColor,
        iconContainerColor = tint.containerColor,
        icon = icon,
        installed = true
    )
}

private data class TargetTint(
    val iconColor: Color?,
    val containerColor: Color
)

private val recommendedPackageNames = listOf(
    "com.google.android.youtube",
    "com.instagram.android",
    "com.zhiliaoapp.musically",
    "com.twitter.android",
    "com.android.chrome",
    "com.sec.android.app.sbrowser",
    "org.mozilla.firefox",
    "com.brave.browser",
    "com.naver.whale",
    "com.netflix.mediaclient",
    "com.google.android.apps.youtube.music",
    "com.reddit.frontpage"
)

private fun descriptionForPackage(packageName: String, recommended: Boolean): String = when (packageName) {
    "com.google.android.youtube" -> "Shorts 포함 영상 앱"
    "com.instagram.android" -> "릴스 · 피드 · DM"
    "com.zhiliaoapp.musically" -> "추천 피드가 길어지기 쉬운 앱"
    "com.twitter.android" -> "피드 · 알림 확인"
    "com.android.chrome",
    "com.sec.android.app.sbrowser",
    "org.mozilla.firefox",
    "com.brave.browser",
    "com.naver.whale" -> "검색 · 뉴스 · 커뮤니티"
    "com.netflix.mediaclient",
    "com.google.android.apps.youtube.music",
    "com.reddit.frontpage" -> "콘텐츠 사용 앱"
    else -> if (recommended) "추천 앱" else packageName
}

private fun defaultDurationFor(packageName: String): Long = when (packageName) {
    "com.zhiliaoapp.musically" -> 180_000L
    "com.android.chrome",
    "com.sec.android.app.sbrowser",
    "org.mozilla.firefox",
    "com.brave.browser",
    "com.naver.whale",
    "com.netflix.mediaclient" -> 600_000L
    else -> 300_000L
}

private fun tintForPackage(packageName: String): TargetTint = when (packageName) {
    "com.google.android.youtube",
    "com.netflix.mediaclient",
    "com.google.android.apps.youtube.music" -> TargetTint(Color(0xFFF05D5E), YouTubeTint)
    "com.instagram.android" -> TargetTint(Color(0xFFFF9F43), InstagramTint)
    "com.zhiliaoapp.musically" -> TargetTint(null, TikTokTint)
    else -> TargetTint(null, NeutralTint)
}

private fun Drawable.toBitmapOrNull(): Bitmap? {
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    return runCatching {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
        }
    }.getOrNull()
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TargetAppSelectionScreenPreview() {
    TeumTheme {
        TargetAppSelectionScreen(
            initialSelectedPackages = setOf("com.google.android.youtube", "com.example.removed"),
            installedApps = listOf(
                TargetAppInstalledApp("com.google.android.youtube", "YouTube"),
                TargetAppInstalledApp("com.instagram.android", "Instagram"),
                TargetAppInstalledApp("com.android.chrome", "Chrome")
            ),
            onCompleteClick = {}
        )
    }
}
