package com.amazon.ivs.stagesrealtimecompose.core.handlers.networking

import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface Api {
    @POST("create")
    suspend fun createStage(@Body body: CreateStageRequest): CreateStageResponse

    @POST("join")
    suspend fun joinStage(@Body body: JoinStageRequest): JoinStageResponse

    @POST("chatToken/create")
    suspend fun joinChat(@Body body: JoinChatRequest): ChatTokenResponse

    @PUT("disconnect")
    suspend fun leaveChat(@Body body: LeaveChatRequest)

    @POST("castVote")
    suspend fun castVote(@Body body: VoteRequest): Response<Unit>

    @PUT("update/mode")
    suspend fun updateStageMode(@Body body: UpdateStageModeRequest)

    @PUT("update/seats")
    suspend fun updateSeats(@Body body: UpdateSeatsRequest)

    @HTTP(method = "DELETE", path = "/", hasBody = true)
    suspend fun deleteStage(@Body body: DeleteStageRequest): Response<Unit>

    @GET(".")
    suspend fun getStages(@Query("status") stageStatus: StageStatus = StageStatus.ACTIVE): GetStagesResponse

    @GET("verify")
    suspend fun verifyConnectionCode(): Response<Unit>
}

@Serializable
data class JoinChatRequest(
    val hostId: String,
    val userId: String,
    val attributes: UserAttributes
)

@Serializable
data class ChatTokenResponse(
    val token: String,
    val sessionExpirationTime: String,
    val tokenExpirationTime: String
)

@Serializable
data class UserAttributes(
    val avatarColBottom: String,
    val avatarColLeft: String,
    val avatarColRight: String,
    val username: String,
)

@Serializable
data class CreateStageRequest(
    val hostId: String,
    val hostAttributes: UserAttributes,
    val type: StageType,
    val cid: String
)

@Serializable
data class CreateStageResponse(
    val region: String,
    val hostParticipantToken: ParticipantToken
)

@Serializable
data class ParticipantToken(
    val token: String,
    val participantId: String,
    val duration: Int
)

@Serializable
data class JoinStageRequest(
    val hostId: String,
    val userId: String,
    val attributes: UserAttributes
)

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

@Serializable
data class DeleteStageRequest(
    val hostId: String
)

@Serializable
data class LeaveChatRequest(
    val hostId: String,
    val userId: String,
    val participantId: String
)

@Serializable
data class UpdateSeatsRequest(
    val hostId: String,
    val userId: String,
    val seats: List<String>
)

@Serializable
data class UpdateStageModeRequest(
    val hostId: String,
    val userId: String,
    val mode: StageModeLegacy
)

@Serializable
data class VoteRequest(
    val hostId: String,
    @SerialName("vote")
    val userIdVote: String
)

@Serializable
data class GetStagesResponse(
    val stages: List<StageResponse>
)

@Serializable
data class StageResponse(
    val hostAttributes: StageHostAttributes,
    val hostId: String,
    val mode: StageModeLegacy,
    val status: StageStatus,
    val createdAt: String,
    val type: StageTypeLegacy,
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

enum class StageModeLegacy {
    NONE, PK, GUEST_SPOT, AUDIO
}

enum class StageTypeLegacy {
    AUDIO, VIDEO
}

enum class StageStatus {
    ACTIVE, IDLE
}
