package com.amazon.ivs.stagesrealtime.repository

import android.content.Context
import com.amazon.ivs.stagesrealtime.common.Failure
import com.amazon.ivs.stagesrealtime.common.Success
import com.amazon.ivs.stagesrealtime.common.VOTE_SESSION_TIME_SECONDS
import com.amazon.ivs.stagesrealtime.common.emptySeats
import com.amazon.ivs.stagesrealtime.common.extensions.asPKModeScore
import com.amazon.ivs.stagesrealtime.common.extensions.getElapsedTimeFromNow
import com.amazon.ivs.stagesrealtime.common.extensions.launchIO
import com.amazon.ivs.stagesrealtime.common.extensions.launchMain
import com.amazon.ivs.stagesrealtime.common.getNewStageId
import com.amazon.ivs.stagesrealtime.repository.chat.ChatManager
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
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.CreateStageRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.DeleteStageRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.DisconnectUserRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.Error
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.JoinStageRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.UpdateSeatsRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.UpdateStageModeRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.VoteRequest
import com.amazon.ivs.stagesrealtime.repository.networking.models.update
import com.amazon.ivs.stagesrealtime.repository.networking.models.updateOrAdd
import com.amazon.ivs.stagesrealtime.repository.networking.models.updateSeats
import com.amazon.ivs.stagesrealtime.repository.stage.StageEvent
import com.amazon.ivs.stagesrealtime.repository.stage.StageManager
import com.amazon.ivs.stagesrealtime.ui.stage.models.AudioSeatUIModel
import com.amazon.ivs.stagesrealtime.ui.stage.models.ScrollDirection
import com.amazon.ivs.stagesrealtime.ui.stage.models.StageListModel
import com.amazon.ivs.stagesrealtime.ui.stage.models.StageUIModel
import com.amazonaws.ivs.chat.messaging.requests.SendMessageRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import kotlin.properties.Delegates

class StageRepository(
    private val context: Context,
    private val preferenceProvider: PreferenceProvider,
    private val networkClient: NetworkClient,
    private val chatManager: ChatManager
) {
    private val api get() = networkClient.getOrCreateApi()
    private val availableStages = mutableListOf<StageUIModel>()

    private val _stages = MutableStateFlow(StageListModel())
    private val _stageRTCData = MutableStateFlow(RTCData())
    private val _stageRTCDataList = MutableStateFlow(emptyList<RTCData>())
    private val _onActiveSessionScore = Channel<PKModeScore>()
    private val _onActiveSessionTime = Channel<PKModeSessionTime>()
    private var stageManager = StageManager(context, preferenceProvider.bitrate)
    private val stageJobs = mutableListOf<Job>()

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

    val messages = chatManager.messages
    val onStageLike = chatManager.onStageLike
    val onPKModeScore = merge(chatManager.onPKModeScore, _onActiveSessionScore.receiveAsFlow())
    val onVoteStart = merge(chatManager.onVoteStart, _onActiveSessionTime.receiveAsFlow())
    val stageRTCData = _stageRTCData.asStateFlow()
    val stageRTCDataList = _stageRTCDataList.asStateFlow()

    val stages by lazy {
        createOnSeatsUpdatedJob()
        createOnModeChangedJob()
        _stages.asStateFlow()
    }

    var stageId by Delegates.observable(preferenceProvider.stageId ?: getNewStageId()) { _, _, id ->
        preferenceProvider.stageId = id
    }
    var userAvatar by Delegates.observable(preferenceProvider.userAvatar) { _, _, avatar ->
        preferenceProvider.userAvatar = avatar
    }
    var customerCode by Delegates.observable(preferenceProvider.customerCode) { _, _, code ->
        preferenceProvider.customerCode = code
        if (code == null) {
            networkClient.destroyApi()
        }
    }
    var bitrate by Delegates.observable(preferenceProvider.bitrate) { _, _, bitrate ->
        Timber.d("New bitrate set: $bitrate")
        preferenceProvider.bitrate = bitrate
    }
    var apiKey by Delegates.observable(preferenceProvider.apiKey) { _, _, apiKey ->
        Timber.d("New api key set: $apiKey")
        preferenceProvider.apiKey = apiKey
    }

    fun scrollStages(direction: ScrollDirection) {
        val stageCount = availableStages.size
        scrollDirection = direction
        val lastPosition = currentPosition
        currentPosition = when (direction) {
            ScrollDirection.UP -> if (currentPosition - 1 >= 0) currentPosition - 1 else stageCount - 1
            ScrollDirection.DOWN -> if (currentPosition + 1 < stageCount) currentPosition + 1 else 0
            ScrollDirection.NONE -> {
                currentPosition
                return
            }
        }
        val positionChanged = lastPosition != currentPosition
        Timber.d("Feed scrolled: $direction to index: $currentPosition, will refresh: $positionChanged")
        updateStages()
        val currentStage = availableStages.getOrNull(currentPosition)
        if (positionChanged && currentStage != null) {
            joinStage(currentStage)
        } else {
            updateStages()
        }
    }

    suspend fun createStage(type: StageType) = try {
        val userAvatar = preferenceProvider.userAvatar
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
        currentToken = token
        currentParticipantId = hostParticipantId
        currentStageType = type
        Timber.d("Stage created: $response for $userAvatar, $currentStageType")
        val isAudioMode = type == StageType.AUDIO
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
        availableStages.add(
            StageUIModel(
                stageId = stageId,
                creatorAvatar = userAvatar,
                selfAvatar = userAvatar,
                isCreator = true,
                isAudioMode = isAudioMode,
                seats = seats
            )
        )
        createAndObserveNewStageJobs()
        stageManager.joinStage(
            stageId = stageId,
            hostParticipantId = hostParticipantId,
            token = token,
            type = type,
            isCreator = true
        )
        stageManager.observeStage()
        updateStages()
        createChat(stageId, stageId, region)
        Success()
    } catch (e: Exception) {
        Timber.e(e, "Failed to create stage")
        Failure(Error.CreateStageError)
    }

    suspend fun onSeatClicked(index: Int) = try {
        val stage = availableStages.getOrNull(currentPosition) ?: run { return Failure(Error.UpdateSeatsError) }
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
                seat.copy(participantId = id, userAvatar = userAvatar, isMuted = stageManager.isLocalAudioOff())
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
        api.updateSeats(UpdateSeatsRequest(hostId = stage.stageId, userId = stageId, seats = seats.getIDs()))
        Success()
    } catch (e: Exception) {
        Timber.e(e, "Failed to update seats")
        Failure(Error.UpdateSeatsError)
    }

    suspend fun getStages() = try {
        val response = api.getStages()
        Timber.d("Stages received: ${availableStages.count()}, $response, $currentPosition, $currentlyJoinedStageId")
        @Suppress("UNNECESSARY_SAFE_CALL")
        // TODO: There is a race condition where stage.stageId can throw a null pointer
        val staleStages =
            availableStages.toList().filter { stage -> response.stages.none { it.hostId == stage?.stageId } }
        Timber.d("Stale stages: $staleStages")
        staleStages.forEach { staleStage ->
            availableStages.remove(staleStage)
            if (staleStage.stageId == currentlyJoinedStageId) {
                currentlyJoinedStageId = null
            }
        }
        if (currentPosition >= availableStages.count()) currentPosition = 0

        val indexOfCurrentStage = availableStages.indexOfFirst { it.stageId == currentlyJoinedStageId }
        if (indexOfCurrentStage != -1 && indexOfCurrentStage != currentPosition) currentPosition = indexOfCurrentStage

        availableStages.updateOrAdd(stageId, response.stages, userAvatar)
        availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes)
        val currentStage = availableStages.getOrNull(currentPosition)
        if (currentStage != null && currentStage.stageId != currentlyJoinedStageId) {
            joinStage(currentStage)
        } else {
            updateStages()
        }
        Success()
    } catch (e: Exception) {
        Timber.e(e, "Failed to get stages")
        Failure(Error.GetStagesError)
    }

    suspend fun verifyConnectionCode() = try {
        Timber.d("Verifying the connection with the API")
        val response = api.verifyConnectionCode()
        if (response.isSuccessful) {
            Success()
        } else {
            Failure(Error.CustomerCodeError)
        }
    } catch (e: Exception) {
        Timber.d("Failed to connect to backend")
        Failure(Error.CustomerCodeError)
    }

    suspend fun startPublishing(mode: StageMode, updateMode: Boolean = true) = try {
        Timber.d("Start publishing: $mode, $currentStageIdByPosition, $currentStageType")
        val type = currentStageType ?: run { return Failure(Error.JoinStageError) }
        val stageIdCurrent = currentStageIdByPosition ?: run { return Failure(Error.JoinStageError) }
        if (updateMode) {
            api.updateStageMode(UpdateStageModeRequest(hostId = stageIdCurrent, userId = stageId, mode = mode))
        }
        stageManager.startPublishing(type, mode)
        availableStages.update(
            index = currentPosition,
            _isPKMode = mode == StageMode.PK,
            _isGuestMode = mode == StageMode.GUEST_SPOT
        )
        Success()
    } catch (e: Exception) {
        Timber.d(e, "Failed to start publishing on position $currentPosition")
        Failure(Error.JoinStageError)
    }

    suspend fun stopPublishing() = try {
        Timber.d("Leave stage: $currentParticipantId, $currentStageIdByPosition")
        val stageIdCurrent = currentStageIdByPosition ?: run { return Failure(Error.LeaveStageError) }
        if (currentStageType == StageType.AUDIO) {
            val stage = availableStages.getOrNull(currentPosition) ?: run { return Failure(Error.LeaveStageError) }
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
            api.updateSeats(UpdateSeatsRequest(hostId = stage.stageId, userId = stageId, seats = seats.getIDs()))
        } else {
            api.updateStageMode(
                UpdateStageModeRequest(
                    hostId = stageIdCurrent,
                    userId = stageId,
                    mode = StageMode.NONE
                )
            )
        }
        stageManager.stopPublishing()
        Success()
    } catch (e: Exception) {
        Failure(Error.LeaveStageError)
    }

    fun clearResources() {
        stageJobs.forEach { it.cancel() }
        stageJobs.clear()
        chatManager.clearPreviousChat()
        currentlyJoinedStageId = null
        currentPosition = 0
        currentStageType = null
        currentToken = null
        currentParticipantId = null
        availableStages.clear()
    }

    suspend fun disconnectFromCurrentStage() = try {
        stageManager.leaveStage()
        val participantId = currentParticipantId ?: run { return Failure(Unit) }
        val hostId = currentStageIdByPosition ?: run { return Failure(Unit) }
        Timber.d("Disconnecting from $hostId, $stageId, $participantId")
        api.disconnectUser(DisconnectUserRequest(hostId, stageId, participantId))
        Success()
    } catch (e: Exception) {
        Timber.d(e, "Failed to disconnect from current stage")
        Failure(Unit)
    }

    suspend fun kickParticipant() = try {
        Timber.d("Kicking participant and updating stage to NONE")
        api.updateStageMode(UpdateStageModeRequest(hostId = stageId, userId = stageId, mode = StageMode.NONE))
        Success()
    } catch (e: Exception) {
        Failure(Error.KickParticipantError)
    }

    suspend fun deleteStage() = try {
        api.deleteStage(DeleteStageRequest(stageId))
        Timber.d("Stage deleted")
        availableStages.clear()
        stageManager.leaveStage()
        clearResources()
        updateStages()
        Success()
    } catch (e: Exception) {
        Timber.d("Failed to delete stage")
        Failure(Error.DeleteStageError)
    }

    suspend fun castVote(voteHost: Boolean) = try {
        val hostId = currentStageIdByPosition ?: run { return Failure(Error.CastVoteError) }
        val voteBody = VoteRequest(
            hostId,
            if (voteHost) hostId
            else if (stageManager.isParticipating() && !stageManager.isStageCreator()) stageId
            else stageManager.getGuestId()!!
        )
        Timber.d("Casting vote for $voteBody")
        api.castVote(voteBody)
        Success()
    } catch (e: Exception) {
        Timber.d(e, "Failed to cast vote")
        Failure(Error.CastVoteError)
    }

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

    fun switchFacing() {
        if (isParticipating()) stageManager.switchFacing()
    }

    fun canScroll() = availableStages.size > 1 && !isParticipating()

    fun requestRTCStats() = stageManager.requestRTCStats()

    fun getPKModeWinnerAvatar(hostWin: Boolean): UserAvatar {
        availableStages.getOrNull(currentPosition)?.let { stage ->
            val isCreator = stage.isCreator
            val isParticipant = stage.isParticipant
            val hostId = stage.stageId
            val guestId = stageManager.getGuestId()
            Timber.d("Get PK Mode winner: $hostId, $guestId")
            return when (hostWin) {
                true -> if (isCreator) userAvatar else stageManager.getParticipantAvatar(hostId)
                else -> if (isParticipant) userAvatar else stageManager.getParticipantAvatar(guestId)
            } ?: UserAvatar()
        } ?: return UserAvatar()
    }

    private fun getParticipantAttributes(participantId: String): ParticipantAttributes? {
        availableStages.getOrNull(currentPosition)?.let { stage ->
            return if (currentParticipantId == participantId) {
                ParticipantAttributes(
                    stageId = stage.stageId,
                    participantId = participantId,
                    userAvatar = userAvatar,
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

    private fun updateStages() {
        val stageList = StageListModel(stageCount = availableStages.size)
        Timber.d("Updating stages: ${availableStages.isNotEmpty()}, $currentPosition")
        if (availableStages.isNotEmpty()) {
            val centerStage = availableStages.getByIndexOrFirst(index = currentPosition)

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

    private fun clearPreviousStageVideos(stage: StageUIModel?): StageUIModel? {
        if (stage != null && stage.stageId != currentStageIdByPosition && stage in availableStages) {
            val index = availableStages.indexOf(stage)
            availableStages[index] = stage.copy(creatorVideo = null, guestVideo = null)
            return availableStages[index]
        }
        return stage
    }

    private fun joinStage(stage: StageUIModel) {
        Timber.d("Attempting join stage - $stage; $currentlyJoinedStageId")
        currentJoinJob?.cancel()
        currentJoinJob = launchMain {
            try {
                if (isStageCreator() || stage.stageId == currentlyJoinedStageId) return@launchMain
                currentlyJoinedStageId = stage.stageId
                val userAvatar = preferenceProvider.userAvatar
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
                response.metadata.activeVotingSession?.let { votingSession ->
                    val secondsRemaining = VOTE_SESSION_TIME_SECONDS - votingSession.startedAt.getElapsedTimeFromNow()
                    val currentScore = votingSession.tally.asPKModeScore(
                        hostId = stage.stageId,
                        shouldResetScore = true
                    )
                    Timber.d("Joined voting session with score and remained time: $currentScore; $secondsRemaining")
                    _onActiveSessionScore.send(currentScore)
                    _onActiveSessionTime.send(PKModeSessionTime(secondsRemaining = secondsRemaining))
                }
                currentToken = token
                currentParticipantId = participantId
                currentStageType = stageType
                Timber.d("Stage joined: $response, $currentStageType")
                createAndObserveNewStageJobs()
                stageManager.joinStage(
                    stageId = stage.stageId,
                    token = token,
                    type = stageType,
                    isCreator = false
                )
                stageManager.observeStage()
                createChat(stage.stageId, stageId, region)
            } catch (e: Exception) {
                Timber.e(e, "Failed to join stage")
                currentlyJoinedStageId = null
            }
        }
    }

    private suspend fun createChat(hostId: String, userId: String, region: String) {
        try {
            Timber.d("Creating new local chat: $hostId")
            val userAvatar = preferenceProvider.userAvatar
            val response = api.createChat(
                CreateChatRequest(
                    hostId = hostId,
                    userId = userId,
                    attributes = UserAttributes(
                        avatarColLeft = userAvatar.colorLeft,
                        avatarColRight = userAvatar.colorRight,
                        avatarColBottom = userAvatar.colorBottom,
                        username = stageId
                    )
                )
            )
            chatManager.joinRoom(response.asChatToken(), region, userId, hostId)
        } catch (e: Exception) {
            Timber.d("Failed to create chat")
        }
    }

    private fun List<StageUIModel>.getByIndexOrFirst(index: Int) = getOrElse(index) { firstOrNull() }
    private fun List<StageUIModel>.getByIndexOrLast(index: Int) = getOrElse(index) { lastOrNull() }

    private fun createAndObserveNewStageJobs() {
        val oldManager = stageManager
        stageJobs.forEach { it.cancel() }
        stageJobs.clear()
        stageManager = StageManager(context, bitrate)
        Timber.d("New stage manager created")
        stageJobs.add(createStageEventJob())
        stageJobs.add(launchIO {
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
        stageJobs.add(launchIO {
            stageManager.rtcData.collect { data ->
                _stageRTCData.update { data.copy() }
            }
        })
        stageJobs.add(launchIO {
            stageManager.rtcDataList.collect { dataList ->
                _stageRTCDataList.update { dataList.map { it.copy() } }
            }
        })
        oldManager.leaveStage()
        oldManager.stopPublishing()
    }

    private fun createStageEventJob() = launchIO {
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

    private fun createOnSeatsUpdatedJob() = launchIO {
        chatManager.onSeatsUpdated.collect { seats ->
            Timber.d("Seats collected: $seats")
            availableStages.updateSeats(currentPosition, stageManager, ::getParticipantAttributes, seats)
            stageManager.removeParticipantsNotInSeats(seats)
            updateStages()
        }
    }

    private fun createOnModeChangedJob() = launchIO {
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
