package com.amazon.ivs.stagesrealtime.repository.models

import com.amazonaws.ivs.broadcast.StageStream
import kotlinx.coroutines.Job

data class ParticipantAttributes(
    val stageId: String,
    val participantId: String,
    var isMuted: Boolean,
    var isSpeaking: Boolean = false,
    val userAvatar: UserAvatar,
    val isHost:Boolean,
    val audioStream: StageStream? = null,
    val videoStream: StageStream? = null,
    var speakingJob: Job? = null
)
