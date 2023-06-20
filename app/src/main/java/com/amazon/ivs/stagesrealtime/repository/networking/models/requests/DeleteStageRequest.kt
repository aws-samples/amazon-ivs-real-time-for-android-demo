package com.amazon.ivs.stagesrealtime.repository.networking.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class DeleteStageRequest(
    val hostId: String
)
