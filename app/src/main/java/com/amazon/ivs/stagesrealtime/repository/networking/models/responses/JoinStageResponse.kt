package com.amazon.ivs.stagesrealtime.repository.networking.models.responses

import com.amazon.ivs.stagesrealtime.repository.networking.models.UserAttributes
import kotlinx.serialization.Serializable

@Serializable
data class JoinStageResponse(
    val hostAttributes: UserAttributes,
    val token: String,
    val participantId: String,
    val duration: Long,
    val region: String,
    val metadata: Metadata
)

@Serializable
data class Metadata(
    val activeVotingSession: VotingSession? = null
)

@Serializable
data class VotingSession(
    val startedAt: String,
    val tally: Map<String, String>
)
