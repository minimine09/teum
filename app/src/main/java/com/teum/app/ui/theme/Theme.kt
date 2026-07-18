package com.teum.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TeumLightColorScheme = lightColorScheme(
    primary = Color(0xFF2F6F66),
    onPrimary = Color.White,
    secondary = Color(0xFF6750A4),
    background = Color(0xFFFBFCFA),
    surface = Color(0xFFFBFCFA),
    surfaceContainer = Color(0xFFEFF4F1),
    surfaceContainerLow = Color(0xFFF5F0FA),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF5F6561)
)

@Composable
fun TeumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TeumLightColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
