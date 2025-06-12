package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackSecondary

@Composable
fun LoadingOverlay(
    isLoading: Boolean
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = BlackSecondary.copy(alpha = 0.8f))
                .unClickable(),
            contentAlignment = Alignment.Center
        ) {
            LoadingSpinner(isLoading = isLoading)
        }
    }
}

@Preview
@Composable
private fun LoadingOverlayPreview() {
    PreviewSurface {
        LoadingOverlay(
            isLoading = true
        )
    }
}
