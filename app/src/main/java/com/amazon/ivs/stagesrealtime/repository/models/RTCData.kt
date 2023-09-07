package com.amazon.ivs.stagesrealtime.repository.models

import com.amazonaws.ivs.broadcast.BroadcastSession

data class RTCData(
    val streamQuality: StreamQuality? = null,
    val cpuLimitedTime: String? = null,
    val networkLimitedTime: String? = null,
    val latency: String? = null,
    val fps: String? = null,
    val packetLoss: String? = null,
    val rawRTCStats: String? = null,
    val userInfo: ParticipantAttributes? = null,
    val isHostData: Boolean = false,
    val isGuestData: Boolean = false,
    val sdkVersion: String = BroadcastSession.getVersion()
) {
    enum class StreamQuality { NORMAL, LIMITED }
}
