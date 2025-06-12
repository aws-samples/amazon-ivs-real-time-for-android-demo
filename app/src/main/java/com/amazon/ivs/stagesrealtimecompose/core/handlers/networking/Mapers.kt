package com.amazon.ivs.stagesrealtimecompose.core.handlers.networking

import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.toDate
import com.amazonaws.ivs.chat.messaging.ChatToken

fun ChatTokenResponse.asChatToken() = ChatToken(
    token = token,
    sessionExpirationTime = sessionExpirationTime.toDate(),
    tokenExpirationTime = tokenExpirationTime.toDate()
)
