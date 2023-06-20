package com.amazon.ivs.stagesrealtime.repository.networking.models.requests

import com.amazon.ivs.stagesrealtime.repository.networking.models.UserAttributes
import kotlinx.serialization.Serializable

@Serializable
data class JoinStageRequest(
    val hostId: String,
    val userId: String,
    val attributes: UserAttributes
)
