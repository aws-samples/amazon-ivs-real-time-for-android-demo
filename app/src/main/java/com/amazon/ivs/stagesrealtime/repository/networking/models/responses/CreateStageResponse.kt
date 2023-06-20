package com.amazon.ivs.stagesrealtime.repository.networking.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class CreateStageResponse(
    val region: String,
    val hostParticipantToken: ParticipantToken
)

@Serializable
data class ParticipantToken(
    val token: String,
    val participantId: String,
    val duration: Int
)
