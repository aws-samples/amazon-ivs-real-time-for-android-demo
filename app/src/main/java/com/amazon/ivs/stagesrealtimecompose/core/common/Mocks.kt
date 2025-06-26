package com.amazon.ivs.stagesrealtimecompose.core.common

import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.toHexA
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.handlers.chat.StageMessage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.AudioSeat
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.RTCData
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.Stage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageParticipantMode
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageType
import com.amazon.ivs.stagesrealtimecompose.ui.theme.AvatarUnknownBottom
import com.amazon.ivs.stagesrealtimecompose.ui.theme.AvatarUnknownLeft
import com.amazon.ivs.stagesrealtimecompose.ui.theme.AvatarUnknownRight

private val creatorAvatar = getNewUserAvatar()

val mockSeats = (0 .. 11).map { index ->
    AudioSeat(
        id = index,
        participantId = if (index == 0) getNewStageId() else null,
        userAvatar = if (index == 0) creatorAvatar.copy(hasBorder = true) else null,
    )
}

val mockEmptyStage = Stage(stageId = "", isLoading = true)

val mockVideoStage = Stage(
    stageId = "video",
    isLoading = false,
    isStageCreator = true,
    creatorAvatar = creatorAvatar,
)

val mockAudioStage = Stage(
    stageId = "audio",
    isLoading = false,
    creatorAvatar = creatorAvatar,
    selfAvatar = getNewUserAvatar(),
    type = StageType.Audio,
    seats = mockSeats,
    isStageParticipant = true,
)

val mockStages = listOf(
    mockVideoStage,
    mockAudioStage,
    Stage(
        stageId = "3",
        isLoading = true
    ),
    Stage(
        stageId = "4",
        isLoading = false,
        isStageCreator = true,
        mode = StageParticipantMode.Guest
    ),
    Stage(
        stageId = "5",
        isStageCreator = true,
        mode = StageParticipantMode.VS
    )
)

val mockMessages = listOf(
    StageMessage.UserMessage(
        messageId = "1",
        username = "CranberryPee",
        message = "Hello",
        avatar = getNewUserAvatar()
    ),
    StageMessage.SystemMessage(messageId = "2", username = "StrawberryPoop", message = "joined"),
    StageMessage.SystemMessage(messageId = "3", username = "StrawberryPoop", message = "is on stage")
)

val mockRTCData = RTCData(
    isCreator = true,
    streamQuality = RTCData.StreamQuality.Limited,
    cpuLimitedTime = "120",
    networkLimitedTime = "18",
    latency = "36",
    fps = "14",
    packetLoss = "0",
)

val unknownAvatar = UserAvatar(
    colorLeft = AvatarUnknownLeft.toHexA(),
    colorRight = AvatarUnknownRight.toHexA(),
    colorBottom = AvatarUnknownBottom.toHexA(),
    hasBorder = true
)
