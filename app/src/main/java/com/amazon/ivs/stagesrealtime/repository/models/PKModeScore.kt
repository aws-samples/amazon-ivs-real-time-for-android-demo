package com.amazon.ivs.stagesrealtime.repository.models

data class PKModeScore(
    val hostScore: Int = 0,
    val guestScore: Int = 0,
    val shouldResetScore: Boolean = false
)
