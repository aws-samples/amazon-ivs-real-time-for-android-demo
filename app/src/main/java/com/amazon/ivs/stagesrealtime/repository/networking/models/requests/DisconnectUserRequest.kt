package com.amazon.ivs.stagesrealtime.repository.networking.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class DisconnectUserRequest(
    val hostId: String,
    val userId: String,
    val participantId: String
)
