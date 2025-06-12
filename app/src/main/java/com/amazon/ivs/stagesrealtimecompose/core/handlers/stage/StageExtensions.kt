package com.amazon.ivs.stagesrealtimecompose.core.handlers.stage

import com.amazon.ivs.stagesrealtimecompose.core.common.mockSeats
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.StageModeLegacy
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.StageResponse
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.StageTypeLegacy
import com.amazonaws.ivs.broadcast.ParticipantInfo

fun List<Stage>.update(response: List<StageResponse>) = response.map { stage ->
    find { it.stageId == stage.hostId }?.run {
        update(stage)
    } ?: Stage(stageId = stage.hostId).update(stage)
}

fun StageModeLegacy.toStageParticipantMode() = when (this) {
    StageModeLegacy.PK -> StageParticipantMode.VS
    StageModeLegacy.GUEST_SPOT -> StageParticipantMode.Guest
    else -> StageParticipantMode.None
}

fun StageParticipantMode.asStageModeLegacy() = when (this) {
    StageParticipantMode.Guest -> StageModeLegacy.GUEST_SPOT
    StageParticipantMode.VS -> StageModeLegacy.PK
    StageParticipantMode.None -> StageModeLegacy.NONE
}

fun ParticipantInfo.toActualString() = "Participant: $participantId, $isLocal, $userId, $attributes"

fun createSeats(participantId: String, userAvatar: UserAvatar) = mockSeats.toMutableList().apply {
    removeAt(0)
    add(
        index = 0,
        element = AudioSeat(
            id = 0,
            participantId = participantId,
            userAvatar = userAvatar
        )
    )
}

private fun Stage.update(stage: StageResponse): Stage {
    val user = UserHandler.currentUser
    return copy(
        isStageCreator = stage.hostId == user.username,
        type = when (stage.type) {
            StageTypeLegacy.AUDIO -> StageType.Audio
            StageTypeLegacy.VIDEO -> StageType.Video
        },
        mode = stage.mode.toStageParticipantMode(),
        creatorAvatar = UserAvatar(
            colorLeft = stage.hostAttributes.avatarColLeft,
            colorRight = stage.hostAttributes.avatarColRight,
            colorBottom = stage.hostAttributes.avatarColBottom,
        ),
        selfAvatar = user.userAvatar,
        seats = stage.seats?.mapIndexed { id, participantId ->
            val participant = StageManager.getParticipant(participantId)
            AudioSeat(
                id = id,
                participantId = participantId.takeIf { it.isNotBlank() },
                userAvatar = participant?.userAvatar,
                isMuted = participant?.isAudioOff == true,
                isSpeaking = participant?.isSpeaking == true,
            )
        } ?: seats
    )
}
