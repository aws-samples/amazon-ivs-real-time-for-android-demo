package com.amazon.ivs.stagesrealtime.repository.stage

import android.content.Context
import android.view.View
import androidx.datastore.core.DataStore
import com.amazon.ivs.stagesrealtime.common.COLOR_BOTTOM_ATTRIBUTE_NAME
import com.amazon.ivs.stagesrealtime.common.COLOR_LEFT_ATTRIBUTE_NAME
import com.amazon.ivs.stagesrealtime.common.COLOR_RIGHT_ATTRIBUTE_NAME
import com.amazon.ivs.stagesrealtime.common.DEFAULT_COLOR_BOTTOM
import com.amazon.ivs.stagesrealtime.common.DEFAULT_COLOR_LEFT
import com.amazon.ivs.stagesrealtime.common.DEFAULT_COLOR_RIGHT
import com.amazon.ivs.stagesrealtime.common.RMS_SPEAKING_THRESHOLD
import com.amazon.ivs.stagesrealtime.common.extensions.formatString
import com.amazon.ivs.stagesrealtime.repository.models.AppSettings
import com.amazon.ivs.stagesrealtime.repository.models.ParticipantAttributes
import com.amazon.ivs.stagesrealtime.repository.models.RTCLatency
import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageMode
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageType
import com.amazonaws.ivs.broadcast.AudioStageStream
import com.amazonaws.ivs.broadcast.BroadcastConfiguration.AspectMode
import com.amazonaws.ivs.broadcast.BroadcastException
import com.amazonaws.ivs.broadcast.ImageDevice
import com.amazonaws.ivs.broadcast.ParticipantInfo
import com.amazonaws.ivs.broadcast.Stage
import com.amazonaws.ivs.broadcast.StageRenderer
import com.amazonaws.ivs.broadcast.StageStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue

class StageManager(
    private val context: Context,
    appSettingsStore: DataStore<AppSettings>,
    ioScope: CoroutineScope,
) {
    // The currently joined stage
    private val stageStrategy = StageStrategy(context, appSettingsStore) { ttvLatency ->
        onVideoStatsUpdated(ttvLatency)
    }
    private var currentStage: Stage? = null
    private var hostVideoStream: StageStream? = null
    private var hostAudioStream: StageStream? = null
    private var guestVideoStream: StageStream? = null
    private var stageId: String? = null
    private var stageType: StageType? = null

    // Contains all participants except the local participant
    private val joinedParticipants = ConcurrentLinkedQueue<ParticipantAttributes>()
    private val _onEvent = MutableSharedFlow<StageEvent>(replay = 1)
    private var isCreator = false
    private var isParticipant = false
    private var hostParticipantId: String? = null
    private var selfParticipantId: String? = null
    private var lastLatency = RTCLatency()
    private var lastVideoStats = StageEvent.VideoStatsUpdated()

    private var creatorJoinTime = 0L
    private var creatorVideoTime = 0L
    private var guestJoinTime = 0L
    private var guestVideoTime = 0L

    val onEvent = _onEvent.asSharedFlow()
    val isLocalUserSpeaking = stageStrategy.isLocalUserSpeaking
    val rtcData = stageStrategy.rtcData
    val rtcDataList = stageStrategy.rtcDataList

    private val renderer = object : StageRenderer {
        override fun onError(error: BroadcastException) {
            Timber.d("Stage error: $error")
        }

        override fun onConnectionStateChanged(
            stage: Stage,
            state: Stage.ConnectionState,
            exception: BroadcastException?
        ) {
            Timber.d("State changed: $state, error: $exception")
            if (state == Stage.ConnectionState.DISCONNECTED) {
                _onEvent.tryEmit(StageEvent.StageGone)
            }
        }

        override fun onParticipantJoined(stage: Stage, info: ParticipantInfo) {
            if (info.isLocal) return
            Timber.d("Participant joined: ${info.participantId}, ${info.attributes}, $hostParticipantId, $stageId")
            val isHost = hostParticipantId == null && stageId == info.attributes["username"]
            if (isHost) {
                hostParticipantId = info.participantId
                Timber.d("Creator joined: ${info.participantId}, ${info.attributes}, ${info.capabilities}")
            }
            var participantUsername = ""
            val userAvatar = info.attributes?.let { attributes ->
                participantUsername = attributes["username"] ?: ""
                UserAvatar(
                    colorLeft = attributes[COLOR_LEFT_ATTRIBUTE_NAME] ?: DEFAULT_COLOR_LEFT,
                    colorRight = attributes[COLOR_RIGHT_ATTRIBUTE_NAME] ?: DEFAULT_COLOR_RIGHT,
                    colorBottom = attributes[COLOR_BOTTOM_ATTRIBUTE_NAME] ?: DEFAULT_COLOR_BOTTOM,
                )
            } ?: UserAvatar()
            val attributes = ParticipantAttributes(
                stageId = participantUsername,
                participantId = info.participantId,
                isMuted = false,
                userAvatar = userAvatar,
                isHost = isHost
            )
            joinedParticipants.add(attributes)
            stageStrategy.joinedParticipants = joinedParticipants
            stage.refreshStrategy()
            val event = if (isHost) StageEvent.CreatorJoined(
                participantId = info.participantId,
                isAudioOff = false,
                isVideoOff = false,
                userAvatar = userAvatar,
                video = null
            ) else StageEvent.GuestJoined(
                participantId = info.participantId,
                isAudioOff = false,
                isVideoOff = false,
                userAvatar = userAvatar,
                video = null
            )
            _onEvent.tryEmit(event)
        }

        override fun onParticipantLeft(stage: Stage, info: ParticipantInfo) {
            if (info.isLocal) return
            if (joinedParticipants.removeIf { it.participantId == info.participantId }) {
                Timber.d("Participant left: ${info.participantId}, ${info.attributes}")
                val creatorLeft = info.participantId == hostParticipantId
                guestJoinTime = 0L
                guestVideoTime = 0L
                lastVideoStats = lastVideoStats.copy(guestTTV = null, guestLatency = "")
                if (creatorLeft) {
                    hostVideoStream = null
                    hostParticipantId = null
                    creatorJoinTime = 0L
                    creatorVideoTime = 0L
                    lastVideoStats = lastVideoStats.copy(creatorTTV = null, creatorLatency = "")
                    stageStrategy.joinedParticipants = joinedParticipants
                    stage.refreshStrategy()
                }
                _onEvent.tryEmit(if (creatorLeft) StageEvent.CreatorLeft else StageEvent.GuestLeft)
            }
        }

        override fun onParticipantPublishStateChanged(stage: Stage, info: ParticipantInfo, state: Stage.PublishState) {
            if (info.isLocal) return
            Timber.d("Participant: ${info.participantId}, ${info.attributes} publish state changed: $state")

            if (state != Stage.PublishState.PUBLISHED) return
            val isHost = stageId == info.attributes["username"]
            if (isHost) {
                creatorJoinTime = Date().time
                Timber.d("Creator started publishing: ${info.participantId}, ${info.attributes}, $creatorJoinTime, ${info.capabilities}")
            } else {
                guestJoinTime = Date().time
                Timber.d("Guest started publishing: ${info.participantId}, ${info.attributes}, $guestJoinTime, ${info.capabilities}")
            }
        }

        override fun onParticipantSubscribeStateChanged(
            stage: Stage,
            info: ParticipantInfo,
            state: Stage.SubscribeState
        ) {
            if (info.isLocal) return
            Timber.d("Participant: ${info.participantId}, ${info.attributes} subscribe state changed: $state")
        }

        override fun onStreamsAdded(stage: Stage, info: ParticipantInfo, streams: MutableList<StageStream>) {
            if (info.isLocal) return
            var video: View? = null
            var isAudioOff = true
            var isVideoOff = true
            val isHost = info.participantId == hostParticipantId
            Timber.d("Participant streams added: ${info.participantId}, ${info.attributes}, $isHost, ${streams.count()}")
            var participantUsername = ""
            val userAvatar = info.attributes?.let { attributes ->
                participantUsername = attributes["username"] ?: ""
                UserAvatar(
                    colorLeft = attributes[COLOR_LEFT_ATTRIBUTE_NAME] ?: DEFAULT_COLOR_LEFT,
                    colorRight = attributes[COLOR_RIGHT_ATTRIBUTE_NAME] ?: DEFAULT_COLOR_RIGHT,
                    colorBottom = attributes[COLOR_BOTTOM_ATTRIBUTE_NAME] ?: DEFAULT_COLOR_BOTTOM,
                )
            } ?: UserAvatar()
            var videoStream: StageStream? = null
            var audioStream: StageStream? = null
            var speakingJob: Job? = null
            streams.find { it.streamType == StageStream.Type.VIDEO }?.let { stream ->
                isVideoOff = stream.muted
                videoStream = stream
                if (isHost) hostVideoStream = stream else guestVideoStream = stream
                video = stream.getVideoPreview()
                (stream.device as? ImageDevice)?.setOnFrameCallback {
                    if (isHost) {
                        creatorVideoTime = Date().time
                    } else {
                        guestVideoTime = Date().time
                    }
                    val ttv = if (isHost) {
                        if (creatorVideoTime > creatorJoinTime) {
                            val time = (creatorVideoTime - creatorJoinTime) / 1000f
                            formatString("%.2f", time)
                        } else {
                            null
                        }
                    } else {
                        if (guestVideoTime > guestJoinTime) {
                            val time = (guestVideoTime - guestJoinTime) / 1000f
                            formatString("%.2f", time)
                        } else {
                            null
                        }
                    }
                    lastVideoStats = if (isHost) {
                        lastVideoStats.copy(creatorTTV = ttv)
                    } else {
                        lastVideoStats.copy(guestTTV = ttv)
                    }
                    Timber.d("TTV Updated: $ttv, $lastVideoStats")
                    _onEvent.tryEmit(lastVideoStats)
                    (stream.device as? ImageDevice)?.setOnFrameCallback(null)
                }
            }
            streams.find { it.streamType == StageStream.Type.AUDIO }?.let { stream ->
                isAudioOff = stream.muted
                if (isHost) hostAudioStream = stream
                audioStream = stream
                speakingJob = ioScope.launch {
                    (stream as AudioStageStream).setStatsCallback { _, rms ->
                        joinedParticipants.find { it.participantId == info.participantId }?.let { participant ->
                            val isSpeaking = rms >= RMS_SPEAKING_THRESHOLD
                            if (participant.isSpeaking != isSpeaking && !stream.muted) {
                                participant.isSpeaking = isSpeaking
                                _onEvent.tryEmit(StageEvent.GuestSpeakingStateUpdated)
                            }
                        }
                    }
                }
            }
            Timber.d("Participant streams added: ${info.participantId}, ${info.attributes}, $isVideoOff, $isAudioOff, $isHost")
            val attributes = ParticipantAttributes(
                stageId = participantUsername,
                participantId = info.participantId,
                isMuted = isAudioOff,
                userAvatar = userAvatar,
                isHost = isHost,
                audioStream = audioStream,
                videoStream = videoStream,
                speakingJob = speakingJob
            )
            videoStream?.setListener(
                stageStrategy.createRTCStatsListenerObject(
                    participantId= attributes.participantId,
                    stageId = attributes.stageId,
                    forViewer = true
                )
            )
            audioStream?.setListener(
                stageStrategy.createRTCStatsListenerObject(
                    participantId= attributes.participantId,
                    stageId = attributes.stageId,
                    forAudio = true,
                    forViewer = true
                )
            )
            joinedParticipants.removeIf { it.participantId == info.participantId }
            joinedParticipants.add(attributes)
            stageStrategy.joinedParticipants = joinedParticipants
            stage.refreshStrategy()
            val event = if (isHost) StageEvent.CreatorJoined(
                participantId = info.participantId,
                isAudioOff = isAudioOff,
                isVideoOff = isVideoOff,
                userAvatar = userAvatar,
                video = video,
            ) else StageEvent.GuestJoined(
                participantId = info.participantId,
                isAudioOff = isAudioOff,
                isVideoOff = isVideoOff,
                userAvatar = userAvatar,
                video = video,
            )
            _onEvent.tryEmit(event)
        }

        override fun onStreamsRemoved(stage: Stage, info: ParticipantInfo, streams: MutableList<StageStream>) {
            if (info.isLocal) return
            Timber.d("onStreams removed for user ${info.participantId}, ${info.attributes}")
            val isVideoStreamRemoved = streams.any { it.streamType == StageStream.Type.VIDEO }
            if (stageType == StageType.AUDIO && isVideoStreamRemoved) return

            if (joinedParticipants.removeAndCancelJob(info.participantId)) {
                stageStrategy.removeRTCDataById(info.participantId)
                val isHost = info.participantId == hostParticipantId
                if (isHost) {
                    hostVideoStream?.setListener(null)
                    hostVideoStream = null
                } else {
                    guestVideoStream?.setListener(null)
                    guestVideoStream = null
                }
                streams.find { it.streamType == StageStream.Type.AUDIO }?.let { stream ->
                    (stream as AudioStageStream).setStatsCallback(null)
                    if (isHost) {
                        hostAudioStream?.setListener(null)
                        hostAudioStream = null
                    }
                }
                Timber.d("Participant streams removed: ${info.participantId}, ${info.attributes}, $isHost")
                guestJoinTime = 0L
                guestVideoTime = 0L
                lastVideoStats = lastVideoStats.copy(guestTTV = null, guestLatency = "")
                val event = if (isHost) {
                    creatorJoinTime = 0L
                    creatorVideoTime = 0L
                    lastVideoStats = lastVideoStats.copy(creatorTTV = null, creatorLatency = "")
                    StageEvent.CreatorLeft
                } else StageEvent.GuestLeft
                _onEvent.tryEmit(event)
            }
        }

        override fun onStreamsMutedChanged(stage: Stage, info: ParticipantInfo, streams: MutableList<StageStream>) {
            if (info.isLocal) return
            var isVideoOff: Boolean? = null
            val isAudioOff = streams.find { it.streamType == StageStream.Type.AUDIO }?.muted
            val isHost = info.participantId == hostParticipantId
            val video = streams.find { it.streamType == StageStream.Type.VIDEO }?.let { stream ->
                isVideoOff = stream.muted
                if (isHost) hostVideoStream = stream else guestVideoStream = stream
                stream.getVideoPreview()
            }
            joinedParticipants.find { it.participantId == info.participantId }?.isMuted = isAudioOff ?: false
            Timber.d("Participant streams muted: ${info.participantId}, ${info.attributes}, $isVideoOff, $isAudioOff, $isHost")
            val event = if (isHost) StageEvent.CreatorUpdated(
                participantId = info.participantId,
                isAudioOff = isAudioOff,
                isVideoOff = isVideoOff,
                video = video,
            ) else {
                StageEvent.GuestUpdated(
                    participantId = info.participantId,
                    isAudioOff = isAudioOff,
                    isVideoOff = isVideoOff,
                    video = video,
                )
            }
            _onEvent.tryEmit(event)
        }
    }

    suspend fun joinStage(
        stageId: String,
        token: String,
        type: StageType,
        isCreator: Boolean,
        selfParticipantId: String,
        hostParticipantId: String? = null
    ) {
        this.isCreator = isCreator
        this.hostParticipantId = hostParticipantId
        this.selfParticipantId = selfParticipantId
        this.isParticipant = false
        this.stageId = stageId
        this.stageType = type
        this.hostVideoStream = null
        this.hostAudioStream = null
        this.guestVideoStream = null
        joinedParticipants.clear()
        stageStrategy.joinedParticipants = this.joinedParticipants
        stageStrategy.setup(
            type = type,
            isParticipating = isCreator,
            isCreator = isCreator,
            stageId = stageId,
            participantId = selfParticipantId
        )
        currentStage?.leave()
        currentStage = null
        currentStage = Stage(context, token, stageStrategy.strategy)
        currentStage?.join()
        Timber.d("Joined new stage: $stageId, $type, $isCreator")
    }

    fun isCurrentStageVideo() = stageType == StageType.VIDEO

    fun observeStage() {
        currentStage?.removeRenderer(renderer)
        currentStage?.addRenderer(renderer)
        currentStage?.refresh()
    }

    suspend fun startPublishing(type: StageType, mode: StageMode) {
        isParticipant = true
        stageStrategy.setup(
            type = type,
            mode = mode,
            isParticipating = true,
            isCreator = false,
            stageId = this.stageId,
            participantId = this.selfParticipantId,
        )
        currentStage?.refresh()
    }

    fun stopPublishing() {
        isParticipant = false
        stageStrategy.removeStreams()
        currentStage?.refresh()
    }

    fun leaveStage() {
        isCreator = false
        isParticipant = false
        joinedParticipants.forEach { it.speakingJob?.cancel() }
        joinedParticipants.clear()
        stageId = null
        stageType = null
        hostParticipantId = null
        hostVideoStream?.setListener(null)
        hostVideoStream = null
        hostAudioStream?.setListener(null)
        hostAudioStream = null
        guestVideoStream?.setListener(null)
        guestVideoStream = null
        stageStrategy.dispose()
        currentStage?.refreshStrategy()
        currentStage?.removeRenderer(renderer)
        currentStage?.leave()
        guestJoinTime = 0L
        guestVideoTime = 0L
        creatorJoinTime = 0L
        creatorVideoTime = 0L
        lastVideoStats = StageEvent.VideoStatsUpdated()
        _onEvent.tryEmit(lastVideoStats)
        Timber.d("Stage manager cleared")
    }

    fun switchAudio() {
        stageStrategy.switchAudio()
        currentStage?.refresh()
    }

    fun switchVideo() {
        stageStrategy.switchVideo()
        currentStage?.refresh()
    }

    fun isLocalAudioOff() = stageStrategy.isAudioOff

    suspend fun switchFacing() {
        stageStrategy.switchFacing()
        currentStage?.refresh()
    }

    fun isStageCreator() = isCreator

    fun isParticipating() = isCreator || isParticipant

    fun getCreatorVideo() = if (isCreator) {
        stageStrategy.refreshVideoPreview()
    } else {
        hostVideoStream.getVideoPreview()
    }

    fun getGuestVideo() = if (isCreator) {
        guestVideoStream.getVideoPreview()
    } else {
        stageStrategy.refreshVideoPreview()
    }

    fun removeParticipantsNotInSeats(seats: List<String>) {
        val participantsToRemove = mutableListOf<String>()
        joinedParticipants.forEach { participant ->
            if (!seats.contains(participant.participantId)) {
                participantsToRemove.add(participant.participantId)
            }
        }

        // Separate list and iteration to prevent updating concurrent update of joinedParticipants list while iterating through it
        participantsToRemove.forEach { id ->
            Timber.d("Removing joined participant not in seats $id")
            joinedParticipants.removeAndCancelJob(id)
            stageStrategy.removeRTCDataById(id)
        }
    }

    fun getGuestId() = joinedParticipants.find { it.participantId != hostParticipantId }?.stageId

    suspend fun updateMode(type: StageType, mode: StageMode) {
        if (stageStrategy.currentMode == mode) return
        Timber.d("Update mode: ${stageStrategy.currentMode}, $mode")
        stageStrategy.setup(
            type = type,
            mode = mode,
            isCreator = isCreator,
            isParticipating = isParticipating(),
            stageId = this.stageId,
            participantId = this.selfParticipantId
        )
        currentStage?.refresh()
    }

    fun getParticipantAttributes(participantId: String) = joinedParticipants.find { it.participantId == participantId }

    fun getParticipantAvatar(stageId: String?) = joinedParticipants.find { it.stageId == stageId }?.userAvatar

    fun requestRTCStats() {
        if (stageType == null) return
        Timber.d("Requesting stats for: $stageType")
        if (stageType == StageType.VIDEO) {
            requestRTCStatsForVideo()
        } else {
            requestRTCStatsForAudio()
        }
    }

    private fun requestRTCStatsForVideo() {
        if (isParticipating()) {
            stageStrategy.requestRTCStatsForVideoStream()
        }
        joinedParticipants.forEach { it.videoStream?.requestRTCStats() }
    }

    private fun requestRTCStatsForAudio() {
        if (isParticipating()) {
            stageStrategy.requestRTCStatsForAudioStream()
        } else {
            joinedParticipants.forEach { it.audioStream?.requestRTCStats() }
        }
    }

    private fun StageStream?.getVideoPreview() = try {
        (this?.device as? ImageDevice)?.getPreviewView(AspectMode.FILL)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get video preview")
        null
    }

    private fun Stage.refresh() {
        refreshStrategy()
        _onEvent.tryEmit(
            StageEvent.LocalParticipantUpdated(
                participantId = hostParticipantId,
                isCreator = isCreator,
                isParticipant = isParticipant,
                isAudioOff = stageStrategy.isAudioOff,
                isVideoOff = stageStrategy.isVideoOff,
                isFacingBack = stageStrategy.isFacingBack,
                video = stageStrategy.preview
            )
        )
    }

    private fun ConcurrentLinkedQueue<ParticipantAttributes>.removeAndCancelJob(participantId: String): Boolean {
        this.find { it.participantId == participantId }?.let { participant ->
            participant.speakingJob?.cancel()
            this.removeAll { it.participantId == participantId }
            return true
        } ?: run { return false }
    }

    private fun onVideoStatsUpdated(rtcLatency: RTCLatency) {
        if (lastLatency == rtcLatency) return
        lastLatency = rtcLatency
        val isHost = rtcLatency.isHost
        lastVideoStats = if (isHost) {
            lastVideoStats.copy(creatorLatency = rtcLatency.value)
        } else {
            lastVideoStats.copy(guestLatency = rtcLatency.value)
        }
        Timber.d("Latency updated: $rtcLatency, $lastVideoStats")
        _onEvent.tryEmit(lastVideoStats)
    }
}
