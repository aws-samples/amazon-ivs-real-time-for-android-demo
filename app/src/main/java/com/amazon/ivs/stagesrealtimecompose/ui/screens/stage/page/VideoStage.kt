package com.amazon.ivs.stagesrealtimecompose.ui.screens.stage.page

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.toTimerSeconds
import com.amazon.ivs.stagesrealtimecompose.core.common.mockVideoStage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.VSHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.ActiveVideoStream
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.Stage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageManager
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageParticipantMode
import com.amazon.ivs.stagesrealtimecompose.ui.components.CameraOffBox
import com.amazon.ivs.stagesrealtimecompose.ui.components.LatencyText
import com.amazon.ivs.stagesrealtimecompose.ui.components.LoadingSpinner
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.components.ScoreBar
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackSecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterTertiary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.ldp
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun VideoStage(
    stage: Stage,
) {
    VideoStageContent(
        stage = stage,
    )
}

@Composable
private fun VideoStageContent(
    stage: Stage,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = stage.isVSMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = R.drawable.bg_pk_vs,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = stage.mode
        ) { mode ->
            when (mode) {
                StageParticipantMode.Guest -> StageVideoGuest(
                    stage = stage,
                )
                StageParticipantMode.VS -> StageVideoVS(
                    stage = stage,
                )
                StageParticipantMode.None -> {
                    val isPreview = LocalInspectionMode.current
                    val stream = when {
                        isPreview -> ActiveVideoStream()
                        else -> StageManager.activeCreatorStream.collectAsStateWithLifecycle().value
                    }

                    StageVideoBox(
                        modifier = Modifier.fillMaxSize(),
                        isCreator = true,
                        stream = stream,
                        showStats = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.StageVideoGuest(
    stage: Stage,
) {
    val shape = RoundedCornerShape(18.dp)
    val isPreview = LocalInspectionMode.current
    val creatorStream = when {
        isPreview -> ActiveVideoStream()
        else -> StageManager.activeCreatorStream.collectAsStateWithLifecycle().value
    }

    StageVideoBox(
        modifier = Modifier.fillMaxSize(),
        isCreator = true,
        stream = creatorStream,
        showStats = false,
    )

    AnimatedVisibility(
        modifier = Modifier
            .padding(end = 16.dp, top = 60.dp)
            .size(
                width = 140.dp,
                height = 200.dp
            )
            .border(
                width = 1.dp,
                shape = shape,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        WhitePrimary,
                        WhitePrimary.copy(alpha = 0.3f)
                    )
                )
            )
            .shadow(elevation = 6.dp, shape = shape)
            .background(color = BlackSecondary, shape = shape)
            .clip(shape = shape)
            .align(Alignment.TopEnd),
        visible = stage.isParticipantJoined,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (!stage.isParticipantJoined) return@AnimatedVisibility
        val participantStream = when {
            isPreview -> ActiveVideoStream()
            else -> StageManager.activeParticipantStream.collectAsStateWithLifecycle().value
        }

        StageVideoBox(
            modifier = Modifier.fillMaxSize(),
            isCreator = false,
            stream = participantStream,
            showStats = true,
        )
    }
}

@Composable
private fun StageVideoVS(
    stage: Stage,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .fillMaxHeight(0.7f),
        contentAlignment = Alignment.Center
    ) {
        var participantSize by remember { mutableStateOf(IntSize.Zero) }
        var fullSize by remember { mutableStateOf(IntSize.Zero) }
        val ratio = 16f / 25f
        val fullWidth = fullSize.width.ldp
        val participantHeight = participantSize.height.ldp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fullSize = it },
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { participantSize = it }
                ) {
                    val isPreview = LocalInspectionMode.current
                    val creatorStream = when {
                        isPreview -> ActiveVideoStream()
                        else -> StageManager.activeCreatorStream.collectAsStateWithLifecycle().value
                    }

                    StageVideoBox(
                        modifier = Modifier.weight(1f)
                            .aspectRatio(ratio = ratio)
                            .background(BlackSecondary),
                        isCreator = true,
                        stream = creatorStream,
                        showStats = true,
                    )
                    Box(
                        modifier = Modifier.weight(1f)
                            .aspectRatio(ratio = ratio)
                            .background(BlackSecondary)
                    ) {
                        LoadingSpinner(
                            modifier = Modifier.align(Alignment.Center),
                            isLoading = !stage.isParticipantJoined
                        )
                        this@Row.AnimatedVisibility(
                            modifier = Modifier.fillMaxSize(),
                            visible = stage.isParticipantJoined,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            if (!stage.isParticipantJoined) return@AnimatedVisibility
                            val isPreview = LocalInspectionMode.current
                            val participantStream = when {
                                isPreview -> ActiveVideoStream()
                                else -> StageManager.activeParticipantStream.collectAsStateWithLifecycle().value
                            }

                            StageVideoBox(
                                modifier = Modifier.fillMaxSize(),
                                isCreator = false,
                                stream = participantStream,
                                showStats = true,
                            )
                        }
                    }
                }
                this@Column.AnimatedVisibility(
                    modifier = Modifier
                        .height(participantHeight)
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    visible = participantSize != IntSize.Zero,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (participantSize == IntSize.Zero) return@AnimatedVisibility

                    AnimatedLightning(
                        height = participantHeight
                    )
                }
                VSCounter(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                )
            }
            AnimatedVisibility(
                visible = participantSize != IntSize.Zero,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (participantSize == IntSize.Zero) return@AnimatedVisibility

                ScoreBar(width = fullWidth)
            }
        }
        AsyncImage(
            modifier = Modifier
                .size(128.dp)
                .align(Alignment.Center),
            model = R.drawable.ic_pk,
            contentScale = ContentScale.FillBounds,
            contentDescription = null
        )
    }
}

@Composable
private fun AnimatedLightning(
    height: Dp,
) {
    val lightningCount = 7
    val lightningHeight = height / lightningCount
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(WhitePrimary.copy(alpha = 0.75f))
        )
        Column(
            modifier = Modifier.fillMaxHeight()
                .drawBehind {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                WhitePrimary.copy(alpha - 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                }
        ) {
            (0..lightningCount).forEach { index ->
                key(index) {
                    val scale by infiniteTransition.animateFloat(
                        initialValue = -1f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    AsyncImage(
                        modifier = Modifier
                            .height(lightningHeight)
                            .width(20.dp)
                            .alpha(alpha)
                            .scale(scaleX = scale, scaleY = 1f),
                        model = R.drawable.ic_lightning,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }
    }
}

@Composable
private fun StageVideoBox(
    modifier: Modifier,
    showStats: Boolean,
    isCreator: Boolean,
    stream: ActiveVideoStream,
) {
    var video by remember { mutableStateOf(stream.video) }

    LaunchedEffect(key1 = Unit) {
        while (video == null) {
            delay(500)
            val preview = stream.video
            if (preview != null) {
                Timber.d("Video preview updated: $preview, ${stream.isOff}")
                video = preview
            }
        }
    }

    LaunchedEffect(key1 = stream) {
        val preview = stream.video
        Timber.d("Video view stream changed: $preview, ${stream.isOff}")
        video = preview
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Timber.d("SHOWING VIDEO: $video, ${stream.isOff}")
        CameraOffBox()
        AnimatedVisibility(
            visible = !stream.isOff && video != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    FrameLayout(context).apply {
                        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                }, update = { layout ->
                    val preview = if (!stream.isOff) {
                        video ?: return@AndroidView
                    } else {
                        return@AndroidView
                    }
                    Timber.d("Video view - updating view: $preview")
                    layout.removeView(preview)
                    (preview.parent as? ViewGroup)?.removeView(preview)
                    layout.addView(preview)
                }
            )
        }

        LatencyText(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp),
            isVisible = showStats,
            isCreator = isCreator,
        )
    }
}

@Composable
private fun VSCounter(
    modifier: Modifier = Modifier,
) {
    val time by VSHandler.scoreTimer.collectAsStateWithLifecycle()
    val showTimer by VSHandler.showTimer.collectAsStateWithLifecycle()

    AnimatedVisibility(
        modifier = modifier.fillMaxWidth(),
        visible = showTimer,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier
                    .width(52.dp)
                    .background(color = BlackPrimary, shape = RoundedCornerShape(100))
                    .padding(
                        horizontal = 9.dp,
                        vertical = 3.dp
                    ),
                text = time.toTimerSeconds(),
                textAlign = TextAlign.Center,
                style = InterTertiary.copy(
                    fontWeight = FontWeight.W500,
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Preview
@Composable
private fun VideoStageNonePreview() {
    VideoStageContentPreview(mode = StageParticipantMode.None)
}

@Preview
@Composable
private fun VideoStageGuestPreview() {
    VideoStageContentPreview(mode = StageParticipantMode.Guest)
}

@Preview
@Composable
private fun VideoStageVSPreview() {
    VideoStageContentPreview(mode = StageParticipantMode.VS)
}

@Composable
private fun VideoStageContentPreview(
    mode: StageParticipantMode,
) {
    PreviewSurface(
        background = BlackSecondary
    ) {
        VideoStageContent(
            stage = mockVideoStage.copy(
                mode = mode,
                isCreatorVideoOff = false,
                isParticipantVideoOff = true
            ),
        )
    }
}
