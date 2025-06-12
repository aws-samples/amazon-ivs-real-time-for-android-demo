package com.amazon.ivs.stagesrealtimecompose.core.handlers.stage

import com.amazon.ivs.stagesrealtimecompose.appContext
import com.amazon.ivs.stagesrealtimecompose.core.common.RMS_SPEAKING_THRESHOLD
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.asObject
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.formatString
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.getUserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.getUserName
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.launchDefault
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.launchMain
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.updateList
import com.amazon.ivs.stagesrealtimecompose.core.handlers.PreferencesHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserAvatar
import com.amazonaws.ivs.broadcast.AudioLocalStageStream
import com.amazonaws.ivs.broadcast.BroadcastConfiguration.AspectMode
import com.amazonaws.ivs.broadcast.BroadcastException
import com.amazonaws.ivs.broadcast.Device
import com.amazonaws.ivs.broadcast.Device.Descriptor.DeviceType
import com.amazonaws.ivs.broadcast.Device.Descriptor.Position
import com.amazonaws.ivs.broadcast.DeviceDiscovery
import com.amazonaws.ivs.broadcast.ImageDevice
import com.amazonaws.ivs.broadcast.ImageLocalStageStream
import com.amazonaws.ivs.broadcast.LocalAudioStats
import com.amazonaws.ivs.broadcast.LocalStageStream
import com.amazonaws.ivs.broadcast.LocalVideoStats
import com.amazonaws.ivs.broadcast.ParticipantInfo
import com.amazonaws.ivs.broadcast.RemoteAudioStats
import com.amazonaws.ivs.broadcast.RemoteVideoStats
import com.amazonaws.ivs.broadcast.Stage
import com.amazonaws.ivs.broadcast.Stage.Strategy
import com.amazonaws.ivs.broadcast.Stage.SubscribeType
import com.amazonaws.ivs.broadcast.StageRenderer
import com.amazonaws.ivs.broadcast.StageStream
import com.amazonaws.ivs.broadcast.StageVideoConfiguration
import com.amazonaws.ivs.broadcast.SurfaceSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import org.json.JSONObject
import timber.log.Timber
import java.util.Date
import kotlin.math.roundToInt
import kotlin.random.Random

object StageManager {
    private val deviceDiscovery by lazy { DeviceDiscovery(appContext) }
    private val _participants = MutableStateFlow(emptyList<Participant>())
    private val _activeCreatorStream = MutableStateFlow(ActiveVideoStream())
    private val _activeParticipantStream = MutableStateFlow(ActiveVideoStream())
    private val _selfStreams = mutableListOf<LocalStageStream>()
    private val _rtcStats = MutableStateFlow(RTCStats())
    private val _rtcData = MutableStateFlow(RTCData())
    private val _rtcDataList = MutableStateFlow(emptyList<RTCData>())
    private var _selfVideoStream: ImageLocalStageStream? = null
    private var _selfAudioStream: AudioLocalStageStream? = null
    private var _creatorVideoStream: StageStream? = null
    private var _creatorAudioStream: StageStream? = null
    private var _participantVideoStream: StageStream? = null
    private var _participantAudioStream: StageStream? = null
    private var _currentFacing = Position.FRONT
    private var _subscribeType = SubscribeType.NONE
    private var _stage: Stage? = null
    private var _rtcStatsJob: Job? = null

    private var creatorJoinTime = 0L
    private var creatorVideoTime = 0L
    private var guestJoinTime = 0L
    private var guestVideoTime = 0L

    val participants = _participants.asStateFlow()
    val activeCreatorStream = _activeCreatorStream.asStateFlow()
    val activeParticipantStream = _activeParticipantStream.asStateFlow()
    val rtcStats = _rtcStats.asStateFlow()
    val rtcData = _rtcData.asStateFlow()
    val rtcDataList = _rtcDataList.asStateFlow()

    private val creatorStream get() = if (StageHandler.currentStage?.isStageCreator == true) {
        _selfVideoStream
    } else {
        _creatorVideoStream
    }

    private val participantStream get() = if (StageHandler.currentStage?.isStageParticipant == true) {
        _selfVideoStream
    } else {
        _participantVideoStream
    }

    val isCreatorVideoOff get() = if (StageHandler.currentStage?.isStageCreator == true) {
        _selfVideoStream?.muted == true
    } else {
        _creatorVideoStream?.muted == true
    }

    val isParticipantVideoOff get() = if (StageHandler.currentStage?.isStageParticipant == true) {
        _selfVideoStream?.muted == true
    } else {
        _participantVideoStream?.muted == true
    }

    val isCreatorAudioOff get() = if (StageHandler.currentStage?.isStageCreator == true) {
        _selfAudioStream?.muted == true
    } else {
        _creatorAudioStream?.muted == true
    }

    val isParticipantAudioOff get() = if (StageHandler.currentStage?.isStageParticipant == true) {
        _selfAudioStream?.muted == true
    } else {
        _participantAudioStream?.muted == true
    }

    private val stageStrategy = object : Strategy {
        override fun stageStreamsToPublishForParticipant(stage: Stage, info: ParticipantInfo) = _selfStreams
        override fun shouldPublishFromParticipant(stage: Stage, info: ParticipantInfo) =
            StageHandler.currentStage?.isJoined == true
        override fun shouldSubscribeToParticipant(stage: Stage, info: ParticipantInfo) = _subscribeType
    }

    private val stageRenderer = object : StageRenderer {
        override fun onConnectionStateChanged(
            stage: Stage,
            state: Stage.ConnectionState,
            exception: BroadcastException?
        ) {
            super.onConnectionStateChanged(stage, state, exception)
            Timber.d("Stage state changed: $state, error: $exception")
            if (state == Stage.ConnectionState.DISCONNECTED) {
                StageHandler.onStageGone()
            }
        }

        override fun onParticipantJoined(stage: Stage, info: ParticipantInfo) {
            super.onParticipantJoined(stage, info)
            val userName = info.attributes.getUserName()
            val userAvatar = info.attributes.getUserAvatar()
            val participant = Participant(
                participantId = info.participantId,
                userName = userName,
                userAvatar = userAvatar,
                isLocal = info.isLocal,
            )
            Timber.d("Participant joined: $participant")

            insertParticipant(participant = participant)
            stage.refreshStrategy()
        }

        override fun onParticipantLeft(stage: Stage, info: ParticipantInfo) {
            super.onParticipantLeft(stage, info)
            val participantId = info.participantId
            if (_participants.value.any { it.participantId == participantId }) {
                val participantToRemove = _participants.value.first { it.participantId == participantId }
                Timber.d("Participant left: $participantToRemove")
                val participants = _participants.value.filter { it.participantId != participantId }
                _participants.update { participants }
                _rtcDataList.updateList {
                    val isRemoved = removeIf { it.participantId == participantId }
                    Timber.d("RTC data removed: $isRemoved, for: $participantId")
                }
                if (info.attributes.getUserName() == StageHandler.currentStage?.stageId) {
                    Timber.d("Stage creator left the stage")
                    StageHandler.onStageGone()
                }
                refreshVideoStreams()
            }
        }

        override fun onParticipantPublishStateChanged(
            stage: Stage,
            info: ParticipantInfo,
            publishState: Stage.PublishState
        ) {
            super.onParticipantPublishStateChanged(stage, info, publishState)
            Timber.d("Participant publish state changed: ${info.toActualString()}, $publishState")
            val stage = StageHandler.currentStage ?: return
            if (publishState != Stage.PublishState.PUBLISHED) return
            if (stage.stageId == info.attributes.getUserName()) {
                creatorJoinTime = Date().time
                Timber.d("Creator started publishing")
            } else {
                guestJoinTime = Date().time
                Timber.d("Participant started publishing")
            }
            refreshVideoStreams()
        }

        override fun onStreamsAdded(stage: Stage, info: ParticipantInfo, streams: List<StageStream?>) {
            super.onStreamsAdded(stage, info, streams)
            Timber.d("Streams added: ${streams.count()} for: ${info.toActualString()}, username: ${info.attributes.getUserName()}, stage id: ${StageHandler.currentStage?.stageId}")
            if (info.isLocal) return
            val stage = StageHandler.currentStage ?: return

            val isCreator = stage.stageId == info.attributes.getUserName()
            var isVideoOff = true
            var isAudioOff = true

            streams.find { it?.streamType == StageStream.Type.VIDEO }?.let { stream ->
                isVideoOff = stream.muted
                if (isCreator) {
                    _creatorVideoStream = stream
                } else {
                    _participantVideoStream = stream
                }
                (stream.device as? ImageDevice)?.setOnFrameCallback {
                    if (isCreator) {
                        creatorVideoTime = Date().time
                    } else {
                        guestVideoTime = Date().time
                    }
                    val ttv = if (isCreator) {
                        if (creatorVideoTime > creatorJoinTime) {
                            val time = (creatorVideoTime - creatorJoinTime) / 1000f
                            formatString("%.2f", time)
                        } else {
                            ""
                        }
                    } else {
                        if (guestVideoTime > guestJoinTime) {
                            val time = (guestVideoTime - guestJoinTime) / 1000f
                            formatString("%.2f", time)
                        } else {
                            ""
                        }
                    }
                    val lastVideoStats = _rtcStats.value.copy(
                        creatorTTV = if (isCreator) ttv else _rtcStats.value.creatorTTV,
                        participantTTV = if (!isCreator) ttv else _rtcStats.value.participantTTV,
                    )
                    Timber.d("TTV Updated: $ttv, $lastVideoStats")
                    _rtcStats.update { lastVideoStats }
                    (stream.device as? ImageDevice)?.setOnFrameCallback(null)
                }
                stream.setListener(createRTCStatsListenerObject(
                    stageId = stage.stageId,
                    participantId = info.participantId,
                    forAudio = false,
                    forViewer = true,
                ))
            }
            streams.find { it?.streamType == StageStream.Type.AUDIO }?.let { stream ->
                isAudioOff = stream.muted
                if (isCreator) {
                    _creatorAudioStream = stream
                } else {
                    _participantAudioStream = stream
                }
                (stream as? AudioLocalStageStream)?.setStatsCallback { _, rms ->
                    updateParticipant(participantId = info.participantId) { participant ->
                        participant.copy(isSpeaking = rms > RMS_SPEAKING_THRESHOLD)
                    }
                }
                stream.setListener(createRTCStatsListenerObject(
                    stageId = stage.stageId,
                    participantId = info.participantId,
                    forAudio = true,
                    forViewer = true,
                ))
            }
            _stage?.refreshStrategy()

            Timber.d("Streams added: ${streams.count()} for: ${info.toActualString()}, $isCreator, $isVideoOff, $isAudioOff")
            val participants = _participants.value.map { participant ->
                if (participant.participantId == info.participantId) {
                    participant.copy(
                        userName = info.attributes.getUserName(participant.userName),
                        userAvatar = info.attributes.getUserAvatar(),
                        isVideoOff = isVideoOff,
                        isAudioOff = isAudioOff,
                    )
                } else {
                    participant.copy()
                }
            }
            _participants.update { participants }
            refreshVideoStreams()
        }

        override fun onStreamsRemoved(stage: Stage, info: ParticipantInfo, streams: List<StageStream?>) {
            super.onStreamsRemoved(stage, info, streams)
            Timber.d("Streams removed: ${streams.count()} for: ${info.toActualString()}")
            if (info.isLocal) return

            val isVideoStreamRemoved = streams.any { it?.streamType == StageStream.Type.VIDEO }
            val isAudioStreamRemoved = streams.any { it?.streamType == StageStream.Type.AUDIO }
            val isCreator = StageHandler.currentStage?.stageId == info.attributes.getUserName()
            Timber.d("Streams removed for: ${info.toActualString()}, isVideo: $isVideoStreamRemoved, isAudio: $isAudioStreamRemoved, isCreator: $isCreator")
            if (isVideoStreamRemoved) {
                refreshVideoStreams()
            }
        }

        override fun onStreamsMutedChanged(stage: Stage, info: ParticipantInfo, streams: List<StageStream?>) {
            super.onStreamsMutedChanged(stage, info, streams)
            Timber.d("Streams muted: ${streams.count()} for: ${info.toActualString()}")
            if (info.isLocal) return
            val stage = StageHandler.currentStage ?: return

            val isCreator = stage.stageId == info.attributes.getUserName()
            val isVideoOff = streams.find { it?.streamType == StageStream.Type.VIDEO }?.muted != false
            val isAudioOff = streams.find { it?.streamType == StageStream.Type.AUDIO }?.muted != false
            Timber.d("Streams muted: ${streams.count()} for: ${info.toActualString()}, $isCreator, $isVideoOff, $isAudioOff")
            val participants = _participants.value.map { participant ->
                if (participant.participantId == info.participantId) {
                    participant.copy(
                        userName = info.attributes.getUserName(participant.userName),
                        userAvatar = info.attributes.getUserAvatar(),
                        isVideoOff = isVideoOff,
                        isAudioOff = isAudioOff,
                    )
                } else {
                    participant.copy()
                }
            }
            _participants.update { participants }
            refreshVideoStreams()
        }
    }

    init {
        deviceDiscovery.addOnDevicesChangedListener(object : DeviceDiscovery.OnDevicesChangedListener {
            override fun onDevicesAdded(discovery: DeviceDiscovery, devices: MutableCollection<Device>) {
                Timber.d("Devices added: $devices")
            }
            override fun onDevicesRemoved(discovery: DeviceDiscovery, devices: MutableCollection<Device>) {
                Timber.d("Devices removed: $devices")
            }
        })
    }

    fun requestRTCStats() {
        _rtcStatsJob = launchDefault {
            StageHandler.currentStage?.let { stage ->
                _selfAudioStream?.requestRTCStats()
                _participantAudioStream?.requestRTCStats()
                _creatorAudioStream?.requestRTCStats()
                if (!stage.isAudioRoom) {
                    _selfVideoStream?.requestRTCStats()
                    _participantVideoStream?.requestRTCStats()
                    _creatorVideoStream?.requestRTCStats()
                }
            }
            delay(1000)
            requestRTCStats()
        }
    }

    fun joinStage(token: String) {
        _stage?.removeRenderer(stageRenderer)
        _stage?.leave()

        _stage = Stage(appContext, token, stageStrategy)
        _stage?.refreshStrategy()
        _stage?.addRenderer(stageRenderer)
        _stage?.join()

        _rtcStatsJob?.cancel()
        requestRTCStats()
        Timber.d("Stage joined: $token")
    }

    fun dispose() {
        Timber.d("Disposing stage manager")
        _rtcStatsJob?.cancel()
        _rtcStatsJob = null
        _stage?.removeRenderer(stageRenderer)
        _stage?.leave()
        _stage = null
    }

    fun switchMic(): Boolean {
        val isMuted = _selfAudioStream?.muted == true
        _selfAudioStream?.muted = !isMuted
        Timber.d("Mic is muted: ${!isMuted}")
        updateLocalParticipant { participant ->
            participant.copy(isAudioOff = !isMuted)
        }
        _stage?.refreshStrategy()
        return !isMuted
    }

    fun switchCamera(): Boolean {
        val isMuted = _selfVideoStream?.muted == true
        _selfVideoStream?.muted = !isMuted
        Timber.d("Camera is muted: ${!isMuted}")
        updateLocalParticipant { participant ->
            participant.copy(isVideoOff = !isMuted)
        }
        refreshVideoStreams()
        return !isMuted
    }

    fun switchFacing(): Boolean {
        _selfVideoStream?.run {
            val isMuted = muted
            _selfStreams.remove(this)
            setListener(null)
            _currentFacing = if (_currentFacing == Position.FRONT) {
                Position.BACK
            } else {
                Position.FRONT
            }
            setCameraStream(facing = _currentFacing, isMuted = isMuted)
            refreshVideoStreams()
            return _currentFacing == Position.BACK
        }
        return false
    }

    fun stopPublishing() {
        _selfStreams.clear()
        _selfAudioStream?.setStatsCallback(null)
        _selfAudioStream?.setListener(null)
        _selfAudioStream = null
        _selfVideoStream?.setListener(null)
        _selfVideoStream = null
        Timber.d("Publishing stopped")
        refreshVideoStreams()
    }

    fun subscribeToStage(mode: StageParticipantMode) {
        val stage = StageHandler.currentStage ?: return
        _subscribeType = when (stage.type) {
            StageType.Audio -> SubscribeType.AUDIO_ONLY
            StageType.Video -> SubscribeType.AUDIO_VIDEO
        }
        Timber.d("Subscribing to stream: $mode, type: $_subscribeType")
        refreshVideoStreams()
    }

    fun startPublishing() {
        if (_selfStreams.isNotEmpty()) return
        val stage = StageHandler.currentStage ?: return
        val isAudioOff = _selfAudioStream?.muted == true
        val isVideoOff = _selfVideoStream?.muted == true
        val participantId = StageHandler.selfParticipantId
        _rtcData.update { RTCData() }

        Timber.d("Starting publishing: Audio off: $isAudioOff, Video off: $isVideoOff, Type: ${stage.type}")
        getDevice(type = DeviceType.MICROPHONE)?.let { device ->
            val audioDevice = AudioLocalStageStream(device)
            audioDevice.muted = isAudioOff
            audioDevice.setStatsCallback { _, rms ->
                updateLocalParticipant { participant ->
                    participant.copy(isSpeaking = rms > RMS_SPEAKING_THRESHOLD)
                }
            }
            if (stage.isAudioRoom && participantId != null) {
                Timber.d("Setting RTC listener for audio stream")
                audioDevice.setListener(createRTCStatsListenerObject(
                    stageId = stage.stageId,
                    participantId = participantId,
                    forAudio = true
                ))
            }
            _selfAudioStream = audioDevice
            _selfStreams.add(audioDevice)
        }
        if (!stage.isAudioRoom) {
            setCameraStream(facing = _currentFacing, isMuted = isVideoOff)
            refreshVideoStreams()
        } else {
            _stage?.refreshStrategy()
        }
    }

    fun getParticipant(participantId: String?) = if (participantId == null) {
        null
    } else {
        _participants.value.find { it.participantId == participantId }
    }

    fun getParticipantId(userName: String?) = if (userName == null) {
        null
    } else {
        _participants.value.find { it.userName == userName }?.participantId
    }

    fun getParticipantName(isCreator: Boolean, stageId: String) = if (isCreator) {
        stageId
    } else {
        _participants.value.find { it.userName != stageId }?.userName
    }

    fun getParticipantAvatar(isCreator: Boolean, stageId: String) = if (isCreator) {
        _participants.value.find { it.userName == stageId }?.userAvatar
    } else {
        _participants.value.find { it.userName != stageId }?.userAvatar
    }

    fun createRTCStatsListenerObject(
        participantId: String? = null,
        stageId: String? = null,
        forAudio: Boolean = false,
        forViewer: Boolean = false
    ) = object : StageStream.Listener {
        override fun onMutedChanged(state: Boolean) { /* Ignored */ }
        override fun onLocalAudioStats(stats: LocalAudioStats) { /* Ignored */ }
        override fun onLocalVideoStats(stats: MutableList<LocalVideoStats>) { /* Ignored */ }
        override fun onRemoteAudioStats(stats: RemoteAudioStats) { /* Ignored */ }
        override fun onRemoteVideoStats(stats: RemoteVideoStats) { /* Ignored */ }

        override fun onRTCStats(stats: MutableMap<String, MutableMap<String, String>>) {
            val participant = _participants.value.find { it.participantId == participantId } ?: return
            val isCreator = participant.userName == stageId
            var rtcData = parseRTCData(stats = stats, isForViewer = forViewer, isCreator = isCreator)
            if (!forViewer) {
                _rtcData.update { rtcData }
            } else if (participantId != null) {
                _rtcDataList.updateList {
                    val index = this.indexOfFirst { it.participantId == participantId }
                    rtcData = RTCData(
                        latency = rtcData.latency,
                        fps = if (forAudio) null else rtcData.fps,
                        packetLoss = rtcData.packetLoss,
                        userName = stageId,
                        participantId = participantId,
                        isCreator = isCreator,
                        isParticipant = !isCreator && _subscribeType != SubscribeType.AUDIO_ONLY
                    )
                    if (index != -1) {
                        set(index, rtcData)
                    } else {
                        add(rtcData)
                    }
                }
            }
        }
    }

    private fun refreshVideoStreams() = launchMain {
        val stage = StageHandler.currentStage
        val creatorId = Random.nextInt()
        val participantId = Random.nextInt()
        Timber.d("Refreshing video streams, isCreator: ${stage?.isStageCreator}, isParticipant: ${stage?.isStageParticipant}")
        Timber.d("Is creator off: $isCreatorVideoOff, ${_activeCreatorStream.value.id}, $creatorId, $creatorStream")
        Timber.d("Is participant off: $isParticipantVideoOff, ${_activeParticipantStream.value.id}, $participantId, $participantStream")
        _activeCreatorStream.update {
            ActiveVideoStream(
                id = creatorId,
                stream = creatorStream,
                isOff = isCreatorVideoOff,
            )
        }
        _activeParticipantStream.update {
            ActiveVideoStream(
                id = participantId,
                stream = participantStream,
                isOff = isParticipantVideoOff,
            )
        }
        _stage?.refreshStrategy()
    }

    private fun setCameraStream(facing: Position, isMuted: Boolean) {
        getDevice(type = DeviceType.CAMERA, position = facing)?.let { device ->
            val stageId = StageHandler.currentStage?.stageId
            val participantId = StageHandler.selfParticipantId
            val videoDevice = ImageLocalStageStream(device, StageVideoConfiguration().apply {
                val bitrate = PreferencesHandler.bitrate
                val isSimulcastEnabled = PreferencesHandler.simulcastEnabled
                Timber.d("Max bitrate set: $bitrate")
                simulcast.isEnabled = isSimulcastEnabled

                // Enabling simulcast means the bitrate is automatically managed
                if (!isSimulcastEnabled) {
                    maxBitrate = bitrate
                }
            })
            videoDevice.muted = isMuted
            if (stageId != null && participantId != null) {
                Timber.d("Setting RTC listener for video stream")
                videoDevice.setListener(
                    createRTCStatsListenerObject(
                        stageId = stageId,
                        participantId = participantId
                    )
                )
            }
            _selfVideoStream = videoDevice
            _selfStreams.add(videoDevice)
            Timber.d("Self video updated, facing: $facing, is muted: $isMuted")
        }
    }

    private fun insertParticipant(participant: Participant) {
        val current = _participants.value
        val participants = if (current.any { it.participantId == participant.participantId }) {
            current.map {
                if (it.participantId == participant.participantId) {
                    it.copy(
                        userAvatar = participant.userAvatar,
                        userName = participant.userName,
                    )
                } else {
                    it.copy()
                }
            }
        } else {
            current.toMutableList().apply {
                add(participant)
            }
        }
        Timber.d("Participants updated: ${participants.map { it.userName }}")
        _participants.update { participants }
    }

    private fun updateLocalParticipant(delegate: (Participant) -> Participant) {
        val participants = _participants.value.map { participant ->
            if (participant.isLocal) {
                delegate(participant.copy())
            } else {
                participant.copy()
            }
        }
        _participants.update { participants }
    }

    private fun updateParticipant(participantId: String, delegate: (Participant) -> Participant) {
        val participants = _participants.value.map { participant ->
            if (participant.participantId == participantId) {
                delegate(participant.copy())
            } else {
                participant.copy()
            }
        }
        _participants.update { participants }
    }

    private fun getDevice(type: DeviceType, position: Position? = null): Device? {
        val devices: List<Device> = deviceDiscovery.listLocalDevices().sortedBy { it.descriptor.deviceId }
        return devices.find { it.descriptor.type == type && (position == null || it.descriptor.position == position) }
    }

    private fun parseRTCData(
        stats: Map<String, Map<String, String>>?,
        isForViewer: Boolean = false,
        isCreator: Boolean = false
    ): RTCData {
        try {
            var outbound: RTCBoundData? = null
            var inbound: RTCBoundData? = null
            var remoteInbound: RTCBoundData? = null
            var candidatePair: RTCBoundData? = null
            stats?.forEach { stat ->
                val boundData = stat.value.asObject<RTCBoundData>()
                when {
                    boundData.isOutbound && outbound == null -> outbound = boundData
                    boundData.isInbound && inbound == null -> inbound = boundData
                    boundData.isRemoteInbound && remoteInbound == null -> remoteInbound = boundData
                    boundData.isCandidatePair && candidatePair == null -> candidatePair = boundData
                }
            }

            val streamQuality: RTCData.StreamQuality? = outbound?.qualityLimitationReason?.let { reason ->
                if (reason == "none") RTCData.StreamQuality.Normal else RTCData.StreamQuality.Limited
            }

            // Workaround, because of the non-consistency of data format in raw RTC stats map
            val qualityDurations = outbound?.qualityLimitationDurations?.let { JSONObject(it) }
                ?.toString()
                ?.asObject<QualityLimitationDurationData>()

            val latency = candidatePair?.currentRoundTripTime?.times(1000)
            val fps = if (isForViewer) inbound?.framesPerSecond else outbound?.framesPerSecond
            val packetLost = if (isForViewer) inbound?.packetsLost else remoteInbound?.packetsLost
            latency?.roundToInt()?.toString()?.let { value ->
                _rtcStats.update { stats ->
                    if (isCreator) {
                        stats.copy(creatorLatency = value)
                    } else {
                        stats.copy(participantLatency = value)
                    }
                }
            }

            return RTCData(
                streamQuality = streamQuality,
                cpuLimitedTime = qualityDurations?.cpu,
                networkLimitedTime = qualityDurations?.bandwidth,
                latency = latency?.toString(),
                fps = fps,
                packetLoss = packetLost,
                rawRTCStats = stats?.toMap()?.let { JSONObject(it).toString() }
            )
        } catch (e: Exception) {
            Timber.d(e, "Failed to parse RTC data")
            return RTCData()
        }
    }
}

data class Participant(
    val participantId: String,
    val userName: String,
    val userAvatar: UserAvatar,
    val isLocal: Boolean = false,
    val isAudioOff: Boolean = false,
    val isVideoOff: Boolean = false,
    val isSpeaking: Boolean = false,
)

@Serializable
data class RTCBoundData(
    val packetsLost: String? = null,
    val framesPerSecond: String? = null,
    val qualityLimitationReason: String? = null,
    val qualityLimitationDurations: String? = null,
    val type: String? = null,
    val currentRoundTripTime: Float? = null,
    val id: String? = null
) {
    val isOutbound = type == "outbound-rtp"
    val isInbound = type == "inbound-rtp"
    val isRemoteInbound = type == "remote-inbound-rtp"
    val isCandidatePair = type == "candidate-pair"
}

@Serializable
data class QualityLimitationDurationData(
    val cpu: String? = null,
    val bandwidth: String? = null
)

data class RTCData(
    val streamQuality: StreamQuality? = null,
    val cpuLimitedTime: String? = null,
    val networkLimitedTime: String? = null,
    val latency: String? = null,
    val fps: String? = null,
    val packetLoss: String? = null,
    val rawRTCStats: String? = null,
    val userName: String? = null,
    val participantId: String? = null,
    val isCreator: Boolean = false,
    val isParticipant: Boolean = false
) {
    enum class StreamQuality { Normal, Limited }
}

data class RTCStats(
    val creatorTTV: String = "",
    val creatorLatency: String = "",
    val participantTTV: String = "",
    val participantLatency: String = "",
)

data class ActiveVideoStream(
    val id: Int = 0,
    val stream: StageStream? = null,
    val isOff: Boolean = false,
) {
    val video get() = try {
        (stream?.device as? SurfaceSource)?.getPreviewView(AspectMode.FILL)
    } catch (_: Exception) {
        null
    }
}
