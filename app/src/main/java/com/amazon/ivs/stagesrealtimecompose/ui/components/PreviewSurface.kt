package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.amazon.ivs.stagesrealtimecompose.ui.theme.StagesRealtimeComposeTheme

@Composable
fun PreviewSurface(
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    StagesRealtimeComposeTheme {
        Surface(
            color = background
        ) {
            Box(
                modifier = modifier
            ) {
                content()
            }
        }
    }
}

@Preview(
    name = "Portrait Preview",
    widthDp = 392,
    heightDp = 851,
)
annotation class PortraitPreview

@Preview(
    name = "Square Preview",
    widthDp = 851,
    heightDp = 851,
)
annotation class SquarePreview

@Preview(
    name = "Landscape Preview",
    widthDp = 851,
    heightDp = 392,
)
annotation class LandscapePreview

@Preview(
    name = "Desktop Preview",
    widthDp = 1280,
    heightDp = 720,
)
annotation class DesktopPreview
