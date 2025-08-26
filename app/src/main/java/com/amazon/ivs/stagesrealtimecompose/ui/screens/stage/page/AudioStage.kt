package com.amazon.ivs.stagesrealtimecompose.ui.screens.stage.page

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.common.mockAudioStage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.AudioSeat
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.Stage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageHandler
import com.amazon.ivs.stagesrealtimecompose.ui.components.AvatarImage
import com.amazon.ivs.stagesrealtimecompose.ui.components.DesktopPreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.LandscapePreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.PortraitPreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.components.SquarePreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.fillMaxPortraitWidth
import com.amazon.ivs.stagesrealtimecompose.ui.components.isSquareOrLandscape
import com.amazon.ivs.stagesrealtimecompose.ui.screens.stage.common.StageOverlay
import com.amazon.ivs.stagesrealtimecompose.ui.theme.OrangePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary

@Composable
fun AudioStage(
    stage: Stage,
    topPadding: Dp
) {
    AudioStageContent(
        stage = stage,
        topPadding = topPadding
    )
}

@Composable
private fun AudioStageContent(
    stage: Stage,
    topPadding: Dp
) {
    val paddingExtra = if (isSquareOrLandscape()) 36.dp else 70.dp

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = R.drawable.bg_audio_stage,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding + paddingExtra)
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var index = 0
            repeat(3) {
                Row(
                    modifier = Modifier
                        .fillMaxPortraitWidth(
                            maxWidth = 550.dp
                        ),
                ) {
                    repeat(4) {
                        AudioSeat(
                            seat = stage.seats[index++],
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(ratio = if (isSquareOrLandscape()) 1f else 0.87f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioSeat(
    seat: AudioSeat,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current
    val shape = RoundedCornerShape(12.dp)
    var size by remember {
        mutableStateOf(
            value = if (isPreview) {
                IntSize(width = 150, height = 150)
            } else {
                IntSize.Zero
            }
        )
    }
    val seatSize = LocalDensity.current.run { minOf(size.width.toDp(), size.height.toDp()) }

    Box(
        modifier = modifier
            .padding(5.dp)
            .background(
                color = WhitePrimary.copy(alpha = 0.2f),
                shape = shape
            )
            .border(
                width = 2.dp,
                color = WhitePrimary.copy(alpha = 0.4f),
                shape = shape
            )
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = OrangePrimary),
                enabled = seat.isEmpty,
                onClick = {
                    StageHandler.joinAudioRoom(seat.id)
                }
            ),
    ) {
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = seat.isEmpty,
        ) { isEmpty ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.7f)
                        .onSizeChanged { size = it },
                    contentAlignment = Alignment.Center
                ) {
                    when (isEmpty) {
                        true -> {
                            Image(
                                modifier = Modifier.size(36.dp),
                                painter = painterResource(R.drawable.ic_plus),
                                contentDescription = stringResource(R.string.dsc_join_seat)
                            )
                        }

                        else -> {
                            AvatarImage(
                                modifier = Modifier.aspectRatio(1f),
                                avatar = seat.userAvatar,
                                isSpeaking = seat.isSpeaking,
                                isMuted = seat.isMuted,
                                size = seatSize
                            )
                        }
                    }
                }
            }
        }
    }
}

@PortraitPreview
@Composable
private fun AudioPortrait() {
    AudioStageContentPreview()
}

@SquarePreview
@Composable
private fun AudioSquare() {
    AudioStageContentPreview()
}

@LandscapePreview
@Composable
private fun AudioLandscape() {
    AudioStageContentPreview()
}

@DesktopPreview
@Composable
private fun AudioDesktop() {
    AudioStageContentPreview()
}

@Composable
private fun AudioStageContentPreview() {
    PreviewSurface {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            AudioStageContent(
                stage = mockAudioStage,
                topPadding = 0.dp
            )
            StageOverlay(
                stage = mockAudioStage
            )
        }
    }
}
