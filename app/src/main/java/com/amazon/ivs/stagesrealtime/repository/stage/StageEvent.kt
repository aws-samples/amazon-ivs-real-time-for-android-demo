package com.amazon.ivs.stagesrealtime.repository.stage

import android.view.View
import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar

/**
 * A collection of events that can be triggered by the stage to deliver changes
 * to the UI.
 */
sealed interface StageEvent {
    data object StageGone : StageEvent
    data object CreatorLeft : StageEvent
    data object GuestLeft : StageEvent
    data object GuestSpeakingStateUpdated : StageEvent
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
    data class VideoStatsUpdated(
        val guestTTV: String? = null,
        val guestLatency: String = "",
        val creatorTTV: String? = null,
        val creatorLatency: String = "",
    ) : StageEvent
}
