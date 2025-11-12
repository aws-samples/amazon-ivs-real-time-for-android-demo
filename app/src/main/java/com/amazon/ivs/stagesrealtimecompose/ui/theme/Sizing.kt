package com.amazon.ivs.stagesrealtimecompose.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

val Int.ldp @Composable get() = with (LocalDensity.current) {
    toDp()
}
