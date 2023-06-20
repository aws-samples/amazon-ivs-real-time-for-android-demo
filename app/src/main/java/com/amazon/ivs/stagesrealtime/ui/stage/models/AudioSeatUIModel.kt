package com.amazon.ivs.stagesrealtime.ui.stage.models

import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar

data class AudioSeatUIModel(
    val id: Int,
    val participantId: String = "",
    val isMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val userAvatar: UserAvatar? = null
)
