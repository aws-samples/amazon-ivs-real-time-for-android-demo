package com.amazon.ivs.stagesrealtimecompose.core.handlers.networking

import com.amazon.ivs.stagesrealtimecompose.BuildConfig
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.json
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.launchDefault
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.launchMain
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.chat.ChatHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageManager
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageType
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val REQUEST_TIMEOUT = 30L

object NetworkHandler {
    private fun getClient(apiKey: String): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true }
            .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val updatedRequest = chain
                    .request()
                    .newBuilder()
                    .addHeader("x-api-key", apiKey)
                    .build()
                chain.proceed(updatedRequest)
            }
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(interceptor)
        }
        return builder.build()
    }

    private var api = getApi(
        customerCode = UserHandler.currentSession?.customerCode ?: "xxx",
        apiKey = UserHandler.currentSession?.apiKey ?: ""
    )

    init {
        launchDefault {
            UserHandler.session.collect { session ->
                if (session == null) return@collect
                api = getApi(
                    customerCode = session.customerCode,
                    apiKey = session.apiKey
                )
            }
        }
    }

    suspend fun verifyConnection() = try {
        val session = UserHandler.currentSession ?: throw Exception("No session found")
        Timber.d("Verifying the connection with the API for: $session")
        api = getApi(
            customerCode = session.customerCode,
            apiKey = session.apiKey
        )
        val response = api.verifyConnectionCode()
        if (response.isSuccessful) {
            Timber.d("Code is valid")
            Success()
        } else {
            val error = response.errorBody()?.string()
            throw Exception("Failed to verify connection: ${response.code()}, $error")
        }
    } catch (e: Exception) {
        Timber.w(e, "Failed to verify connection")
        delay(1000)
        Failure(Error.CustomerCodeError)
    }

    suspend fun getStages() = try {
        val response = api.getStages()
        Timber.d("Stages received: $response")
        Success(response)
    } catch (e: Exception) {
        Timber.w(e, "Failed to get stages")
        Failure(Error.GetStagesError)
    }

    suspend fun createStage(type: StageType) = try {
        val user = UserHandler.currentUser
        val session = UserHandler.currentSession!!
        val response = api.createStage(
            body = CreateStageRequest(
                hostId = user.username,
                hostAttributes = UserAttributes(
                    avatarColRight = user.userAvatar.colorRight,
                    avatarColLeft = user.userAvatar.colorLeft,
                    avatarColBottom = user.userAvatar.colorBottom,
                    username = user.username
                ),
                type = type,
                cid = session.customerCode
            )
        )

        Timber.d("Stage created: $response for $user")
        launchDefault {
            joinChat(
                stageId = user.username,
                region = response.region
            )
        }
        launchMain {
            StageManager.joinStage(response.hostParticipantToken.token)
        }
        Success(response)
    } catch (e: Exception) {
        Timber.w(e, "Failed to create stage")
        Failure(Error.CreateStageError)
    }

    suspend fun joinStage(stageId: String) = try {
        val user = UserHandler.currentUser
        val response = api.joinStage(
            body = JoinStageRequest(
                hostId = stageId,
                userId = user.username,
                attributes = UserAttributes(
                    avatarColRight = user.userAvatar.colorRight,
                    avatarColLeft = user.userAvatar.colorLeft,
                    avatarColBottom = user.userAvatar.colorBottom,
                    username = user.username
                )
            )
        )
        Timber.d("Stage joined: $response for $user")
        launchDefault {
            joinChat(
                stageId = response.hostAttributes.username,
                region = response.region
            )
        }
        launchMain {
            StageManager.joinStage(response.token)
        }
        Success(response)
    } catch (e: Exception) {
        Timber.w(e, "Failed to join stage")
        Failure(Error.JoinStageError)
    }

    suspend fun deleteStage() = try {
        val user = UserHandler.currentUser
        api.deleteStage(
            body = DeleteStageRequest(
                hostId = user.username
            )
        )
        Success()
    } catch (e: Exception) {
        Timber.w(e, "Failed to delete stage")
        Failure(Error.DeleteStageError)
    }

    suspend fun leaveChat(stageId: String) = try {
        val user = UserHandler.currentUser
        val participantId = StageManager.getParticipantId(user.username) ?: user.username
        api.leaveChat(
            body = LeaveChatRequest(
                hostId = stageId,
                userId = user.username,
                participantId = participantId
            )
        )
        Success()
    } catch (e: Exception) {
        Timber.w(e, "Failed to leave chat")
        Failure(Error.LeaveStageError)
    }

    suspend fun kickParticipant() = try {
        val user = UserHandler.currentUser
        api.updateStageMode(
            body = UpdateStageModeRequest(
                hostId = user.username,
                userId = user.username,
                mode = StageModeLegacy.NONE
            )
        )
        Success()
    } catch (e: Exception) {
        Timber.w(e, "Failed to kick participant")
        Failure(Error.KickParticipantError)
    }

    suspend fun updateSeats(stageId: String, seats: List<String>) = try {
        val user = UserHandler.currentUser
        api.updateSeats(
            body = UpdateSeatsRequest(
                hostId = stageId,
                userId = user.username,
                seats = seats
            )
        )
        Success()
    } catch (e: Exception) {
        Timber.w(e, "Failed to update seats")
        Failure(Error.UpdateSeatsError)
    }

    suspend fun castVote(stageId: String, userName: String) = try {
        val voteBody = VoteRequest(
            hostId = stageId,
            userIdVote = userName
        )
        Timber.d("Casting vote for $voteBody")
        api.castVote(voteBody)
        Success()
    } catch (e: Exception) {
        Timber.w(e, "Failed to cast vote")
        Failure(Error.CastVoteError)
    }

    suspend fun updateMode(stageId: String, mode: StageModeLegacy) = try {
        val user = UserHandler.currentUser
        api.updateStageMode(
            body = UpdateStageModeRequest(
                hostId = stageId,
                userId = user.username,
                mode = mode
            )
        )
        Success()
    } catch (e: Exception) {
        Timber.w(e, "Failed to update stage mode")
        Failure(Error.UpdateModeError)
    }

    private suspend fun joinChat(stageId: String, region: String) = try {
        val user = UserHandler.currentUser
        val response = api.joinChat(
            body = JoinChatRequest(
                hostId = stageId,
                userId = user.username,
                attributes = UserAttributes(
                    avatarColLeft = user.userAvatar.colorLeft,
                    avatarColRight = user.userAvatar.colorRight,
                    avatarColBottom = user.userAvatar.colorBottom,
                    username = user.username
                )
            )
        )
        ChatHandler.joinRoom(
            chatToken = response.asChatToken(),
            region = region,
            stageId = stageId
        )
        Timber.d("Chat room joined: $stageId, $region, $user")
    } catch (e: Exception) {
        Timber.w(e, "Failed to join chat: $stageId")
    }

    private fun getApi(customerCode: String, apiKey: String) = Retrofit.Builder()
        .client(getClient(apiKey))
        .baseUrl("https://$customerCode.cloudfront.net/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(Api::class.java)
}
