package com.amazon.ivs.stagesrealtime.repository.stage

import android.content.Context
import androidx.datastore.core.DataStore
import com.amazon.ivs.stagesrealtime.common.RMS_SPEAKING_THRESHOLD
import com.amazon.ivs.stagesrealtime.common.extensions.asObject
import com.amazon.ivs.stagesrealtime.common.extensions.setVisibleOr
import com.amazon.ivs.stagesrealtime.common.extensions.updateList
import com.amazon.ivs.stagesrealtime.repository.models.AppSettings
import com.amazon.ivs.stagesrealtime.repository.models.ParticipantAttributes
import com.amazon.ivs.stagesrealtime.repository.models.QualityLimitationDurationData
import com.amazon.ivs.stagesrealtime.repository.models.RTCBoundData
import com.amazon.ivs.stagesrealtime.repository.models.RTCData
import com.amazon.ivs.stagesrealtime.repository.models.RTCLatency
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageMode
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageType
import com.amazonaws.ivs.broadcast.AudioLocalStageStream
import com.amazonaws.ivs.broadcast.BroadcastConfiguration
import com.amazonaws.ivs.broadcast.Device
import com.amazonaws.ivs.broadcast.Device.Descriptor.DeviceType
import com.amazonaws.ivs.broadcast.Device.Descriptor.Position
import com.amazonaws.ivs.broadcast.DeviceDiscovery
import com.amazonaws.ivs.broadcast.ImageLocalStageStream
import com.amazonaws.ivs.broadcast.ImagePreviewView
import com.amazonaws.ivs.broadcast.LocalStageStream
import com.amazonaws.ivs.broadcast.ParticipantInfo
import com.amazonaws.ivs.broadcast.Stage
import com.amazonaws.ivs.broadcast.Stage.Strategy
import com.amazonaws.ivs.broadcast.Stage.SubscribeType
import com.amazonaws.ivs.broadcast.StageStream
import com.amazonaws.ivs.broadcast.StageVideoConfiguration
import com.amazonaws.ivs.broadcast.SurfaceSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt

class StageStrategy(
    context: Context,
    private val appSettingsStore: DataStore<AppSettings>,
    private val onVideoStatsUpdated: (RTCLatency) -> Unit
) {
    private val deviceDiscovery = DeviceDiscovery(context)
    private val streams = mutableListOf<LocalStageStream>()
    
    private var subscribeType = SubscribeType.NONE
    private var currentFacing = Position.FRONT
    private var _currentMode = StageMode.NONE
    private var videoDevice: ImageLocalStageStream? = null
    private var audioDevice: AudioLocalStageStream? = null
    private var _preview: ImagePreviewView? = null
    private var _isParticipating = false
    private var _isCreator = false
    private var _stageId: String? = null
    private var _participantId: String? = null

    private val _isLocalUserSpeaking = MutableStateFlow(false)
    private val _rtcData = MutableStateFlow(RTCData())
    private val _rtcDataList = MutableStateFlow(emptyList<RTCData>())

    var isAudioOff = false
        private set
    var isVideoOff = false
        private set
    val isFacingBack get() = currentFacing != Position.FRONT
    val preview get() = _preview
    val currentMode get() = _currentMode
    var joinedParticipants = ConcurrentLinkedQueue<ParticipantAttributes>()
    val isLocalUserSpeaking = _isLocalUserSpeaking.asStateFlow()
    val rtcData = _rtcData.asStateFlow()
    val rtcDataList = _rtcDataList.asStateFlow()

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

    val strategy = object : Strategy {
        override fun stageStreamsToPublishForParticipant(stage: Stage, info: ParticipantInfo) = streams

        override fun shouldPublishFromParticipant(stage: Stage, info: ParticipantInfo) = _isParticipating

        override fun shouldSubscribeToParticipant(stage: Stage, info: ParticipantInfo) = subscribeType
    }

    suspend fun setup(
        type: StageType,
        isParticipating: Boolean,
        isCreator: Boolean,
        stageId: String?,
        participantId: String?,
        mode: StageMode = StageMode.NONE
    ) {
        _currentMode = mode
        _isParticipating = isParticipating
        _stageId = stageId
        _participantId = participantId
        _isCreator = isCreator
        subscribeType = when {
            isCreator && mode == StageMode.NONE -> SubscribeType.NONE
            type == StageType.AUDIO -> SubscribeType.AUDIO_ONLY
            type == StageType.VIDEO -> SubscribeType.AUDIO_VIDEO
            else -> SubscribeType.NONE
        }
        val isVideoEnabled = isParticipating && type == StageType.VIDEO
        Timber.d("Setup stage strategy: $subscribeType, $type, $isParticipating, $isCreator, $mode, $isVideoEnabled")
        setupStreams(isParticipating, isVideoEnabled, type)
    }

    fun removeStreams() {
        streams.clear()
    }

    fun switchAudio() {
        audioDevice?.let { device ->
            isAudioOff = !isAudioOff
            device.muted = isAudioOff
        }
    }

    fun switchVideo() {
        videoDevice?.let { device ->
            isVideoOff = !isVideoOff
            device.muted = isVideoOff
        }
    }

    suspend fun switchFacing() {
        videoDevice?.let { currentDevice ->
            val wasMuted = isVideoOff
            _preview?.setVisibleOr(false)
            streams.remove(currentDevice)
            currentDevice.setListener(null)
            currentFacing = if (currentFacing == Position.FRONT) Position.BACK else Position.FRONT
            getDevice(DeviceType.CAMERA, currentFacing)?.let { device ->
                Timber.d("Camera device has been switched to $currentFacing")
                videoDevice = ImageLocalStageStream(device, StageVideoConfiguration().apply {
                    simulcast.isEnabled = isSimulcastEnabled()
                })
                videoDevice!!.muted = wasMuted
                videoDevice?.setListener(createRTCStatsListenerObject(
                    stageId = _stageId,
                    participantId = _participantId
                ))
                streams.add(videoDevice!!)
            }
            refreshVideoPreview()
        }
    }

    fun refreshVideoPreview(): ImagePreviewView? {
        Timber.d("Refreshing preview")
        _preview = (videoDevice?.device as? SurfaceSource)?.getPreviewView(BroadcastConfiguration.AspectMode.FILL)
        return _preview
    }

    fun dispose() {
        removeStreams()
        _rtcDataList.updateList { clear() }
        joinedParticipants = ConcurrentLinkedQueue()
        deviceDiscovery.release()
        audioDevice?.setStatsCallback(null)
        audioDevice?.setListener(null)
        videoDevice?.setListener(null)
        videoDevice = null
        audioDevice = null
        _preview = null
    }

    fun requestRTCStatsForVideoStream() {
        videoDevice?.requestRTCStats()
    }

    fun requestRTCStatsForAudioStream() {
        audioDevice?.requestRTCStats()
    }

    fun removeRTCDataById(participantId: String) =
        _rtcDataList.updateList {
            val isRemoved = removeIf { it.participantId == participantId }
            Timber.d("RTC data user removal state - $isRemoved")
        }

    fun createRTCStatsListenerObject(
        participantId: String? = null,
        stageId: String? = null,
        forAudio: Boolean = false,
        forViewer: Boolean = false
    ) = object : StageStream.Listener {
        override fun onMutedChanged(state: Boolean) { /* Ignored */ }

        override fun onRTCStats(stats: MutableMap<String, MutableMap<String, String>>) {
            val isHost = joinedParticipants.find { it.participantId == participantId }?.isHost ?: _isCreator
            Timber.d("RTC stats for stream: $forAudio, $forViewer, $_currentMode, $subscribeType, $stageId, $participantId, $isHost")
            var rtcData = parseRTCData(stats = stats, isForViewer = forViewer, isHost = isHost)
            if (!forViewer) {
                _rtcData.update { rtcData }
            } else if (participantId != null) {
                _rtcDataList.updateList {
                    val index = this.indexOfFirst { it.participantId == participantId }
                    rtcData = RTCData(
                        latency = rtcData.latency,
                        fps = if (forAudio) null else rtcData.fps,
                        packetLoss = rtcData.packetLoss,
                        stageId = stageId,
                        participantId = participantId,
                        isHostData = isHost,
                        isGuestData = !isHost && subscribeType != SubscribeType.AUDIO_ONLY
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

    private suspend fun isSimulcastEnabled() = appSettingsStore.data.first().isSimulcastEnabled
    private suspend fun getBitrate() = appSettingsStore.data.first().bitrate

    private fun parseRTCData(
        stats: Map<String, Map<String, String>>?,
        isForViewer: Boolean = false,
        isHost: Boolean = false
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
                if (reason == "none") RTCData.StreamQuality.NORMAL else RTCData.StreamQuality.LIMITED
            }

            // Workaround, because of the non-consistency of data format in raw RTC stats map
            val qualityDurations = outbound?.qualityLimitationDurations?.let { JSONObject(it) }
                ?.toString()
                ?.asObject<QualityLimitationDurationData>()

            val latency = candidatePair?.currentRoundTripTime?.times(1000)
            val fps = if (isForViewer) inbound?.framesPerSecond else outbound?.framesPerSecond
            val packetLost = if (isForViewer) inbound?.packetsLost else remoteInbound?.packetsLost
            latency?.roundToInt()?.toString()?.let { value ->
                onVideoStatsUpdated(RTCLatency(value = value, isHost = isHost))
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

    private suspend fun setupStreams(isAudioEnabled: Boolean, isVideoEnabled: Boolean, type: StageType) {
        streams.clear()
        audioDevice?.setStatsCallback(null)
        audioDevice?.setListener(null)
        audioDevice = null
        videoDevice?.setListener(null)
        videoDevice = null
        if (isAudioEnabled) {
            getDevice(DeviceType.MICROPHONE)?.let { device ->
                audioDevice = AudioLocalStageStream(device)
                audioDevice?.muted = isAudioOff
                audioDevice?.setStatsCallback { _, rms ->
                    _isLocalUserSpeaking.update { rms >= RMS_SPEAKING_THRESHOLD }
                }
                if (type == StageType.AUDIO) {
                    audioDevice?.setListener(createRTCStatsListenerObject(
                        stageId = _stageId,
                        participantId = _participantId,
                        forAudio = true
                    ))
                }
                streams.add(audioDevice!!)
            }
        }
        if (isVideoEnabled) {
            getDevice(DeviceType.CAMERA, currentFacing)?.let { device ->
                videoDevice = ImageLocalStageStream(device, StageVideoConfiguration().apply {
                    val bitrate = getBitrate()
                    val isSimulcastEnabled = isSimulcastEnabled()
                    Timber.d("Max bitrate set: $bitrate")
                    simulcast.isEnabled = isSimulcastEnabled

                    // Enabling simulcast means the bitrate is automatically managed
                    if (!isSimulcastEnabled) {
                        maxBitrate = bitrate
                    }
                })
                videoDevice?.muted = isVideoOff
                if (type == StageType.VIDEO) {
                    videoDevice?.setListener(createRTCStatsListenerObject(
                        stageId = _stageId,
                        participantId = _participantId
                    ))
                }
                streams.add(videoDevice!!)
            }
        }
        refreshVideoPreview()
        Timber.d("Participant streams added: $isAudioEnabled, $isVideoEnabled, ${streams.size}")
    }

    private fun getDevice(type: DeviceType, position: Position? = null): Device? {
        val devices: List<Device> = deviceDiscovery.listLocalDevices().sortedBy { it.descriptor.deviceId }
        return devices.find { it.descriptor.type == type && (position == null || it.descriptor.position == position) }
    }
}
