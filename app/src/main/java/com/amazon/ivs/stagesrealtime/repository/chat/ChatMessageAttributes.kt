package com.amazon.ivs.stagesrealtime.repository.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageAttributes(
    val type: String,
    val reaction: String
)
