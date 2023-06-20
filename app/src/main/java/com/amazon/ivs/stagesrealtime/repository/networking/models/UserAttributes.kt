package com.amazon.ivs.stagesrealtime.repository.networking.models

import kotlinx.serialization.Serializable

@Serializable
data class UserAttributes(
    val avatarColBottom: String,
    val avatarColLeft: String,
    val avatarColRight: String,
    val username: String,
)
