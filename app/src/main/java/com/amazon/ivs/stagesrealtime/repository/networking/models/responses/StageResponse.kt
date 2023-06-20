package com.amazon.ivs.stagesrealtime.repository.networking.models.responses

import com.amazon.ivs.stagesrealtime.repository.networking.models.StageMode
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageStatus
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageType
import kotlinx.serialization.Serializable

@Serializable
data class GetStagesResponse(
    val stages: List<StageResponse>
)

@Serializable
data class StageResponse(
    val hostAttributes: StageHostAttributes,
    val hostId: String,
    val mode: StageMode,
    val status: StageStatus,
    val createdAt: String,
    val type: StageType,
    val stageArn: String,
    val seats: List<String>? = null
)

@Serializable
data class StageHostAttributes(
    val avatarColLeft: String,
    val avatarColRight: String,
    val avatarColBottom: String,
    val username: String
)
