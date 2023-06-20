package com.amazon.ivs.stagesrealtime.ui.stage.models

import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar

sealed class ChatUIMessage(val id: String) {
    data class UserMessage(
        val messageId: String,
        val username: String,
        val message: String,
        val avatar: UserAvatar
    ) : ChatUIMessage(messageId)

    data class SystemMessage(
        val messageId: String,
        val message: String,
    ) : ChatUIMessage(messageId)
}
