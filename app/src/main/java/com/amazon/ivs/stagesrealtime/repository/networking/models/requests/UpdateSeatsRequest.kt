package com.amazon.ivs.stagesrealtime.repository.networking.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class UpdateSeatsRequest(
    val hostId: String,
    val userId: String,
    val seats: List<String>
)
