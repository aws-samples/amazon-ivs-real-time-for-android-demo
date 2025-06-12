package com.amazon.ivs.stagesrealtimecompose.ui.screens.stage.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amazon.ivs.stagesrealtimecompose.core.common.ANIMATION_DURATION_MEDIUM
import com.amazon.ivs.stagesrealtimecompose.core.common.mockEmptyStage
import com.amazon.ivs.stagesrealtimecompose.core.common.mockVideoStage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.Stage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageParticipantMode
import com.amazon.ivs.stagesrealtimecompose.ui.components.GradientBox
import com.amazon.ivs.stagesrealtimecompose.ui.components.LatencyText
import com.amazon.ivs.stagesrealtimecompose.ui.components.LoadingSpinner
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StagePage(
    stage: Stage,
    isActiveStage: Boolean,
    topPadding: Dp
) {
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = stage.isLoading) {
        if (stage.isLoading) {
            isLoading = true
            return@LaunchedEffect
        }

        scope.launch {
            delay(ANIMATION_DURATION_MEDIUM)
            isLoading = false
        }
    }

    StagePageContent(
        stage = stage,
        isActiveStage = isActiveStage,
        topPadding = topPadding,
        isLoading = isLoading
    )
}

@Composable
private fun StagePageContent(
    stage: Stage,
    topPadding: Dp,
    isActiveStage: Boolean,
    isLoading: Boolean
) {
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxSize()
            .background(color = BlackTertiary, shape = shape)
            .clip(shape),
    ) {
        LoadingSpinner(
            modifier = Modifier.align(Alignment.Center),
            isLoading = stage.isLoading
        )

        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = !isLoading && isActiveStage,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box {
                Crossfade(
                    targetState = stage.isAudioRoom
                ) { isAudioMode ->
                    when (isAudioMode) {
                        true -> AudioStage(
                            stage = stage,
                            topPadding = topPadding
                        )
                        else -> VideoStage(
                            stage = stage,
                        )
                    }
                }
                GradientBox(isUp = true, topPadding = topPadding)
                GradientBox(isUp = false)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = topPadding)
                        .height(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LatencyText(
                        isVisible = !stage.isAudioRoom && stage.mode != StageParticipantMode.VS,
                        isCreator = true,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun StagePageContentPreview() {
    PreviewSurface {
        StagePageContent(
            stage = mockVideoStage,
            topPadding = 0.dp,
            isLoading = false,
            isActiveStage = true,
        )
    }
}

@Preview
@Composable
private fun EmptyStagePageContentPreview() {
    PreviewSurface {
        StagePageContent(
            stage = mockEmptyStage,
            topPadding = 0.dp,
            isLoading = false,
            isActiveStage = true,
        )
    }
}

@Preview
@Composable
private fun InActiveStagePageContentPreview() {
    PreviewSurface {
        StagePageContent(
            stage = mockEmptyStage,
            topPadding = 0.dp,
            isLoading = false,
            isActiveStage = false,
        )
    }
}
