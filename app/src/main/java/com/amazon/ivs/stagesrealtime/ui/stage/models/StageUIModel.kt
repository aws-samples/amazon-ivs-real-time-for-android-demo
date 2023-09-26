package com.amazon.ivs.stagesrealtime.ui.stage.models

import android.view.View
import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar

data class StageUIModel(
    val stageId: String,
    val isCreator: Boolean = false,
    val isParticipant: Boolean = false,
    val isCameraSwitched: Boolean = false,
    val isStageAudioOff: Boolean = false,
    val isCreatorAudioOff: Boolean = false,
    val isCreatorVideoOff: Boolean = false,
    val isGuestAudioOff: Boolean = false,
    val isGuestVideoOff: Boolean = false,
    val isLocalAudioOff: Boolean = false,
    val isGuestJoined: Boolean = false,
    val isGuestMode: Boolean = false,
    val isSpeaking: Boolean = false,
    val isPKMode: Boolean = false,
    val isAudioMode: Boolean = false,
    val isVideoStatsEnabled: Boolean = true,
    val creatorVideo: View? = null,
    val guestVideo: View? = null,
    val creatorAvatar: UserAvatar? = null,
    val selfAvatar: UserAvatar? = null,
    val seats: List<AudioSeatUIModel> = emptyList(),
    val guestTTV: String? = null,
    val guestLatency: String? = null,
    val creatorTTV: String? = null,
    val creatorLatency: String? = null
) {
    fun showKickButton() = !isAudioMode &&
            ((isCreator && isGuestJoined) || (!isCreator && !isGuestJoined && !isPKMode && !isGuestMode))

    fun isVideoOff() = if (isCreator) isCreatorVideoOff else isGuestVideoOff

    fun showGlobalProgressBar() = creatorVideo == null && !isCreatorVideoOff && !isPKMode
}
