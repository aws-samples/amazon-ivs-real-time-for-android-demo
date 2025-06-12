package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.OrangePrimary

@Composable
fun BoxScope.GradientBox(
    isUp: Boolean,
    topPadding: Dp = 0.dp
) {
    val height = (if (isUp) 113.dp + topPadding else 300.dp)
    val colors = if (isUp) {
        listOf(
            BlackPrimary.copy(0.6f),
            BlackPrimary.copy(0f)
        )
    } else {
        listOf(
            BlackPrimary.copy(0f),
            BlackPrimary.copy(0.6f),
            BlackPrimary.copy(0.8f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .align(if (isUp) Alignment.TopCenter else Alignment.BottomCenter)
            .imeOffsetPadding(
                enabled = !isUp
            )
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = colors,
                        endY = height.toPx()
                    )
                )
            }
    )
}

@Preview
@Composable
private fun GradientBoxPreview() {
    PreviewSurface {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OrangePrimary)
        ) {
            GradientBox(isUp = true)
            GradientBox(isUp = false)
        }
    }
}
