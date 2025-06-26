package com.amazon.ivs.stagesrealtimecompose.ui.screens.stage.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.common.getNewUserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.common.mockMessages
import com.amazon.ivs.stagesrealtimecompose.core.common.mockVideoStage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.DialogDestination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.handlers.VSHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.chat.ChatHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.chat.StageMessage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.Stage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageParticipantMode
import com.amazon.ivs.stagesrealtimecompose.ui.components.AvatarImage
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonCircleImage
import com.amazon.ivs.stagesrealtimecompose.ui.components.DesktopPreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.GradientBox
import com.amazon.ivs.stagesrealtimecompose.ui.components.HeartBox
import com.amazon.ivs.stagesrealtimecompose.ui.components.LandscapePreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.PortraitPreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.components.SquarePreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.TextInput
import com.amazon.ivs.stagesrealtimecompose.ui.components.VoteButton
import com.amazon.ivs.stagesrealtimecompose.ui.components.fillMaxPortraitWidth
import com.amazon.ivs.stagesrealtimecompose.ui.components.imeOffsetPadding
import com.amazon.ivs.stagesrealtimecompose.ui.components.thenOptional
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackQuaternary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackQuinary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BluePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GreenSecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterTertiary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RedPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.ldp

@Composable
fun StageOverlay(
    stage: Stage,
    modifier: Modifier = Modifier,
) {
    val messages by ChatHandler.messages.collectAsStateWithLifecycle()

    StageOverlayContent(
        stage = stage,
        messages = messages,
        modifier = modifier,
    )
}

@Composable
private fun StageOverlayContent(
    stage: Stage,
    messages: List<StageMessage>,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .imeOffsetPadding()
            .fillMaxSize()
            .onSizeChanged {
                if (size == IntSize.Zero) {
                    size = it
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxPortraitWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom
            ) {
                ChatPanel(
                    avatar = stage.selfAvatar,
                    messages = messages,
                    parentSize = size,
                    isVSMode = stage.isVSMode,
                    modifier = Modifier.weight(1f)
                )
                SideMenu(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .padding(bottom = 20.dp),
                    stage = stage
                )
            }
            HeartBox(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 32.dp)
            )
        }
    }
}

@Composable
private fun ChatPanel(
    avatar: UserAvatar?,
    messages: List<StageMessage>,
    parentSize: IntSize,
    isVSMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val halfHeight = LocalDensity.current.run { (parentSize.height / 2).ldp }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .thenOptional(
                enabled = parentSize == IntSize.Zero
            ) {
                fillMaxHeight()
            }
            .thenOptional(
                enabled = parentSize != IntSize.Zero
            ) {
                height(halfHeight)
            }
            .padding(
                start = 8.dp,
                end = 8.dp,
                bottom = 16.dp
            ),
        verticalArrangement = Arrangement.Bottom
    ) {
        val topPadding by animateDpAsState(if (isVSMode) 12.dp else 0.dp)
        val bottomPadding by animateDpAsState(if (isVSMode) 20.dp else 0.dp)
        val backgroundColor by animateColorAsState(
            targetValue = if (isVSMode) BlackQuaternary.copy(alpha = 0.8f) else Color.Transparent
        )
        val shape = RoundedCornerShape(
            topStart = topPadding,
            topEnd = topPadding,
            bottomStart = bottomPadding,
            bottomEnd = bottomPadding
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp)
                .padding(bottom = 16.dp)
                .graphicsLayer { alpha = 0.99f }
                .drawWithContent {
                    val maskBrush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, WhitePrimary),
                        startY = 50f,
                        endY = size.height * 0.5f
                    )
                    drawContent()
                    drawRect(
                        brush = maskBrush,
                        blendMode = BlendMode.DstIn
                    )
                },
            verticalArrangement = Arrangement.Bottom
        ) {
            messages.forEach { message ->
                key(message.id) {
                    ChatItem(
                        message = message,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = backgroundColor, shape = shape)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = isVSMode
            ) {
                if (!isVSMode) return@AnimatedVisibility
                val score by VSHandler.score.collectAsStateWithLifecycle()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VoteButton(
                        modifier = Modifier.weight(1f),
                        votes = score.creatorScore,
                        iconBackground = RedPrimary,
                        onClick = {
                            StageHandler.castVote(forCreator = true)
                        }
                    )
                    VoteButton(
                        modifier = Modifier.weight(1f),
                        votes = score.participantScore,
                        iconBackground = BluePrimary,
                        onClick = {
                            StageHandler.castVote(forCreator = false)
                        }
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var message by remember { mutableStateOf("") }

                AvatarImage(
                    avatar = avatar,
                    size = 42.dp
                )
                TextInput(
                    modifier = Modifier.heightIn(min = 42.dp),
                    hint = stringResource(R.string.say_something),
                    text = message,
                    backgroundColor = BlackQuaternary.copy(alpha = 0.4f),
                    borderColor = Color.Transparent,
                    textColor = WhitePrimary,
                    hintColor = WhitePrimary,
                    imeAction = ImeAction.Send,
                    onImeAction = { text ->
                        ChatHandler.sendMessage(text)
                        message = ""
                    },
                    onValueChanged = { message = it }
                )
            }
        }
    }
}

@Composable
private fun SideMenu(
    stage: Stage,
    modifier: Modifier = Modifier,
) {
    val avatar = when {
        stage.isAudioRoom && !stage.isStageCreator -> stage.creatorAvatar
        stage.isStageCreator -> null
        else -> stage.creatorAvatar
    }

    Column(
        modifier = modifier.width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AnimatedVisibility(
            visible = avatar != null,
        ) {
            if (avatar == null) return@AnimatedVisibility
            AvatarImage(avatar)
        }
        AnimatedVisibility(
            visible = stage.isJoined,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ButtonCircleImage(
                image = if (stage.isSelfMicOff) R.drawable.ic_mic_off else R.drawable.ic_mic_on,
                tint = if (stage.isSelfMicOff) RedPrimary else WhitePrimary,
                background = if (stage.isSelfMicOff) WhitePrimary else BlackQuinary,
                description = stringResource(R.string.dsc_microphone_button),
                onClick = StageHandler::switchMic
            )
        }
        AnimatedVisibility(
            visible = stage.isJoined && !stage.isAudioRoom,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ButtonCircleImage(
                image = if (stage.isSelfVideoOff) R.drawable.ic_camera_off else R.drawable.ic_camera_on,
                tint = if (stage.isSelfVideoOff) RedPrimary else WhitePrimary,
                background = if (stage.isSelfVideoOff) WhitePrimary else BlackQuinary,
                description = stringResource(R.string.dsc_video_button),
                onClick = StageHandler::switchCamera
            )
        }
        AnimatedVisibility(
            visible = stage.isJoined && !stage.isAudioRoom,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ButtonCircleImage(
                image = R.drawable.ic_switch_camera,
                tint = if (stage.isFacingSwitched) BlackPrimary else WhitePrimary,
                background = if (stage.isFacingSwitched) WhitePrimary else BlackQuinary,
                description = stringResource(R.string.dsc_switch_camera_button),
                onClick = StageHandler::switchFacing
            )
        }
        AnimatedVisibility(
            visible = stage.isJoined,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ButtonCircleImage(
                image = R.drawable.ic_exit,
                tint = WhitePrimary,
                background = RedPrimary,
                description = stringResource(R.string.dsc_exit_button),
                onClick = {
                    if (stage.isStageCreator) {
                        NavigationHandler.showDialog(DialogDestination.EndStage)
                    } else if (stage.isAudioRoom) {
                        NavigationHandler.showDialog(DialogDestination.LeaveAudioRoom)
                    } else {
                        NavigationHandler.showDialog(DialogDestination.LeaveStage)
                    }
                }
            )
        }
        AnimatedVisibility(
            visible = stage.isJoined,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 1.dp)
                    .background(BlackQuaternary.copy(alpha = 0.8f))
            )
        }
        AnimatedVisibility(
            visible = stage.canJoin || stage.canKick,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ButtonCircleImage(
                image = if (stage.canJoin) R.drawable.ic_user_add else R.drawable.ic_user_remove,
                tint = WhitePrimary,
                background = BlackQuaternary.copy(alpha = 0.8f),
                description = stringResource(R.string.dsc_exit_button),
                onClick = {
                    if (stage.canJoin) {
                        NavigationHandler.showDialog(DialogDestination.JoinStage)
                    } else if (stage.canKick) {
                        NavigationHandler.showDialog(DialogDestination.KickParticipant)
                    } else if (stage.isAudioRoom) {
                        NavigationHandler.showDialog(DialogDestination.LeaveAudioRoom)
                    } else {
                        NavigationHandler.showDialog(DialogDestination.LeaveStage)
                    }
                }
            )
        }
        ButtonCircleImage(
            image = R.drawable.ic_heart,
            tint = WhitePrimary,
            background = BlackQuaternary.copy(alpha = 0.8f),
            description = stringResource(R.string.dsc_heart_button),
            onClick = {
                StageHandler.addHeart()
                ChatHandler.likeStage()
            }
        )
    }
}

@Composable
private fun ChatItem(
    message: StageMessage,
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = message) {
        isVisible = message.isVisible
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + shrinkVertically() + slideOutVertically(targetOffsetY = { it / -5 }),
    ) {
        when (message) {
            is StageMessage.SystemMessage -> {
                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .background(
                            color = BlackQuaternary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(100)
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.username != null) {
                        Text(
                            text = message.username,
                            style = InterTertiary.copy(fontSize = 14.sp)
                        )
                    }
                    Text(
                        text = message.message,
                        style = InterTertiary.copy(fontSize = 14.sp, fontWeight = FontWeight.W500)
                    )
                }
            }
            is StageMessage.UserMessage -> {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarImage(
                        avatar = message.avatar,
                        size = 42.dp
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = message.username,
                            style = InterTertiary.copy(fontSize = 14.sp, fontWeight = FontWeight.W600)
                        )
                        Text(
                            text = message.message,
                            style = InterTertiary.copy(fontSize = 14.sp, fontWeight = FontWeight.W400)
                        )
                    }
                }
            }
        }
    }
}

@PortraitPreview
@Composable
private fun OverlayPortraitVS() {
    StageOverlayContentPreview(
        mode = StageParticipantMode.VS
    )
}

@PortraitPreview
@Composable
private fun OverlayPortraitNone() {
    StageOverlayContentPreview()
}

@SquarePreview
@Composable
private fun OverlaySquareVS() {
    StageOverlayContentPreview(
        mode = StageParticipantMode.VS
    )
}

@SquarePreview
@Composable
private fun OverlaySquareNone() {
    StageOverlayContentPreview()
}

@LandscapePreview
@Composable
private fun OverlayLandscapeVS() {
    StageOverlayContentPreview(
        mode = StageParticipantMode.VS
    )
}

@LandscapePreview
@Composable
private fun OverlayLandscapeNone() {
    StageOverlayContentPreview()
}

@DesktopPreview
@Composable
private fun OverlayDesktopVS() {
    StageOverlayContentPreview(
        mode = StageParticipantMode.VS
    )
}

@DesktopPreview
@Composable
private fun OverlayDesktopNone() {
    StageOverlayContentPreview()
}

@Composable
private fun StageOverlayContentPreview(
    mode: StageParticipantMode = StageParticipantMode.None,
) {
    PreviewSurface {
        Box(
            modifier = Modifier.background(color = GreenSecondary)
        ) {
            GradientBox(isUp = true)
            GradientBox(isUp = false)
            StageOverlayContent(
                stage = mockVideoStage.copy(
                    mode = mode,
                    selfAvatar = getNewUserAvatar(),
                ),
                messages = mockMessages
            )
        }
    }
}
