package com.amazon.ivs.stagesrealtimecompose.core.handlers.chat

import com.amazon.ivs.stagesrealtimecompose.core.common.MAX_MESSAGE_COUNT
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.asObject
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.asVSSCore
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.getUserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.getUserName
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.toJson
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.updateList
import com.amazon.ivs.stagesrealtimecompose.core.handlers.PreferencesHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.User
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.handlers.VSHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.StageModeLegacy
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.toStageParticipantMode
import com.amazonaws.ivs.chat.messaging.ChatRoom
import com.amazonaws.ivs.chat.messaging.ChatRoomListener
import com.amazonaws.ivs.chat.messaging.ChatToken
import com.amazonaws.ivs.chat.messaging.DisconnectReason
import com.amazonaws.ivs.chat.messaging.SendMessageCallback
import com.amazonaws.ivs.chat.messaging.entities.ChatError
import com.amazonaws.ivs.chat.messaging.entities.ChatEvent
import com.amazonaws.ivs.chat.messaging.entities.ChatMessage
import com.amazonaws.ivs.chat.messaging.entities.DeleteMessageEvent
import com.amazonaws.ivs.chat.messaging.entities.DisconnectUserEvent
import com.amazonaws.ivs.chat.messaging.logger.ChatLogLevel
import com.amazonaws.ivs.chat.messaging.requests.SendMessageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import timber.log.Timber

object ChatHandler {
    private var _chatRoom: ChatRoom? = null
    private var _chatRoomListener: ChatRoomListener? = null
    private val _messages = MutableStateFlow(emptyList<StageMessage>())

    val messages = _messages.asStateFlow()

    fun joinRoom(
        chatToken: ChatToken,
        region: String,
        stageId: String
    ) {
        _messages.update { emptyList() }
        _chatRoom?.disconnect()
        _chatRoom = ChatRoom(
            regionOrUrl = region,
            tokenProvider = { it.onSuccess(chatToken) },
            maxReconnectAttempts = 0
        )
        _chatRoomListener = object : ChatRoomListener {
            override fun onConnected(room: ChatRoom) { Timber.d("On connected: ${room.id}") }
            override fun onConnecting(room: ChatRoom) { Timber.d("On connecting: ${room.id}") }
            override fun onDisconnected(room: ChatRoom, reason: DisconnectReason) {
                Timber.d("On disconnected: ${room.id}, ${reason.name}")
            }
            override fun onUserDisconnected(room: ChatRoom, event: DisconnectUserEvent) {
                Timber.d("On user disconnected: ${event.userId}")
            }

            override fun onEventReceived(room: ChatRoom, event: ChatEvent) {
                Timber.d("On event received: ${room.id}, $event")
                val mode = event.attributes?.get("mode")
                val seats = event.attributes?.get("seats")
                val message = event.attributes?.get("message")
                val notice = event.attributes?.get("notice")
                val username = event.attributes?.get("username")
                val eventMessages = mutableListOf<StageMessage>()
                if (mode != null) {
                    val legacyMode = StageModeLegacy.valueOf(mode)
                    Timber.d("Mode received: $mode, $legacyMode")
                    StageHandler.updateMode(legacyMode.toStageParticipantMode())
                }

                if (seats != null) {
                    try {
                        val parsedSeats = seats.removeSurrounding("[", "]")
                            .split(",")
                            .map { it.removeSurrounding("\"", "\"").trim() }
                        Timber.d("Seats received: $seats, $parsedSeats")
                        StageHandler.updateSeats(parsedSeats)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse seats")
                    }
                }
                if (message != null) {
                    eventMessages.add(
                        StageMessage.SystemMessage(
                            messageId = event.id,
                            username = username,
                            message = message
                        )
                    )
                }
                if (notice != null) {
                    eventMessages.add(
                        StageMessage.SystemMessage(
                            messageId = event.id,
                            username = username,
                            message = notice
                        )
                    )
                }
                _messages.updateList { addAll(eventMessages) }

                when (event.eventName) {
                    StageChatEvent.Vote.eventName -> {
                        val score = event.attributes?.asVSSCore(stageId = stageId) ?: return
                        VSHandler.setScore(score)
                    }
                    StageChatEvent.VoteStart.eventName -> {
                        val score = event.attributes?.asVSSCore(stageId = stageId) ?: return
                        VSHandler.setScore(score)
                        VSHandler.startScoreTimer()
                    }
                }
            }

            override fun onMessageDeleted(room: ChatRoom, event: DeleteMessageEvent) {
                Timber.d("On message deleted: ${room.id}, ${event.attributes}")
                _messages.updateList { remove(find { it.id == event.messageId }) }
            }

            override fun onMessageReceived(room: ChatRoom, message: ChatMessage) {
                Timber.d("Message received: $message")
                if (message.attributes?.get("type") == null) {
                    val messageCount = _messages.value.size
                    val messages = _messages.value.mapIndexed { index, chatMessage ->
                        chatMessage.copyObject(visible = index > messageCount - MAX_MESSAGE_COUNT)
                    }.toMutableSet().apply {
                        add(message.toUserMessage())
                    }.toList()
                    _messages.update { messages }
                    return
                }

                val userId = PreferencesHandler.user?.asObject<User>()?.username ?: return
                if (message.sender.userId == userId) return

                val messageAttributes = message.attributes?.asObject<ChatMessageAttributes>() ?: return
                if (messageAttributes.type == StageChatEvent.Event.eventName) {
                    StageHandler.addHeart()
                }
            }
        }
        _chatRoom?.listener = _chatRoomListener
        _chatRoom?.logLevel = ChatLogLevel.INFO
        _chatRoom?.connect()
    }

    fun sendMessage(message: String) {
        if (_chatRoom?.state != ChatRoom.State.CONNECTED) {
            Timber.d("Failed to send message - chat room not connected")
            return
        }
        val chatMessageRequest = SendMessageRequest(content = message)
        Timber.d("Sending message: $chatMessageRequest")
        _chatRoom?.sendMessage(chatMessageRequest, object : SendMessageCallback {
            override fun onConfirmed(request: SendMessageRequest, response: ChatMessage) {
                Timber.d("Message sent: ${request.requestId}, ${response.content}")
            }
            override fun onRejected(request: SendMessageRequest, error: ChatError) {
                Timber.d("Message send rejected: ${request.requestId}, ${error.errorMessage}")
            }
        })
    }

    fun likeStage() {
        if (_chatRoom?.state != ChatRoom.State.CONNECTED) {
            Timber.d("Failed to send message - chat room not connected")
            return
        }
        val attributes = ChatMessageAttributes(StageChatEvent.Event.eventName, "heart")
        val likeRequest = SendMessageRequest(
            attributes.reaction,
            attributes.toJson().asObject()
        )
        _chatRoom?.sendMessage(likeRequest, object : SendMessageCallback {
            override fun onConfirmed(request: SendMessageRequest, response: ChatMessage) {
                Timber.d("Message sent: ${request.requestId}, ${response.content}")
            }
            override fun onRejected(request: SendMessageRequest, error: ChatError) {
                Timber.d("Message send rejected: ${request.requestId}, ${error.errorMessage}")
            }
        })
    }

    fun clearMessages() {
        _messages.update { emptyList() }
    }
}

sealed class StageMessage(
    val id: String,
    val isVisible: Boolean
) {
    abstract fun copyObject(visible: Boolean): StageMessage

    data class UserMessage(
        val messageId: String,
        val username: String,
        val message: String,
        val avatar: UserAvatar,
        val visible: Boolean = true,
    ) : StageMessage(messageId, visible) {
        override fun copyObject(visible: Boolean) = this.copy(visible = visible)
    }

    data class SystemMessage(
        val messageId: String,
        val username: String?,
        val message: String,
        val visible: Boolean = true,
    ) : StageMessage(messageId, visible) {
        override fun copyObject(visible: Boolean) = this.copy(visible = visible)
    }
}

@Serializable
data class ChatMessageAttributes(
    val type: String,
    val reaction: String
)

enum class StageChatEvent(val eventName: String) {
    Vote("stage:VOTE"),
    VoteStart("stage:VOTE_START"),
    Event("EVENT")
}

private fun ChatMessage.toUserMessage() = StageMessage.UserMessage(
    messageId = id,
    message = content,
    username = sender.attributes.getUserName(fallback = sender.userId),
    avatar = sender.attributes.getUserAvatar()
)
