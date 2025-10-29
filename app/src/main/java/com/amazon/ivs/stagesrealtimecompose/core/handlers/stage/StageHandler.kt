package com.amazon.ivs.stagesrealtimecompose.core.handlers.stage

import com.amazon.ivs.stagesrealtimecompose.core.common.GET_STAGES_REFRESH_DELAY
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.isNullOrFalse
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.launchDefault
import com.amazon.ivs.stagesrealtimecompose.core.handlers.Destination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.ErrorDestination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.StageDestinationType
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.VSHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.chat.ChatHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.Error
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.NetworkHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.StageModeLegacy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

object StageHandler {
    private val _stages = MutableStateFlow(emptyList<Stage>())
    private val _hearts = MutableStateFlow(0)
    private var _currentStageId: String? = null
    private var _getStagesJob: Job? = null
    private var _selfParticipantId: String? = null

    val stages = _stages.asStateFlow()
    val hearts = _hearts.asStateFlow()
    val currentStage get() = _stages.value.find { it.stageId == _currentStageId }
    val selfParticipantId get() = _selfParticipantId

    init {
        launchDefault {
            StageManager.participants.collect {
                updateParticipants()
            }
        }
    }

    fun dispose() {
        Timber.d("Disposing stage handler, current stage: $_currentStageId")
        StageManager.dispose()
        _currentStageId = null
        _getStagesJob?.cancel()
        _getStagesJob = null
    }

    fun onStageStarted(type: StageDestinationType) {
        Timber.d("Stage started: $type")
        when (type) {
            StageDestinationType.None -> getStages()
            else -> {
                _getStagesJob?.cancel()
                _getStagesJob = null
            }
        }
    }

    fun loadPage(index: Int) = launchDefault {
        val currentIndex = _stages.value.indexOfFirst { it.stageId == _currentStageId }
        if (currentIndex == index) return@launchDefault
        currentStage?.run {
            Timber.d("Leaving current stage: $stageId")
            val seats = seats.updateSelfSeat { seat ->
                seat.copy(
                    participantId = null,
                    userAvatar = null
                )
            }
            updateStage { stage ->
                stage.copy(
                    seats = seats,
                    mode = StageParticipantMode.None,
                    isStageParticipant = false
                )
            }
            StageManager.stopPublishing()
            StageManager.dispose()
        }

        val stageId = _stages.value.getOrNull(index)?.stageId
        _currentStageId = stageId
        Timber.d("Loading page: $index, $_currentStageId")
        val stages = _stages.value.map { stage ->
            stage.copy(isLoading = stage.stageId.isBlank() || stage.stageId != _currentStageId)
        }
        if (stageId != null) {
            joinStage()
        }
        _stages.update { stages }
    }

    fun switchMic() {
        val switched = StageManager.switchMic()
        Timber.d("Mic switched: $switched")
        updateStage { stage ->
            val seats = stage.seats.updateSelfSeat { seat ->
                seat.copy(isMuted = switched)
            }
            if (stage.isStageCreator) {
                stage.copy(isCreatorMicOff = switched, seats = seats)
            } else if (stage.isStageParticipant) {
                stage.copy(isParticipantMicOff = switched, seats = seats)
            } else {
                stage.copy()
            }
        }
    }

    fun switchCamera() {
        val switched = StageManager.switchCamera()
        Timber.d("Camera switched: $switched")
        updateStage { stage ->
            if (stage.isStageCreator) {
                stage.copy(isCreatorVideoOff = switched)
            } else if (stage.isStageParticipant) {
                stage.copy(isParticipantVideoOff = switched)
            } else {
                stage.copy()
            }
        }
    }

    fun switchFacing() {
        val switched = StageManager.switchFacing()
        Timber.d("Facing switched: $switched")
        updateStage { stage ->
            if (stage.isStageCreator || stage.isStageParticipant) {
                stage.copy(isFacingSwitched = switched)
            } else {
                stage.copy()
            }
        }
    }

    fun addHeart() {
        val hearts = _hearts.value + 1
        _hearts.update { hearts }
    }

    fun clearHearts() {
        _hearts.update { 0 }
    }

    fun onStageGone() {
        val currentStageIndex = _stages.value.indexOfFirst {
            it.stageId == _currentStageId
        }.takeIf { it >= 0 } ?: return

        Timber.d("Stage gone: ${currentStage?.stageId} at index: $currentStageIndex")
        ChatHandler.clearMessages()
        StageManager.stopPublishing()
        val stages = _stages.value.map { it.copy() }.toMutableList()
        if (stages.isNotEmpty()) stages.removeAt(currentStageIndex)
        _stages.update { stages }

        val nextStageIndex = (currentStageIndex - 1).coerceAtLeast(0)
        loadPage(nextStageIndex)
    }

    fun endStage() = launchDefault {
        val currentStageIndex = _stages.value.indexOfFirst {
            it.stageId == _currentStageId
        }.takeIf { it >= 0 } ?: return@launchDefault
        Timber.d("Ending stage: ${currentStage?.stageId} at index: $currentStageIndex")
        StageManager.stopPublishing()
        NavigationHandler.setLoading(true)
        NetworkHandler.deleteStage().handle(
            onSuccess = {
                NavigationHandler.setLoading(false)
                NavigationHandler.goBack()
                NavigationHandler.goTo(Destination.Stage(StageDestinationType.None))

                val stages = _stages.value.map { it.copy() }.toMutableList()
                stages.removeAt(currentStageIndex)
                _stages.update { stages }
            },
            onFailure = { error ->
                NavigationHandler.setLoading(false)
                NavigationHandler.showError(ErrorDestination.SnackBar(error = error))
            }
        )
    }

    fun joinAudioRoom(id: Int) = launchDefault {
        val stage = currentStage ?: run {
            NavigationHandler.showError(ErrorDestination.SnackBar(Error.UpdateSeatsError))
            return@launchDefault
        }
        val selfUser = UserHandler.currentUser
        var seats = stage.seats.updateSelfSeat { seat ->
            seat.copy(
                participantId = null,
                userAvatar = null,
                isMuted = false,
                isSpeaking = false
            )
        }
        seats = seats.map { seat ->
            if (seat.id == id && seat.isEmpty) {
                seat.copy(
                    participantId = StageManager.getParticipantId(selfUser.username),
                    userAvatar = selfUser.userAvatar.copy(hasBorder = true),
                    isMuted = stage.isMuted
                )
            } else {
                seat.copy()
            }
        }
        Timber.d("Joining audio room: ${stage.stageId}, on seat: $id")
        NavigationHandler.setLoading(true)
        NetworkHandler.updateSeats(
            stageId = stage.stageId,
            seats = seats.map { it.seatId }
        ).handle(
            onSuccess = {
                NavigationHandler.setLoading(false)
                StageManager.startPublishing()
                updateStage { stage ->
                    stage.copy(
                        seats = seats,
                        isStageParticipant = true
                    )
                }
            },
            onFailure = { error ->
                NavigationHandler.setLoading(false)
                NavigationHandler.showError(ErrorDestination.SnackBar(error = error))
            }
        )
    }

    fun leaveAudioRoom() = launchDefault {
        val stage = currentStage ?: run {
            NavigationHandler.showError(ErrorDestination.SnackBar(Error.UpdateSeatsError))
            return@launchDefault
        }
        val seats = stage.seats.updateSelfSeat { seat ->
            seat.copy(
                participantId = null,
                userAvatar = null
            )
        }
        Timber.d("Leaving audio room: ${stage.stageId}")
        StageManager.stopPublishing()
        NavigationHandler.setLoading(true)
        NetworkHandler.updateSeats(
            stageId = stage.stageId,
            seats = seats.map { it.seatId }
        ).handle(
            onSuccess = {
                NavigationHandler.setLoading(false)
                NavigationHandler.goBack()
                updateStage { stage ->
                    stage.copy(
                        seats = seats,
                        isStageParticipant = false
                    )
                }
            },
            onFailure = { error ->
                NavigationHandler.setLoading(false)
                NavigationHandler.showError(ErrorDestination.SnackBar(error = error))
            }
        )
        getStages()
    }

    fun updateSeats(seats: List<String>) {
        updateStage { stage ->
            val updatedSeats = stage.seats.mapIndexed { index, seat ->
                val participantId = seats[index].takeIf { it.isNotBlank() }
                val participant = StageManager.getParticipant(participantId)
                seat.copy(
                    participantId = participantId,
                    userAvatar = participant?.userAvatar?.copy(hasBorder = true),
                    isMuted = participant?.isAudioOff == true,
                    isSpeaking = participant?.isSpeaking == true,
                )
            }
            stage.copy(seats = updatedSeats)
        }
    }

    fun createStage(type: StageType) = launchDefault {
        val user = UserHandler.currentUser
        val destinationType = when (type) {
            StageType.Video -> StageDestinationType.Video
            StageType.Audio -> StageDestinationType.Audio
        }
        Timber.d("Creating stage: $type")
        NavigationHandler.setLoading(true)
        NetworkHandler.createStage(type).handle(
            onSuccess = { response ->
                val participantId = response.hostParticipantToken.participantId
                _selfParticipantId = participantId
                if (type == StageType.Audio) {
                    val seats = createSeats(
                        participantId = participantId,
                        userAvatar = user.userAvatar
                    )
                    NetworkHandler.updateSeats(
                        stageId = UserHandler.currentUser.username,
                        seats = seats.map { it.seatId },
                    ).handle(
                        onSuccess = {
                            _currentStageId = UserHandler.currentUser.username
                            NavigationHandler.setLoading(false)
                            NavigationHandler.goTo(Destination.Stage(destinationType))
                            val stages = listOf(
                                Stage(
                                    stageId = UserHandler.currentUser.username,
                                    isStageCreator = true,
                                    type = type,
                                    seats = seats,
                                    isLoading = false,
                                )
                            )
                            _stages.update { stages }
                            launchDefault {
                                StageManager.subscribeToStage(mode = StageParticipantMode.None)
                                StageManager.startPublishing()
                            }
                        },
                        onFailure = { error ->
                            NavigationHandler.setLoading(false)
                            NavigationHandler.showError(ErrorDestination.SnackBar(error = error))
                        }
                    )
                    return@launchDefault
                }

                _currentStageId = UserHandler.currentUser.username
                NavigationHandler.setLoading(false)
                NavigationHandler.goTo(Destination.Stage(destinationType))
                val stages = listOf(
                    Stage(
                        stageId = UserHandler.currentUser.username,
                        isStageCreator = true,
                        type = type,
                        isLoading = false,
                    )
                )
                _stages.update { stages }
                Timber.d("Stage created: $_currentStageId")
                launchDefault {
                    StageManager.subscribeToStage(mode = StageParticipantMode.None)
                    StageManager.startPublishing()
                }
            },
            onFailure = { error ->
                NavigationHandler.setLoading(false)
                NavigationHandler.showError(ErrorDestination.SnackBar(error = error))
            }
        )
    }

    fun startPublishing(mode: StageParticipantMode) = launchDefault {
        val stageId = _currentStageId ?: run {
            Timber.d("Failed to start publishing, stage not found")
            NavigationHandler.showError(ErrorDestination.SnackBar(Error.UpdateModeError))
            return@launchDefault
        }
        Timber.d("Starting publishing: $stageId")
        NavigationHandler.setLoading(true)
        NetworkHandler.updateMode(
            stageId = stageId,
            mode = mode.asStageModeLegacy()
        ).handle(
            onSuccess = {
                NavigationHandler.goBack()
                NavigationHandler.setLoading(false)
                StageManager.subscribeToStage(mode = mode)
                StageManager.startPublishing()
                updateStage { stage ->
                    stage.copy(
                        mode = mode,
                        isStageParticipant = true
                    )
                }
            },
            onFailure = { error ->
                NavigationHandler.setLoading(false)
                NavigationHandler.showError(ErrorDestination.SnackBar(error = error))
            }
        )
    }

    fun leaveStage() = launchDefault {
        Timber.d("Leaving stage: $currentStage")
        val stage = currentStage ?: run {
            NavigationHandler.showError(ErrorDestination.SnackBar(Error.LeaveStageError))
            return@launchDefault
        }
        val seats = stage.seats.updateSelfSeat { seat ->
            seat.copy(
                participantId = null,
                userAvatar = null
            )
        }
        Timber.d("Stopping publishing: ${stage.stageId}")
        VSHandler.reset()
        NavigationHandler.setLoading(true)
        StageManager.stopPublishing()
        if (stage.isAudioRoom) {
            Timber.d("Leaving audio room: ${stage.stageId}")
            NetworkHandler.updateSeats(
                stageId = stage.stageId,
                seats = seats.map { it.seatId }
            ).handle(
                onSuccess = {
                    NavigationHandler.setLoading(false)
                    NavigationHandler.goBack()
                    updateStage { stage ->
                        stage.copy(
                            seats = seats,
                            isStageParticipant = false
                        )
                    }
                },
                onFailure = { error ->
                    NavigationHandler.setLoading(false)
                    NavigationHandler.showError(ErrorDestination.SnackBar(error = error))
                }
            )
        } else if (stage.isStageParticipant) {
            Timber.d("Leaving stage as participant: ${stage.stageId}")
            NetworkHandler.updateMode(
                stageId = stage.stageId,
                mode = StageModeLegacy.NONE
            ).handle(
                onSuccess = {
                    NavigationHandler.setLoading(false)
                    NavigationHandler.goBack()
                    updateStage { stage ->
                        stage.copy(
                            mode = StageParticipantMode.None,
                            isStageParticipant = false
                        )
                    }
                },
                onFailure = { error ->
                    NavigationHandler.setLoading(false)
                    NavigationHandler.showError(ErrorDestination.SnackBar(error = error))
                }
            )
        } else {
            Timber.d("Leaving stage: ${stage.stageId}")
            NavigationHandler.setLoading(false)
            NavigationHandler.goBack()
        }
        getStages()
    }

    fun updateMode(mode: StageParticipantMode) {
        Timber.d("Stage mode changed: $mode")
        updateStage { stage ->
            stage.copy(
                mode = mode,
                isStageParticipant = mode != StageParticipantMode.None && !stage.isStageCreator
            )
        }
        StageManager.subscribeToStage(mode = mode)
    }

    fun kickParticipant() = launchDefault {
        Timber.d("Kicking participant")
        NavigationHandler.setLoading(true)
        NetworkHandler.kickParticipant().handle(
            onSuccess = {
                NavigationHandler.setLoading(false)
                NavigationHandler.goBack()
                updateStage { stage ->
                    stage.copy(mode = StageParticipantMode.None)
                }
            }, onFailure = { error ->
                NavigationHandler.setLoading(false)
                NavigationHandler.showError(ErrorDestination.SnackBar(error = error))
            }
        )
    }

    fun castVote(forCreator: Boolean) = launchDefault {
        val stageId = _currentStageId ?: run {
            NavigationHandler.showError(ErrorDestination.SnackBar(error = Error.CastVoteError))
            return@launchDefault
        }
        val userName = StageManager.getParticipantName(
            isCreator = forCreator,
            stageId = stageId
        ) ?: run {
            NavigationHandler.showError(ErrorDestination.SnackBar(error = Error.CastVoteError))
            return@launchDefault
        }

        Timber.d("Casting vote: $stageId, $userName")
        VSHandler.castVote(forCreator = forCreator)
        NetworkHandler.castVote(
            stageId = stageId,
            userName = userName,
        )
    }

    private fun getStages() {
        _getStagesJob?.cancel()
        _getStagesJob = launchDefault {
            if (currentStage?.isJoined == true) {
                Timber.d("Stage joined - won't fetch new stages")
                return@launchDefault
            }

            Timber.d("Fetch stages")
            NetworkHandler.getStages().handle(
                onSuccess = { response ->
                    val newStages = _stages.value.update(response.stages)
                    _stages.update { newStages }
                    if (newStages.none { it.stageId == _currentStageId }) {
                        _currentStageId = null
                        if (newStages.isNotEmpty()) {
                            loadPage(0)
                        }
                    }
                    Timber.d("Stages fetched: ${newStages.size}, is joined: ${currentStage?.isJoined.isNullOrFalse()}")
                    if (currentStage?.isJoined.isNullOrFalse()) {
                        delay(GET_STAGES_REFRESH_DELAY)
                        getStages()
                    }
                }, onFailure = {
                    if (currentStage?.isJoined.isNullOrFalse()) {
                        delay(GET_STAGES_REFRESH_DELAY)
                        getStages()
                    }
                }
            )
        }
    }

    private fun joinStage() = launchDefault {
        val stageId = _currentStageId ?: run {
            NavigationHandler.showError(ErrorDestination.SnackBar(Error.JoinStageError))
            return@launchDefault
        }
        Timber.d("Joining stage: $stageId")
        ChatHandler.clearMessages()
        StageManager.stopPublishing()
        NavigationHandler.setLoading(true)
        NetworkHandler.leaveChat(stageId = stageId)
        NetworkHandler.joinStage(stageId = stageId).handle(
            onSuccess = { response ->
                Timber.d("Stage joined: $response")
                _selfParticipantId = response.participantId
                StageManager.subscribeToStage(mode = StageParticipantMode.None)
                NavigationHandler.setLoading(false)
            },
            onFailure = { error ->
                NavigationHandler.setLoading(false)
                NavigationHandler.showError(ErrorDestination.SnackBar(error = error))
            }
        )
    }

    private fun updateStage(stageDelegate: (Stage) -> Stage) {
        val stages = _stages.value.map { stage ->
            if (stage.stageId == _currentStageId) {
                stageDelegate(stage.copy())
            } else {
                stage.copy()
            }
        }
        _stages.update { stages }
    }

    private fun List<AudioSeat>.updateSelfSeat(seatsDelegate: (AudioSeat) -> AudioSeat) = map { seat ->
        if (seat.participantId == StageManager.getParticipantId(UserHandler.currentUser.username)) {
            seatsDelegate(seat.copy())
        } else {
            seat.copy()
        }
    }

    private fun updateParticipants() {
        updateStage { stage ->
            val updatedSeats = stage.seats.map { seat ->
                val participant = StageManager.getParticipant(seat.participantId)
                val userAvatar = participant?.userAvatar ?: seat.userAvatar
                val isMuted = participant?.isAudioOff == true
                val isSpeaking = participant?.isSpeaking == true
                seat.copy(
                    userAvatar = userAvatar,
                    isMuted = isMuted,
                    isSpeaking = isSpeaking,
                )
            }
            stage.copy(
                seats = updatedSeats,
                isCreatorVideoOff = StageManager.isCreatorVideoOff,
                isParticipantVideoOff = StageManager.isParticipantVideoOff,
                isCreatorMicOff = StageManager.isCreatorAudioOff,
                isParticipantMicOff = StageManager.isParticipantAudioOff,
            )
        }
    }
}

enum class StageType {
    Video, Audio
}

enum class StageParticipantMode {
    Guest, VS, None
}

data class Stage(
    val stageId: String,
    val isLoading: Boolean = true,
    val isStageCreator: Boolean = false,
    val isStageParticipant: Boolean = false,
    val isFacingSwitched: Boolean = false,
    val isCreatorMicOff: Boolean = false,
    val isCreatorVideoOff: Boolean = false,
    val isParticipantMicOff: Boolean = false,
    val isParticipantVideoOff: Boolean = false,
    val isLocalAudioOff: Boolean = false,
    val isSpeaking: Boolean = false,
    val isVideoStatsEnabled: Boolean = true,
    val type: StageType = StageType.Video,
    val mode: StageParticipantMode = StageParticipantMode.None,
    val creatorAvatar: UserAvatar? = null,
    val selfAvatar: UserAvatar? = null,
    val seats: List<AudioSeat> = emptyList(),
) {
    val isAudioRoom = type == StageType.Audio
    val isVSMode = mode == StageParticipantMode.VS
    val isParticipantJoined = mode != StageParticipantMode.None
    val isJoined = isStageCreator || isStageParticipant
    val isSelfMicOff = isStageCreator && isCreatorMicOff || isStageParticipant && isParticipantMicOff
    val isSelfVideoOff = isStageCreator && isCreatorVideoOff || isStageParticipant && isParticipantVideoOff
    val canJoin = !isAudioRoom && !isJoined && !isParticipantJoined
    val canKick = !isAudioRoom && isStageCreator && isParticipantJoined
    val isMuted = isStageCreator && isCreatorMicOff || isStageParticipant && isParticipantMicOff
}

data class AudioSeat(
    val id: Int,
    val participantId: String? = null,
    val isMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val userAvatar: UserAvatar? = null
) {
    val isEmpty = participantId == null
    val seatId = participantId ?: ""
}
