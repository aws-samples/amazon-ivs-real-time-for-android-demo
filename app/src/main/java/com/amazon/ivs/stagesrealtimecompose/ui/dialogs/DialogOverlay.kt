package com.amazon.ivs.stagesrealtimecompose.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackSecondary
import timber.log.Timber

@Composable
fun DialogOverlay(
    contentAlignment: Alignment = Alignment.BottomCenter,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .imePadding()
            .fillMaxSize()
            .background(color = BlackSecondary.copy(alpha = 0.8f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        Timber.d("Dialog background clicked")
                        NavigationHandler.goBack()
                    }
                )
            }
            .padding(horizontal = 8.dp, vertical = 16.dp),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}
