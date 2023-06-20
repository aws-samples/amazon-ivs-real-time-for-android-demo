package com.amazon.ivs.stagesrealtime.repository.models

import com.amazon.ivs.stagesrealtime.common.DEFAULT_COLOR_BOTTOM
import com.amazon.ivs.stagesrealtime.common.DEFAULT_COLOR_LEFT
import com.amazon.ivs.stagesrealtime.common.DEFAULT_COLOR_RIGHT

@kotlinx.serialization.Serializable
data class UserAvatar(
    val colorLeft: String = DEFAULT_COLOR_LEFT,
    val colorRight: String = DEFAULT_COLOR_RIGHT,
    val colorBottom: String = DEFAULT_COLOR_BOTTOM,
    var hasBorder: Boolean = false
)
