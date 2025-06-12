package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary

@Composable
fun CameraOffBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(44.dp),
            painter = painterResource(R.drawable.ic_camera_off),
            contentDescription = null,
            colorFilter = ColorFilter.tint(WhitePrimary.copy(alpha = 0.5f))
        )
    }
}
