package com.amazon.ivs.stagesrealtime.repository.chat

enum class ChatSdkError(
    var rawError: String? = null,
    var rawCode: Int = -1,
) {
    MESSAGE_SEND_FAILED,
    LIKE_FAILED;
}
