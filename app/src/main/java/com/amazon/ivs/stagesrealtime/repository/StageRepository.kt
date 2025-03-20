package com.amazon.ivs.stagesrealtime.repository

import android.content.Context
import androidx.datastore.core.DataStore
import com.amazon.ivs.stagesrealtime.common.Failure
import com.amazon.ivs.stagesrealtime.common.Ok
import com.amazon.ivs.stagesrealtime.common.Response
import com.amazon.ivs.stagesrealtime.common.Success
import com.amazon.ivs.stagesrealtime.common.binding
import com.amazon.ivs.stagesrealtime.common.extensions.getByIndexOrFirst
import com.amazon.ivs.stagesrealtime.common.extensions.getByIndexOrLast
import com.amazon.ivs.stagesrealtime.common.extensions.getStageId
import com.amazon.ivs.stagesrealtime.common.extensions.getUserAvatar
import com.amazon.ivs.stagesrealtime.common.extensions.isVideoStatsEnabled
import com.amazon.ivs.stagesrealtime.common.extensions.launchMain
import com.amazon.ivs.stagesrealtime.common.extensions.runCancellableCatching
import com.amazon.ivs.stagesrealtime.di.IOScope
import com.amazon.ivs.stagesrealtime.repository.chat.ChatManager
import com.amazon.ivs.stagesrealtime.repository.models.AppSettings
import com.amazon.ivs.stagesrealtime.repository.models.PKModeScore
import com.amazon.ivs.stagesrealtime.repository.models.PKModeSessionTime
import com.amazon.ivs.stagesrealtime.repository.models.ParticipantAttributes
import com.amazon.ivs.stagesrealtime.repository.models.RTCData
import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar
import com.amazon.ivs.stagesrealtime.repository.networking.NetworkClient
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageMode
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageType
import com.amazon.ivs.stagesrealtime.repository.networking.models.UserAttributes
import com.amazon.ivs.stagesrealtime.repository.networking.models.asChatToken
import com.amazon.ivs.stagesrealtime.repository.networking.models.getIDs
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.CreateChatRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.DeleteStageRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.DisconnectUserRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.Error
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.UpdateSeatsRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.UpdateStageModeRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.VoteRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.update
import com.amazon.ivs.stagesrealtime.repository.networking.models.updateOrAdd
import com.amazon.ivs.stagesrealtime.repository.networking.models.updateSeats
import com.amazon.ivs.stagesrealtime.repository.stage.StageEvent
import com.amazon.ivs.stagesrealtime.repository.stage.StageManager
import com.amazon.ivs.stagesrealtime.repository.stage.usecases.CreateStageUseCase
import com.amazon.ivs.stagesrealtime.repository.stage.usecases.JoinStageUseCase
import com.amazon.ivs.stagesrealtime.ui.stage.models.ScrollDirection
import com.amazon.ivs.stagesrealtime.ui.stage.models.StageListModel
import com.amazon.ivs.stagesrealtime.ui.stage.models.StageUIModel
import com.amazonaws.ivs.chat.messaging.requests.SendMessageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StageRepository @Inject constructor(
    @IOScope private val ioScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    private val networkClient: NetworkClient,
    private val chatManager: ChatManager,
    private val appSettingsStore: DataStore<AppSettings>,
    private val createStageUseCase: CreateStageUseCase,
    private val joinStageUseCase: JoinStageUseCase,
) {
    private val availableStages = mutableListOf<StageUIModel>()
    private val stageJobs = mutableListOf<Job>()

    private val _stages = MutableStateFlow(StageListModel())
    private val _stageRTCData = MutableStateFlow(RTCData())
    private val _stageRTCDataList = MutableStateFlow(emptyList<RTCData>())
    private val onActiveSessionScore = Channel<PKModeScore>()
    private val onActiveSessionTime = Channel<PKModeSessionTime>()
    private var stageManager = StageManager(context, appSettingsStore, ioScope)

    // Current stage variables
    private var currentPosition = 0
    private var scrollDirection = ScrollDirection.NONE
    private var currentToken: String? = null
    private var currentParticipantId: String? = null
    private var currentStageType: StageType? = null
    private var currentJoinJob: Job? = null
    private var isLocalUserSpeaking = false

    // Those 2 are different since stage can be scrolled to new position, but not joined
    private var currentlyJoinedStageId: String? = null
    private val currentStageIdByPosition get() = availableStages.getOrNull(currentPosition)?.stageId

    // Public observable flows used by UI
    val messages = chatManager.messages
    val onStageLike = chatManager.onStageLike
    val onPKModeScore = merge(chatManager.onPKModeScore, onActiveSessionScore.receiveAsFlow())
    val onVoteStart = merge(chatManager.onVoteStart, onActiveSessionTime.receiveAsFlow())
    val stageRTCData = _stageRTCData.asStateFlow()
    val stageRTCDataList = _stageRTCDataList.asStateFlow()
    val stages by lazy {
        createOnSeatsUpdatedJob()
        createOnModeChangedJob()
        _stages.asStateFlow()
    }

    suspend fun scrollStages(direction: ScrollDirection) {
        val stageCount = availableStages.size
        scrollDirection = direction
        val lastPosition = currentPosition
        val newPosition = when (direction) {
            ScrollDirection.UP -> if (currentPosition - 1 >= 0) currentPosition - 1 else stageCount - 1
            ScrollDirection.DOWN -> if (currentPosition + 1 < stageCount) currentPosition + 1 else 0
            ScrollDirection.NONE -> {
                currentPosition
                return
            }
        }
        // Handle old position
        val positionChanged = lastPosition != newPosition
        Timber.d("Feed scrolled: $direction to index: $newPosition, will refresh: $positionChanged, $currentlyJoinedStageId")
        if (positionChanged) {
            currentJoinJob?.cancel()
            currentlyJoinedStageId = null
            availableStages.update(
                index = lastPosition,
                keepCreatorVideo = false,
                keepGuestVideo = false
            )
            availableStages.update(
                index = newPosition,
                keepCreatorVideo = false,
                keepGuestVideo = false
            )
            updateStages()
            disconnectFromCurrentStage()
        }
        // Handle new position
        Timber.d("Stage position scrolled from: $currentPosition to: $newPosition, $currentlyJoinedStageId")
        currentPosition = newPosition
        updateStages()
        val currentStage = availableStages.getOrNull(currentPosition)
        if (positionChanged && currentStage != null) {
            joinStage(currentStage)
        } else {
            updateStages()
        }
    }

    suspend fun createStage(type: StageType): Response<Error.CreateStageError, Ok> {
        val response = createStageUseCase(type)
        return binding {
            val joinedStage = response.bind()
            currentToken = joinedStage.token
            currentParticipantId = joinedStage.hostParticipantId
            currentStageType = joinedStage.type
            availableStages.add(joinedStage.stageUIModel)

            // If stage entry was added in backend - create and observe the Stage instance
            createAndObserveNewStageJobs()
            stageManager.joinStage(
                stageId = joinedStage.stageId,
                hostParticipantId = joinedStage.hostParticipantId,
                selfParticipantId = joinedStage.hostParticipantId,
                token = joinedStage.token,
                type = joinedStage.type,
                isCreator = true
            )
            stageManager.observeStage()
            updateStages()
            createChat(joinedStage.stageId, joinedStage.stageId, joinedStage.region)
            Success()
        }
    }

    suspend fun onSeatClicked(index: Int)= runCancellableCatching(
        tryBlock = {
            val stage = availableStages.getOrNull(currentPosition)
                ?: return@runCancellableCatching Failure(Error.UpdateSeatsError)
            val currentIndex = stage.seats.indexOfFirst { it.participantId == currentParticipantId }
            val joinStage = currentIndex != index
            var seats = stage.seats.map { seat ->
                if (seat.participantId == currentParticipantId) {
                    seat.copy(participantId = "", userAvatar = null, isMuted = false)
                } else {
                    seat.copy()
                }
            }
            seats = seats.mapIndexed { seatIndex, seat ->
                if (seatIndex == index) {
                    val id = currentParticipantId ?: ""
                    seat.copy(
                        participantId = id,
                        userAvatar = appSettingsStore.getUserAvatar(),
                        isMuted = stageManager.isLocalAudioOff()
                    )
                } else {
                    seat.copy()
                }
            }
            Timber.d("Updating stages on click: $joinStage, ${seats.getIDs()}")
            availableStages.update(currentPosition, _seats = seats)
            updateStages()
            if (joinStage) {
                startPublishing(StageMode.NONE, updateMode = false)
            } else {
                stopPublishing()
            }
            networkClient.getOrCreateApi().updateSeats(UpdateSeatsRequest(
                hostId = stage.stageId,
                userId = appSettingsStore.getStageId(),
                seats = seats.getIDs()
            ))
            Success()
        }, errorBlock = { e ->
            Timber.e(e, "Failed to update seats")
            Failure(Error.UpdateSeatsError)
        }
    )

    suspend fun getStages() = runCancellableCatching(
        tryBlock = {
            val response = networkClient.getOrCreateApi().getStages()
            Timber.d("Stages received: ${availableStages.count()}, $response, $currentPosition, $currentlyJoinedStageId")
            @Suppress("UNNECESSARY_SAFE_CALL")
            // There could be a race condition where stage.stageId can throw a null pointer so we need the
            // "unnecessary" safe check when filtering
            val staleStages = availableStages.toList().filter { stage ->
                response.stages.none { it.hostId == stage?.stageId }
            }
            Timber.d("Stale stages: $staleStages")
            staleStages.forEach { staleStage ->
                availableStages.remove(staleStage)
                if (staleStage.stageId == currentlyJoinedStageId) {
                    currentlyJoinedStageId = null
                }
            }
            if (currentPosition >= availableStages.count()) {
                Timber.d("Stage at position does not exist - resetting: $currentPosition to: 0")
                currentPosition = 0
            }

            val indexOfCurrentStage = availableStages.indexOfFirst {
                it.stageId == currentlyJoinedStageId
            }
            if (indexOfCurrentStage != -1 && indexOfCurrentStage != currentPosition) {
                Timber.d("Stage index does not match - updating: $currentPosition to: $indexOfCurrentStage")
                currentPosition = indexOfCurrentStage
            }

            availableStages.updateOrAdd(appSettingsStore.getStageId(), response.stages, appSettingsStore.getUserAvatar())
            availableStages.updateSeats(
                currentPosition,
                stageManager,
                ::getParticipantAttributes
            )
            val currentStage = availableStages.getOrNull(currentPosition)
            if (currentStage != null && currentStage.stageId != currentlyJoinedStageId) {
                joinStage(currentStage)
            } else {
                updateStages()
            }
            Success()
        }, errorBlock = { e ->
            Timber.e(e, "Failed to get stages")
            Failure(Error.GetStagesError)
        }
    )

    suspend fun verifyConnectionCode() = runCancellableCatching(
        tryBlock = {
            Timber.d("Verifying the connection with the API")
            val response = networkClient.getOrCreateApi().verifyConnectionCode()
            if (response.isSuccessful) {
                Success()
            } else {
                Failure(Error.CustomerCodeError)
            }
        }, errorBlock = { e ->
            Timber.e(e, "Failed to connect to backend")
            Failure(Error.CustomerCodeError)
        }
    )

    suspend fun startPublishing(mode: StageMode, updateMode: Boolean = true) = runCancellableCatching(
        tryBlock = {
            Timber.d("Start publishing: $mode, $currentStageIdByPosition, $currentStageType")
            val type = currentStageType ?: return@runCancellableCatching Failure(Error.JoinStageError)
            val stageIdCurrent = currentStageIdByPosition ?: return@runCancellableCatching Failure(Error.JoinStageError)
            if (updateMode) {
                networkClient.getOrCreateApi().updateStageMode(UpdateStageModeRequest(
                    hostId = stageIdCurrent,
                    userId = appSettingsStore.getStageId(),
                    mode = mode
                ))
            }
            stageManager.startPublishing(type, mode)
            availableStages.update(
                index = currentPosition,
                _isPKMode = mode == StageMode.PK,
                _isGuestMode = mode == StageMode.GUEST_SPOT
            )
            Success()
        }, errorBlock = { e ->
            Timber.d(e, "Failed to start publishing on position $currentPosition")
            Failure(Error.JoinStageError)
        }
    )

    suspend fun stopPublishing() = runCancellableCatching(
        tryBlock = {
            Timber.d("Leave stage: $currentParticipantId, $currentStageIdByPosition")
            val api = networkClient.getOrCreateApi()
            val stageIdCurrent = currentStageIdByPosition ?: return@runCancellableCatching Failure(Error.LeaveStageError)
            if (currentStageType == StageType.AUDIO) {
                val stage = availableStages.getOrNull(currentPosition)
                    ?: return@runCancellableCatching Failure(Error.LeaveStageError)
                val seats = stage.seats.map { seat ->
                    if (seat.participantId == currentParticipantId) {
                        seat.copy(participantId = "", userAvatar = null, isMuted = false)
                    } else {
                        seat.copy()
                    }
                }
                Timber.d("Leaving audio room: ${seats.getIDs()}")
                availableStages.update(currentPosition, _seats = seats)
                updateStages()
                api.updateSeats(UpdateSeatsRequest(
                    hostId = stage.stageId,
                    userId = appSettingsStore.getStageId(),
                    seats = seats.getIDs()
                ))
            } else {
                api.updateStageMode(
                    UpdateStageModeRequest(
                        hostId = stageIdCurrent,
                        userId = appSettingsStore.getStageId(),
                        mode = StageMode.NONE
                    )
                )
            }
            stageManager.stopPublishing()
            Success()
        }, errorBlock = { e ->
            Timber.e(e, "Failed to stop publishing on position: $currentPosition")
            Failure(Error.LeaveStageError)
        }
    )

    fun clearResources() {
        Timber.d("Clearing resources: $currentPosition")
        stageJobs.forEach { it.cancel() }
        stageJobs.clear()
        chatManager.clearPreviousChat()
        currentJoinJob?.cancel()
        currentJoinJob = null
        currentlyJoinedStageId = null
        currentPosition = 0
        currentStageType = null
        currentToken = null
        currentParticipantId = null
        availableStages.clear()
        destroyApi()
        Timber.d("Cleared resources: $currentPosition")
    }

    fun destroyApi() {
        networkClient.destroyApi()
    }

    suspend fun disconnectFromCurrentStage() = runCancellableCatching(
        tryBlock = {
            val stageId = appSettingsStore.getStageId()
            stageManager.leaveStage()
            val participantId = currentParticipantId ?: return@runCancellableCatching Failure(Unit)
            val hostId = currentStageIdByPosition ?: return@runCancellableCatching Failure(Unit)
            Timber.d("Disconnecting from $hostId, $stageId, $participantId")
            networkClient.getOrCreateApi().disconnectUser(DisconnectUserRequest(hostId, stageId, participantId))
            Timber.d("Disconnected from: $hostId, $stageId, $participantId")
            updateStages()
            Success()
        }, errorBlock = { e ->
            Timber.e(e, "Failed to disconnect from current stage")
            Failure(Unit)
        }
    )

    suspend fun kickParticipant() = runCancellableCatching(
        tryBlock = {
            Timber.d("Kicking participant and updating stage to NONE")
            val stageId = appSettingsStore.getStageId()
            networkClient.getOrCreateApi().updateStageMode(
                body = UpdateStageModeRequest(
                    hostId = stageId,
                    userId = stageId,
                    mode = StageMode.NONE
                )
            )
            Success()
        }, errorBlock = { e ->
            Timber.e(e, "Failed to kick participant")
            Failure(Error.KickParticipantError)
        }
    )

    suspend fun deleteStage() = runCancellableCatching(
        tryBlock = {
            networkClient.getOrCreateApi().deleteStage(DeleteStageRequest(appSettingsStore.getStageId()))
            Timber.d("Stage deleted")
            availableStages.clear()
            stageManager.leaveStage()
            clearResources()
            updateStages()
            Success()
        }, errorBlock = { e ->
            Timber.e(e, "Failed to delete stage")
            Failure(Error.DeleteStageError)
        }
    )

    suspend fun castVote(voteHost: Boolean) = runCancellableCatching(
        tryBlock = {
            val hostId = currentStageIdByPosition ?: return@runCancellableCatching Failure(Error.CastVoteError)
            val voteBody = VoteRequest(
                hostId,
                if (voteHost) hostId
                else if (stageManager.isParticipating() && !stageManager.isStageCreator()) appSettingsStore.getStageId()
                else stageManager.getGuestId()!!
            )
            Timber.d("Casting vote for $voteBody")
            networkClient.getOrCreateApi().castVote(voteBody)
            Success()
        }, errorBlock = { e ->
            Timber.d(e, "Failed to cast vote")
            Failure(Error.CastVoteError)
        }
    )

    fun sendMessage(message: String) {
        chatManager.sendMessage(SendMessageRequest(message))
    }

    fun likeStage() {
        chatManager.likeStage()
    }

    fun isCurrentStageVideo() = stageManager.isCurrentStageVideo()

    fun isStageCreator() = stageManager.isStageCreator()

    fun isParticipating() = stageManager.isParticipating()

    fun switchAudio() {
        if (isParticipating()) stageManager.switchAudio()
    }

    fun switchVideo() {
        if (isParticipating()) stageManager.switchVideo()
    }

    suspend fun switchFacing() {
        if (isParticipating()) stageManager.switchFacing()
    }

    fun canScroll() = availableStages.size > 1 && !isParticipating()

    fun requestRTCStats() = stageManager.requestRTCStats()

    suspend fun getPKModeWinnerAvatar(hostWin: Boolean): UserAvatar {
        availableStages.getOrNull(currentPosition)?.let { stage ->
            val isCreator = stage.isCreator
            val isParticipant = stage.isParticipant
            val hostId = stage.stageId
            val guestId = stageManager.getGuestId()
            Timber.d("Get PK Mode winner: $hostId, $guestId")
            return when (hostWin) {
                true -> if (isCreator) appSettingsStore.getUserAvatar() else stageManager.getParticipantAvatar(hostId)
                else -> if (isParticipant) appSettingsStore.getUserAvatar() else stageManager.getParticipantAvatar(guestId)
            } ?: UserAvatar()
        } ?: return UserAvatar()
    }

    private suspend fun getParticipantAttributes(participantId: String): ParticipantAttributes? {
        availableStages.getOrNull(currentPosition)?.let { stage ->
            return if (currentParticipantId == participantId) {
                ParticipantAttributes(
                    stageId = stage.stageId,
                    participantId = participantId,
                    userAvatar = appSettingsStore.getUserAvatar(),
                    isMuted = stageManager.isLocalAudioOff(),
                    isSpeaking = isLocalUserSpeaking && !stageManager.isLocalAudioOff(),
                    isHost = isStageCreator()
                )
            } else {
                stageManager.getParticipantAttributes(participantId)
            }
        }
        return null
    }

    private fun joinStage(stage: StageUIModel) {
        currentJoinJob?.cancel()
        if (isStageCreator() || stage.stageId == currentlyJoinedStageId) return
        currentJoinJob = launchMain {
            Timber.d("Attempting join stage - $stage; $currentlyJoinedStageId")
            val response = joinStageUseCase(stage)
            response.onSuccess { joinedStage ->
                // If the stage was successfully joined in backend - create and observe the Stage object locally
                Timber.d("Stage joined: $joinedStage, $currentStageType")
                currentlyJoinedStageId = stage.stageId
                currentToken = joinedStage.token
                currentParticipantId = joinedStage.participantId
                currentStageType = joinedStage.stageType
                val pkModeScore = joinedStage.pkModeScore
                val pkModeSessionTime = joinedStage.pkModeSessionTime
                if (pkModeScore != null && pkModeSessionTime != null) {
                    Timber.d("Joined voting session with score and remained time: $pkModeScore; $pkModeSessionTime")
                    onActiveSessionScore.send(pkModeScore)
                    onActiveSessionTime.send(pkModeSessionTime)
                }

                createAndObserveNewStageJobs()
                stageManager.joinStage(
                    stageId = joinedStage.stageId,
                    token = joinedStage.token,
                    type = joinedStage.stageType,
                    selfParticipantId = joinedStage.participantId,
                    isCreator = false
                )
                stageManager.observeStage()
                createChat(joinedStage.stageId, appSettingsStore.getStageId(), joinedStage.region)
            }
            response.onFailure {
                currentlyJoinedStageId = null
            }
        }
    }

    /**
     * Called whenever something changes in the stage to update the UI.
     * This function is called very frequently and should always contain the latest up to date state of the UI.
     */
    private suspend fun updateStages() {
        val stageList = StageListModel(stageCount = availableStages.size)
        Timber.d("Updating stages: ${availableStages.isNotEmpty()}, $currentPosition")
        if (availableStages.isNotEmpty()) {
            val centerStage = availableStages.getByIndexOrFirst(index = currentPosition)?.copy(
                isVideoStatsEnabled = appSettingsStore.isVideoStatsEnabled()
            )
            // For convenience we crate a local function here not in the class
            fun clearPreviousStageVideos(stage: StageUIModel?): StageUIModel? {
                if (stage != null && stage.stageId != currentStageIdByPosition && stage in availableStages) {
                    val index = availableStages.indexOf(stage)
                    availableStages[index] = stage.copy(creatorVideo = null, guestVideo = null)
                    return availableStages[index]
                }
                return stage
            }

            // Makes sure to clear video sources for scrolled stages
            var topStage: StageUIModel? = availableStages.getByIndexOrLast(index = currentPosition - 1)
            topStage = clearPreviousStageVideos(topStage)

            var bottomStage: StageUIModel? = availableStages.getByIndexOrFirst(index = currentPosition + 1)
            bottomStage = clearPreviousStageVideos(bottomStage)

            stageList.stageTop = topStage
            stageList.stageCenter = centerStage
            stageList.stageBottom = bottomStage
            stageList.stageDummy = availableStages.getByIndexOrFirst(index = currentPosition + 2)
        }
        _stages.update { stageList }
    }

    /**
     * Called when a new stage is created or joined.
     * The function requests a new chat token and starts observing the chat room.
     * Any previous chat room will be disposed.
     */
    private suspend fun createChat(hostId: String, userId: String, region: String) = runCancellableCatching(
        tryBlock = {
            Timber.d("Creating new local chat: $hostId")
            val userAvatar = appSettingsStore.getUserAvatar()
            val response = networkClient.getOrCreateApi().createChat(
                CreateChatRequest(
                    hostId = hostId,
                    userId = userId,
                    attributes = UserAttributes(
                        avatarColLeft = userAvatar.colorLeft,
                        avatarColRight = userAvatar.colorRight,
                        avatarColBottom = userAvatar.colorBottom,
                        username = appSettingsStore.getStageId()
                    )
                )
            )
            chatManager.joinRoom(response.asChatToken(), region, userId, hostId)
        }, errorBlock = { e ->
            Timber.e(e, "Failed to create chat")
        }
    )

    /**
     * Called by the create and join stage use cases.
     * Creates a new [StageManager] instance, observes it and disposes the old one as well
     * as cancelling any ongoing stage observation jobs.
     */
    private fun createAndObserveNewStageJobs() {
        val oldManager = stageManager
        stageJobs.forEach { it.cancel() }
        stageJobs.clear()
        stageManager = StageManager(context, appSettingsStore, ioScope)
        Timber.d("New stage manager created")
        stageJobs.add(createStageEventJob())
        stageJobs.add(ioScope.launch {
            stageManager.isLocalUserSpeaking.collect { isSpeaking ->
                isLocalUserSpeaking = isSpeaking
                availableStages.update(
                    index = currentPosition,
                    _isSpeaking = isLocalUserSpeaking && !stageManager.isLocalAudioOff(),
                    _isLocalAudioOff = stageManager.isLocalAudioOff()
                )
                if (currentStageType == StageType.AUDIO) {
                    availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes)
                }
                updateStages()
            }
        })
        stageJobs.add(ioScope.launch {
            stageManager.rtcData.collect { data ->
                _stageRTCData.update { data.copy() }
            }
        })
        stageJobs.add(ioScope.launch {
            stageManager.rtcDataList.collect { dataList ->
                _stageRTCDataList.update { dataList.map { it.copy() } }
            }
        })
        oldManager.leaveStage()
        oldManager.stopPublishing()
    }

    /**
     * Observes the currently connected stage and delivers updates to the UI.
     * The function returns a [Job] that can be terminated when requested f.e. when switching
     * the currently active stage by scrolling or some other event. It's necessary to terminate
     * this coroutine to escape memory leaks and ensure that only the currently connected stage is
     * observed.
     */
    private fun createStageEventJob() = ioScope.launch {
        stageManager.onEvent.collect { event ->
            Timber.d("Stage event received - $event")
            when (event) {
                is StageEvent.CreatorJoined -> {
                    availableStages.update(
                        index = currentPosition,
                        participantId = event.participantId,
                        _isCreatorVideoOff = event.isVideoOff,
                        _isCreatorAudioOff = event.isAudioOff,
                        _creatorVideo = event.video,
                        _creatorAvatar = event.userAvatar
                    )
                    if (event.isVideoOff) {
                        availableStages.update(index = currentPosition, keepCreatorVideo = false)
                    }
                    availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes)
                    updateStages()
                }
                is StageEvent.CreatorUpdated -> {
                    availableStages.update(
                        index = currentPosition,
                        participantId = event.participantId,
                        _isCreatorVideoOff = event.isVideoOff,
                        _isCreatorAudioOff = event.isAudioOff,
                        _isLocalAudioOff = stageManager.isLocalAudioOff(),
                        _creatorVideo = event.video
                    )
                    if (event.isVideoOff == true) {
                        availableStages.update(index = currentPosition, keepCreatorVideo = false)
                    }
                    availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes)
                    updateStages()
                }
                is StageEvent.GuestJoined -> {
                    availableStages.update(
                        index = currentPosition,
                        participantId = event.participantId,
                        _isGuestVideoOff = event.isVideoOff,
                        _isGuestAudioOff = event.isAudioOff,
                        _guestVideo = event.video,
                        _isGuestJoined = true
                    )
                    if (event.isVideoOff) {
                        availableStages.update(index = currentPosition, keepGuestVideo = false)
                    }
                    availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes)
                    updateStages()
                }
                is StageEvent.GuestUpdated -> {
                    availableStages.update(
                        index = currentPosition,
                        participantId = event.participantId,
                        _isGuestVideoOff = event.isVideoOff,
                        _isGuestAudioOff = event.isAudioOff,
                        _isLocalAudioOff = stageManager.isLocalAudioOff(),
                        _guestVideo = event.video
                    )
                    if (event.isVideoOff == true) {
                        availableStages.update(index = currentPosition, keepGuestVideo = false)
                    }
                    availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes)
                    updateStages()
                }
                is StageEvent.LocalParticipantUpdated -> {
                    availableStages.update(
                        index = currentPosition,
                        participantId = event.participantId,
                        _isCreator = event.isCreator,
                        _isParticipant = event.isParticipant,
                        _isCameraSwitched = event.isFacingBack,
                        _isCreatorAudioOff = if (event.isCreator) event.isAudioOff else null,
                        _isCreatorVideoOff = if (event.isCreator) event.isVideoOff else null,
                        _isGuestAudioOff = if (event.isParticipant) event.isAudioOff else null,
                        _isGuestVideoOff = if (event.isParticipant) event.isVideoOff else null,
                        _isLocalAudioOff = stageManager.isLocalAudioOff(),
                        _creatorVideo = if (event.isCreator) event.video else null,
                        _guestVideo = if (!event.isCreator) event.video else null,
                        _isGuestJoined = if (event.isParticipant) true else if (!event.isCreator) false else null
                    )
                    availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes)
                    updateStages()
                }
                is StageEvent.VideoStatsUpdated -> {
                    availableStages.update(
                        index = currentPosition,
                        _creatorTTV = event.creatorTTV,
                        _creatorLatency = event.creatorLatency,
                        _guestTTV = event.guestTTV,
                        _guestLatency = event.guestLatency
                    )
                    updateStages()
                }
                StageEvent.GuestLeft -> {
                    // Reset to default values
                    availableStages.update(
                        index = currentPosition,
                        _isGuestAudioOff = false,
                        _isGuestVideoOff = false,
                        _isGuestJoined = false,
                        _isGuestMode = false,
                        _isPKMode = false
                    )
                    availableStages.update(index = currentPosition, keepGuestVideo = false)
                    availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes)
                    updateStages()
                }
                StageEvent.CreatorLeft,
                StageEvent.StageGone -> {
                    // Reset to default values
                    availableStages.update(
                        index = currentPosition,
                        _isCreatorAudioOff = false,
                        _isCreatorVideoOff = false,
                        _isGuestAudioOff = false,
                        _isGuestVideoOff = false,
                        _isGuestJoined = false,
                        _isGuestMode = false,
                        _isPKMode = false
                    )
                    availableStages.update(
                        index = currentPosition,
                        keepCreatorVideo = false,
                        keepGuestVideo = false
                    )
                    stageManager.stopPublishing()
                    stageManager.leaveStage()
                    Timber.d("Stage is GONE: ${availableStages.size}, $currentPosition")
                    availableStages.getOrNull(currentPosition)?.let { joinStage(it) } ?: run {
                        Timber.d("Current stage not found - resetting: $currentPosition to: 0")
                        currentPosition = 0
                        currentStageType = null
                        currentToken = null
                        currentParticipantId = null
                        currentlyJoinedStageId = null
                        updateStages()
                    }
                    getStages()
                }
                StageEvent.GuestSpeakingStateUpdated -> {
                    availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes)
                    updateStages()
                }
            }
        }
    }

    /**
     * Observes the currently connected stage audio seats and delivers updates to the UI.
     * The function returns a [Job] that can be terminated when requested f.e. when switching
     * the currently active stage by scrolling or some other event. It's necessary to terminate
     * this coroutine to escape memory leaks and ensure that only the currently connected stage is
     * observed.
     */
    private fun createOnSeatsUpdatedJob() = ioScope.launch {
        chatManager.onSeatsUpdated.collect { seats ->
            Timber.d("Seats collected: $seats")
            availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes, seats)
            stageManager.removeParticipantsNotInSeats(seats)
            updateStages()
        }
    }

    /**
     * Observes the currently connected stage mode and delivers updates to the UI.
     * The function returns a [Job] that can be terminated when requested f.e. when switching
     * the currently active stage by scrolling or some other event. It's necessary to terminate
     * this coroutine to escape memory leaks and ensure that only the currently connected stage is
     * observed.
     */
    private fun createOnModeChangedJob() = ioScope.launch {
        chatManager.onModeChanged.collect { mode ->
            val currentStage = availableStages.getOrNull(currentPosition) ?: return@collect
            Timber.d("Stage mode changed to $mode")

            when (mode) {
                StageMode.GUEST_SPOT.name -> {
                    stageManager.updateMode(StageType.VIDEO, StageMode.GUEST_SPOT)
                    availableStages.update(
                        index = currentPosition,
                        keepCreatorVideo = false,
                        keepGuestVideo = false,
                        _isGuestMode = true,
                        _isPKMode = false,
                        _creatorVideo = stageManager.getCreatorVideo() ?: currentStage.creatorVideo,
                        _guestVideo = stageManager.getGuestVideo() ?: currentStage.guestVideo
                    )
                    updateStages()
                }
                StageMode.PK.name -> {
                    stageManager.updateMode(StageType.VIDEO, StageMode.PK)
                    availableStages.update(
                        index = currentPosition,
                        keepCreatorVideo = false,
                        keepGuestVideo = false,
                        _isGuestMode = false,
                        _isPKMode = true,
                        _creatorVideo = stageManager.getCreatorVideo() ?: currentStage.creatorVideo,
                        _guestVideo = stageManager.getGuestVideo() ?: currentStage.guestVideo
                    )
                    updateStages()
                }
                StageMode.NONE.name -> {
                    stageManager.stopPublishing()
                    stageManager.updateMode(StageType.VIDEO, StageMode.NONE)
                    availableStages.update(
                        index = currentPosition,
                        keepCreatorVideo = false,
                        keepGuestVideo = false,
                        _isGuestMode = false,
                        _isPKMode = false,
                        _creatorVideo = stageManager.getCreatorVideo() ?: currentStage.creatorVideo,
                        _guestVideo = null
                    )
                    updateStages()
                }
            }
        }
    }
}
