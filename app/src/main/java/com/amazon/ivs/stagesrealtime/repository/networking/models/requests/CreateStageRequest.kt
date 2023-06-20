package com.amazon.ivs.stagesrealtime.repository.networking.models.requests

import com.amazon.ivs.stagesrealtime.repository.networking.models.StageType
import com.amazon.ivs.stagesrealtime.repository.networking.models.UserAttributes
import kotlinx.serialization.Serializable

@Serializable
data class CreateStageRequest(
    val hostId: String,
    val hostAttributes: UserAttributes,
    val type: StageType,
    val cid: String
)
