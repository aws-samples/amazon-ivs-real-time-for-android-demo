package com.amazon.ivs.stagesrealtime.repository.stage.usecases

import androidx.datastore.core.DataStore
import com.amazon.ivs.stagesrealtime.common.Failure
import com.amazon.ivs.stagesrealtime.common.Success
import com.amazon.ivs.stagesrealtime.common.VOTE_SESSION_TIME_SECONDS
import com.amazon.ivs.stagesrealtime.common.extensions.asPKModeScore
import com.amazon.ivs.stagesrealtime.common.extensions.getElapsedTimeFromNow
import com.amazon.ivs.stagesrealtime.common.extensions.getStageId
import com.amazon.ivs.stagesrealtime.common.extensions.getUserAvatar
import com.amazon.ivs.stagesrealtime.common.extensions.runCancellableCatching
import com.amazon.ivs.stagesrealtime.repository.models.AppSettings
import com.amazon.ivs.stagesrealtime.repository.models.PKModeScore
import com.amazon.ivs.stagesrealtime.repository.models.PKModeSessionTime
import com.amazon.ivs.stagesrealtime.repository.networking.NetworkClient
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageType
import com.amazon.ivs.stagesrealtime.repository.networking.models.UserAttributes
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.Error
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.JoinStageRequest
import com.amazon.ivs.stagesrealtime.ui.stage.models.StageUIModel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class JoinStageResponse(
    val stageId: String,
    val token: String,
    val region: String,
    val participantId: String,
    val stageType: StageType,
    val pkModeScore: PKModeScore?,
    val pkModeSessionTime: PKModeSessionTime?
)

@Singleton
class JoinStageUseCase @Inject constructor(
    private val networkClient: NetworkClient,
    private val appSettingsStore: DataStore<AppSettings>
) {
    suspend operator fun invoke(stage: StageUIModel) = runCancellableCatching(
        tryBlock = {
            // Make a join stage request to backend
            val api = networkClient.getOrCreateApi()
            val userAvatar = appSettingsStore.getUserAvatar()
            val stageId = appSettingsStore.getStageId()
            val request = JoinStageRequest(
                hostId = stage.stageId,
                userId = stageId,
                attributes = UserAttributes(
                    avatarColLeft = userAvatar.colorLeft,
                    avatarColRight = userAvatar.colorRight,
                    avatarColBottom = userAvatar.colorBottom,
                    username = stageId
                )
            )
            val response = api.joinStage(request)
            val participantId = response.participantId
            val token = response.token
            val region = response.region
            val stageType = if (stage.isAudioMode) StageType.AUDIO else StageType.VIDEO
            var pkModeScore: PKModeScore? = null
            var pkModeSessionTime: PKModeSessionTime? = null
            response.metadata.activeVotingSession?.let { votingSession ->
                val secondsRemaining =
                    VOTE_SESSION_TIME_SECONDS - votingSession.startedAt.getElapsedTimeFromNow()
                pkModeScore = votingSession.tally.asPKModeScore(
                    hostId = stage.stageId,
                    shouldResetScore = true
                )
                pkModeSessionTime = PKModeSessionTime(secondsRemaining = secondsRemaining)
            }

            Success(JoinStageResponse(
                stageId = stage.stageId,
                token = token,
                region = region,
                participantId = participantId,
                stageType = stageType,
                pkModeScore = pkModeScore,
                pkModeSessionTime = pkModeSessionTime,
            ))
        }, errorBlock = { e ->
            Timber.e(e, "Failed to join stage")
            Failure(Error.JoinStageError)
        }
    )
}
