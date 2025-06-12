package com.amazon.ivs.stagesrealtimecompose.ui.screens.stage.page

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.common.mockAudioStage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.AudioSeat
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.Stage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageHandler
import com.amazon.ivs.stagesrealtimecompose.ui.components.AvatarImage
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
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
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = R.drawable.bg_audio_stage,
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding + 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                columns = GridCells.Fixed(4)
            ) {
                items(items = stage.seats, key = { it.id }) { seat ->
                    AudioSeat(
                        seat = seat
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioSeat(
    seat: AudioSeat
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .padding(4.dp)
            .requiredSize(DpSize(width = 80.dp, height = 94.dp))
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
            )
            .padding(horizontal = 10.dp)
            .padding(vertical = 16.dp),
    ) {
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = seat.isEmpty,
        ) { isEmpty ->
            Box(
                modifier = Modifier.fillMaxSize(),
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
                            avatar = seat.userAvatar,
                            isSpeaking = seat.isSpeaking,
                            isMuted = seat.isMuted,
                            size = 60.dp
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AudioStageContentPreview() {
    PreviewSurface {
        AudioStageContent(
            stage = mockAudioStage,
            topPadding = 0.dp
        )
    }
}
