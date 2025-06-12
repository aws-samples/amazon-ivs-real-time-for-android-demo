package com.amazon.ivs.stagesrealtimecompose.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = darkColorScheme(
    primary = BlackSecondary,
    secondary = BlackSecondary,
    tertiary = BlackSecondary,
    background = BlackSecondary,
    surface = BlackSecondary,
    onPrimary = WhitePrimary,
    onSecondary = WhitePrimary,
    onTertiary = WhitePrimary,
    onSurface = WhitePrimary
)

@Composable
fun StagesRealtimeComposeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content
    )
}
