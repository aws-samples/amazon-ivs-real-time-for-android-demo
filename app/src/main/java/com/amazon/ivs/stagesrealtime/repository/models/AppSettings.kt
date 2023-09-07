package com.amazon.ivs.stagesrealtime.repository.models

import com.amazon.ivs.stagesrealtime.common.DEFAULT_VIDEO_BITRATE
import com.amazon.ivs.stagesrealtime.common.getNewStageId
import com.amazon.ivs.stagesrealtime.common.getNewUserAvatar
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val stageId: String = getNewStageId(),
    val customerCode: String? = null,
    val userAvatar: UserAvatar = getNewUserAvatar(),
    val bitrate: Int = DEFAULT_VIDEO_BITRATE,
    val apiKey: String? = null,
    val isSimulcastEnabled: Boolean = false,
)
