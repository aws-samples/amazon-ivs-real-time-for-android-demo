package com.amazon.ivs.stagesrealtime.repository.networking.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class ChatTokenResponse(
    val token: String,
    val sessionExpirationTime: String,
    val tokenExpirationTime: String
)
