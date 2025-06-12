package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
