package com.amazon.ivs.stagesrealtime.repository.networking.models.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoteRequest(
    val hostId: String,
    @SerialName("vote")
    val userIdVote: String
)
