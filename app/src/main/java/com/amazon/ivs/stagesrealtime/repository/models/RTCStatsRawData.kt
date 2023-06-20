package com.amazon.ivs.stagesrealtime.repository.models

import kotlinx.serialization.Serializable

@Serializable
data class VideoStreamInboundData(
    val packetsLost: String? = null,
    val framesPerSecond: String? = null
)

@Serializable
data class VideoStreamOutboundData(
    val framesPerSecond: String? = null,
    val qualityLimitationReason: String? = null,
    val qualityLimitationDurations: String? = null
)

@Serializable
data class QualityLimitationDurationData(
    val cpu: String? = null,
    val bandwidth: String? = null
)

@Serializable
data class CandidatePairData(
    val currentRoundTripTime: Float
)
