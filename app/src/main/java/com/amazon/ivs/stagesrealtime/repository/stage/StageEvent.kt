package com.amazon.ivs.stagesrealtime.repository.stage

import android.view.View
import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar

sealed interface StageEvent {
    object StageGone : StageEvent
    object CreatorLeft : StageEvent
    object GuestLeft : StageEvent
    object GuestSpeakingStateUpdated : StageEvent
    data class CreatorJoined(
        val participantId: String,
        val isAudioOff: Boolean,
        val isVideoOff: Boolean,
        val userAvatar: UserAvatar,
        val video: View?,
    ) : StageEvent
    data class GuestJoined(
        val participantId: String,
        val isAudioOff: Boolean,
        val isVideoOff: Boolean,
        val userAvatar: UserAvatar,
        val video: View?,
    ) : StageEvent
    data class CreatorUpdated(
        val participantId: String,
        val isAudioOff: Boolean?,
        val isVideoOff: Boolean?,
        val video: View?
    ) : StageEvent
    data class GuestUpdated(
        val participantId: String,
        val isAudioOff: Boolean?,
        val isVideoOff: Boolean?,
        val video: View?
    ) : StageEvent
    data class LocalParticipantUpdated(
        val participantId: String?,
        val isCreator: Boolean,
        val isParticipant: Boolean,
        val isAudioOff: Boolean,
        val isVideoOff: Boolean,
        val isFacingBack: Boolean,
        val video: View?
    ) : StageEvent
}
