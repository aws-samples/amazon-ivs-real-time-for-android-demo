package com.amazon.ivs.stagesrealtime.ui.stage.models

data class RTCDataUIItemModel(
    val latency: String?,
    val fps: String?,
    val packetsLost: String?,
    val isForAudio: Boolean = false,
    val username: String,
    val participantId: String,
    val isHost: Boolean = false,
    val isGuest: Boolean = false
)
