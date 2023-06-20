package com.amazon.ivs.stagesrealtime.repository.models

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
    val isGuestData: Boolean = false
) {
    enum class StreamQuality { NORMAL, LIMITED }
}
