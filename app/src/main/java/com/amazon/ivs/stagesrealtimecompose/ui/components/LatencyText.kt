package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.handlers.PreferencesHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.RTCStats
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageManager
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterStats

@Composable
fun LatencyText(
    isVisible: Boolean,
    isCreator: Boolean,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current
    val statsEnabled = if (isPreview) true else PreferencesHandler.videoStatsEnabled
    val rtcStats = if (isPreview) RTCStats() else StageManager.rtcStats.collectAsStateWithLifecycle().value

    LatencyTextContent(
        isVisible = isVisible && statsEnabled,
        modifier = modifier,
        isCreator = isCreator,
        rtcStats = rtcStats,
    )
}

@Composable
private fun LatencyTextContent(
    isVisible: Boolean,
    isCreator: Boolean,
    rtcStats: RTCStats,
    modifier: Modifier = Modifier,
) {
    if (isCreator && rtcStats.creatorLatency.isBlank()) return
    if (!isCreator && rtcStats.participantLatency.isBlank()) return

    val creatorStats = if (rtcStats.creatorTTV.isNotBlank()) {
        stringResource(R.string.video_stats_pattern, rtcStats.creatorTTV, rtcStats.creatorLatency)
    } else {
        stringResource(R.string.video_latency_pattern, rtcStats.creatorLatency)
    }
    val participantStats = if (rtcStats.participantTTV.isNotBlank()) {
        stringResource(R.string.video_stats_pattern, rtcStats.participantTTV, rtcStats.participantLatency)
    } else {
        stringResource(R.string.video_latency_pattern, rtcStats.participantLatency)
    }

    AnimatedVisibility(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 16.dp),
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 500)
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 500)
        )
    ) {
        Box(
            contentAlignment = Alignment.TopEnd,
        ) {
            Text(
                text = if (isCreator) creatorStats else participantStats,
                style = InterStats.copy()
            )
        }
    }
}

@Preview
@Composable
private fun LatencyTextPreview() {
    PreviewSurface {
        LatencyTextContent(
            isVisible = true,
            isCreator = true,
            rtcStats = RTCStats(
                creatorLatency = "123",
                creatorTTV = "123",
            )
        )
    }
}
