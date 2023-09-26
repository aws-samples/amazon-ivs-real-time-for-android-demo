package com.amazon.ivs.stagesrealtime.repository.models

import kotlinx.serialization.Serializable

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
