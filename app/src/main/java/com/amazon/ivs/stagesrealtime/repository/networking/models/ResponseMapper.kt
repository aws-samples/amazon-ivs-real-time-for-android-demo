@file:Suppress("LocalVariableName")

package com.amazon.ivs.stagesrealtime.repository.networking.models

import android.view.View
import com.amazon.ivs.stagesrealtime.common.emptySeats
import com.amazon.ivs.stagesrealtime.common.extensions.toDate
import com.amazon.ivs.stagesrealtime.repository.models.ParticipantAttributes
import com.amazon.ivs.stagesrealtime.repository.models.RTCData
import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar
import com.amazon.ivs.stagesrealtime.repository.networking.models.responses.ChatTokenResponse
import com.amazon.ivs.stagesrealtime.repository.networking.models.responses.StageResponse
import com.amazon.ivs.stagesrealtime.repository.stage.StageManager
import com.amazon.ivs.stagesrealtime.ui.stage.models.AudioSeatUIModel
import com.amazon.ivs.stagesrealtime.ui.stage.models.RTCDataUIItemModel
import com.amazon.ivs.stagesrealtime.ui.stage.models.StageUIModel
import com.amazonaws.ivs.chat.messaging.ChatToken
import timber.log.Timber

fun MutableList<StageUIModel>.updateOrAdd(stageId: String, stages: List<StageResponse>, selfAvatar: UserAvatar) {
    stages.forEachIndexed { index, newStage ->
        find { it.stageId == newStage.hostId }?.let { stage ->
            removeAt(index)
            add(
                index, stage.copy(
                    isCreator = newStage.hostId == stageId,
                    isGuestMode = newStage.mode == StageMode.GUEST_SPOT,
                    isPKMode = newStage.mode == StageMode.PK,
                    creatorAvatar = UserAvatar(
                        colorLeft = newStage.hostAttributes.avatarColLeft,
                        colorRight = newStage.hostAttributes.avatarColRight,
                        colorBottom = newStage.hostAttributes.avatarColBottom,
                    ),
                    selfAvatar = selfAvatar,
                    seats = newStage.seats?.asSeatList() ?: stage.seats
                )
            )
        } ?: add(newStage.toStageUIModel(stageId, selfAvatar))
    }
}

/**
 * A convenient function to update the current [StageUIModel] by modifying ONLY the values that are not null
 * given the index for the stage in the list.
 */
fun MutableList<StageUIModel>.update(
    index: Int,
    keepCreatorVideo: Boolean = true,
    keepGuestVideo: Boolean = true,
    participantId: String? = null,
    _isCreator: Boolean? = null,
    _isParticipant: Boolean? = null,
    _isCreatorAudioOff: Boolean? = null,
    _isCreatorVideoOff: Boolean? = null,
    _isCameraSwitched: Boolean? = null,
    _isStageAudioOff: Boolean? = null,
    _isGuestAudioOff: Boolean? = null,
    _isGuestVideoOff: Boolean? = null,
    _isLocalAudioOff: Boolean? = null,
    _isGuestJoined: Boolean? = null,
    _isGuestMode: Boolean? = null,
    _isSpeaking: Boolean? = null,
    _isPKMode: Boolean? = null,
    _isAudioMode: Boolean? = null,
    _creatorVideo: View? = null,
    _guestVideo: View? = null,
    _creatorAvatar: UserAvatar? = null,
    _selfAvatar: UserAvatar? = null,
    _seats: List<AudioSeatUIModel>? = null,
) = try {
    getOrNull(index)?.let { stage ->
        val isCreator = _isCreator ?: stage.isCreator
        val isParticipant = _isParticipant ?: stage.isParticipant
        val isCreatorAudioOff = _isCreatorAudioOff ?: stage.isCreatorAudioOff
        val isCreatorVideoOff = _isCreatorVideoOff ?: stage.isCreatorVideoOff
        val isCameraSwitched = _isCameraSwitched ?: stage.isCameraSwitched
        val isStageAudioOff = _isStageAudioOff ?: stage.isStageAudioOff
        val isGuestAudioOff = _isGuestAudioOff ?: stage.isGuestAudioOff
        val isGuestVideoOff = _isGuestVideoOff ?: stage.isGuestVideoOff
        val isLocalAudioOff = _isLocalAudioOff ?: stage.isLocalAudioOff
        val isGuestJoined = _isGuestJoined ?: stage.isGuestJoined
        val isGuestMode = _isGuestMode ?: stage.isGuestMode
        val isSpeaking = _isSpeaking ?: stage.isSpeaking
        val isPKMode = _isPKMode ?: stage.isPKMode
        val isAudioMode = _isAudioMode ?: stage.isAudioMode
        val creatorVideo = if (keepCreatorVideo) _creatorVideo ?: stage.creatorVideo else _creatorVideo
        val guestVideo = if (keepGuestVideo) _guestVideo ?: stage.guestVideo else _guestVideo
        val creatorAvatar = _creatorAvatar ?: stage.creatorAvatar
        val selfAvatar = _selfAvatar ?: stage.selfAvatar
        val seats = _seats ?: stage.seats.map {
            if (it.participantId == participantId) {
                it.copy(
                    isMuted = if (isCreator) isCreatorAudioOff else isGuestAudioOff,
                    userAvatar = if (isCreator) creatorAvatar else selfAvatar
                )
            } else {
                it.copy()
            }
        }
        removeAt(index)
        add(
            index, stage.copy(
                isCreator = isCreator,
                isParticipant = isParticipant,
                isCreatorAudioOff = isCreatorAudioOff,
                isCreatorVideoOff = isCreatorVideoOff,
                isCameraSwitched = isCameraSwitched,
                isStageAudioOff = isStageAudioOff,
                isGuestAudioOff = isGuestAudioOff,
                isGuestVideoOff = isGuestVideoOff,
                isLocalAudioOff = isLocalAudioOff,
                isGuestJoined = isGuestJoined,
                isGuestMode = isGuestMode,
                isSpeaking = isSpeaking,
                isPKMode = isPKMode,
                isAudioMode = isAudioMode,
                creatorVideo = creatorVideo,
                guestVideo = guestVideo,
                creatorAvatar = creatorAvatar,
                selfAvatar = selfAvatar,
                seats = seats,
            )
        )
    }
} catch (e: Exception) {
    Timber.e(e, "Failed to update current stage")
}

suspend fun MutableList<StageUIModel>.updateSeats(
    index: Int,
    stageManager: StageManager,
    getParticipantAttributes: suspend (String) -> ParticipantAttributes?,
    seatIds: List<String>? = null
) {
    getOrNull(index)?.let { stage ->
        val updatedSeats = stage.seats.mapIndexed { index, seat ->
            val id = seatIds?.getOrNull(index) ?: seat.participantId
            val attributes = getParticipantAttributes(id)
            val isMuted = attributes?.isMuted ?: false
            seat.copy(
                participantId = id,
                userAvatar = attributes?.userAvatar,
                isMuted = isMuted,
                isSpeaking = if (isMuted || attributes?.isSpeaking == null) false else attributes.isSpeaking
            )
        }
        if (stage.isCreator && stage.isAudioMode) {
            val hasParticipants = updatedSeats.count { it.participantId.isNotBlank() } > 1
            val mode = if (hasParticipants) StageMode.AUDIO else StageMode.NONE
            stageManager.updateMode(StageType.AUDIO, mode)
        }
        removeAt(index)
        add(index, stage.copy(seats = updatedSeats))
    }
}

fun ChatTokenResponse.asChatToken() = ChatToken(
    token = token,
    sessionExpirationTime = sessionExpirationTime.toDate(),
    tokenExpirationTime = tokenExpirationTime.toDate()
)

fun List<String>.asSeatList() = mapIndexed { index, id ->
    AudioSeatUIModel(id = index, participantId = id)
}

fun List<AudioSeatUIModel>.getIDs() = map { it.participantId }

fun RTCData.asRTCUIData(isForAudio: Boolean = false) = RTCDataUIItemModel(
    latency = latency,
    fps = fps,
    packetsLost = packetLoss,
    isForAudio = isForAudio,
    username = userInfo?.stageId ?: "",
    participantId = userInfo?.participantId ?: "",
    isHost = isHostData,
    isGuest = isGuestData
)

fun List<RTCData>.asRTCUIDataList(isForAudio: Boolean = false) = map { it.asRTCUIData(isForAudio) }

private fun StageResponse.toStageUIModel(stageId: String, selfAvatar: UserAvatar) = StageUIModel(
    stageId = hostId,
    isCreator = hostId == stageId,
    isGuestMode = mode == StageMode.GUEST_SPOT,
    isPKMode = mode == StageMode.PK,
    isAudioMode = type == StageType.AUDIO,
    creatorAvatar = UserAvatar(
        colorLeft = hostAttributes.avatarColLeft,
        colorRight = hostAttributes.avatarColRight,
        colorBottom = hostAttributes.avatarColBottom,
    ),
    selfAvatar = selfAvatar,
    seats = seats?.asSeatList() ?: emptySeats
)
