package com.teum.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TeumLightColorScheme = lightColorScheme(
    primary = Color(0xFF5B5FEA),
    onPrimary = Color.White,
    secondary = Color(0xFF7F5BEA),
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFECEEFF),
    surfaceContainerLow = Color(0xFFF8F9FC),
    onSurface = Color(0xFF1F2430),
    onSurfaceVariant = Color(0xFF778092)
)

@Composable
fun TeumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TeumLightColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
