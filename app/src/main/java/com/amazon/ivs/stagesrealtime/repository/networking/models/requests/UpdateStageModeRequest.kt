package com.amazon.ivs.stagesrealtime.repository.networking.models.requests

import com.amazon.ivs.stagesrealtime.repository.networking.models.StageMode
import kotlinx.serialization.Serializable

@Serializable
data class UpdateStageModeRequest(
    val hostId: String,
    val userId: String,
    val mode: StageMode
)
