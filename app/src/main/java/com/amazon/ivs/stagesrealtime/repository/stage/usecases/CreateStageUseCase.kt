package com.amazon.ivs.stagesrealtime.repository.stage.usecases

import androidx.datastore.core.DataStore
import com.amazon.ivs.stagesrealtime.common.Failure
import com.amazon.ivs.stagesrealtime.common.Response
import com.amazon.ivs.stagesrealtime.common.Success
import com.amazon.ivs.stagesrealtime.common.emptySeats
import com.amazon.ivs.stagesrealtime.common.extensions.getCustomerCode
import com.amazon.ivs.stagesrealtime.common.extensions.getStageId
import com.amazon.ivs.stagesrealtime.common.extensions.getUserAvatar
import com.amazon.ivs.stagesrealtime.common.extensions.runCancellableCatching
import com.amazon.ivs.stagesrealtime.repository.models.AppSettings
import com.amazon.ivs.stagesrealtime.repository.networking.NetworkClient
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageType
import com.amazon.ivs.stagesrealtime.repository.networking.models.UserAttributes
import com.amazon.ivs.stagesrealtime.repository.networking.models.getIDs
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.CreateStageRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.Error
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.UpdateSeatsRequest
import com.amazon.ivs.stagesrealtime.ui.stage.models.AudioSeatUIModel
import com.amazon.ivs.stagesrealtime.ui.stage.models.StageUIModel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class CreateStageResponse(
    val token: String,
    val region: String,
    val stageId: String,
    val hostParticipantId: String,
    val type: StageType,
    val stageUIModel: StageUIModel
)

interface CreateStageUseCase {
    suspend fun createStage(type: StageType): Response<Error.CreateStageError, CreateStageResponse>
}

@Singleton
class CreateStageUseCaseImpl  @Inject constructor(
    private val networkClient: NetworkClient,
    private val appSettingsStore: DataStore<AppSettings>,
) : CreateStageUseCase {
    private val api get() = networkClient.getOrCreateApi()

    override suspend fun createStage(type: StageType) = runCancellableCatching(
        tryBlock = {
            // Make backend request for creating a stage instance in DB
            val userAvatar = appSettingsStore.getUserAvatar()
            val stageId = appSettingsStore.getStageId()
            val customerCode = appSettingsStore.getCustomerCode()
            val request = CreateStageRequest(
                hostId = stageId,
                hostAttributes = UserAttributes(
                    avatarColBottom = userAvatar.colorBottom,
                    avatarColLeft = userAvatar.colorLeft,
                    avatarColRight = userAvatar.colorRight,
                    username = stageId
                ),
                type = type,
                cid = customerCode!!
            )
            val response = api.createStage(request)
            val token = response.hostParticipantToken.token
            val hostParticipantId = response.hostParticipantToken.participantId
            val region = response.region

            val isAudioMode = type == StageType.AUDIO
            Timber.d("Stage created: $response for $stageId, $userAvatar, $type, $isAudioMode")
            val seats = mutableListOf<AudioSeatUIModel>()
            if (isAudioMode) {
                seats.addAll(emptySeats)
                seats.removeAt(0)
                seats.add(
                    0, AudioSeatUIModel(
                        id = 0,
                        participantId = hostParticipantId,
                        userAvatar = userAvatar
                    )
                )
                api.updateSeats(UpdateSeatsRequest(hostId = stageId, userId = stageId, seats = seats.getIDs()))
            }
            val stageUIModel = StageUIModel(
                stageId = stageId,
                creatorAvatar = userAvatar,
                selfAvatar = userAvatar,
                isCreator = true,
                isAudioMode = isAudioMode,
                seats = seats
            )
            Success(CreateStageResponse(
                token = token,
                region = region,
                stageId = stageId,
                hostParticipantId = hostParticipantId,
                type = type,
                stageUIModel = stageUIModel
            ))
        }, errorBlock = { e ->
            Timber.e(e, "Failed to create stage")
            Failure(Error.CreateStageError)
        }
    )
}
