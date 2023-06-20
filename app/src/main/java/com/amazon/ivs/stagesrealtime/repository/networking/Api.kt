package com.amazon.ivs.stagesrealtime.repository.networking

import com.amazon.ivs.stagesrealtime.repository.networking.models.StageStatus
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.*
import com.amazon.ivs.stagesrealtime.repository.networking.models.responses.*
import retrofit2.Response
import retrofit2.http.*

interface Api {

    @POST("create")
    suspend fun createStage(@Body body: CreateStageRequest): CreateStageResponse

    @POST("join")
    suspend fun joinStage(@Body body: JoinStageRequest): JoinStageResponse

    @POST("chatToken/create")
    suspend fun createChat(@Body body: CreateChatRequest): ChatTokenResponse

    @POST("castVote")
    suspend fun castVote(@Body body: VoteRequest): Response<Unit>

    @PUT("disconnect")
    suspend fun disconnectUser(@Body body: DisconnectUserRequest)

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
