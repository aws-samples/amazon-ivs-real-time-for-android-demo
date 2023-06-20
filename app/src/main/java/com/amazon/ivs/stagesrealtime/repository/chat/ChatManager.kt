package com.amazon.ivs.stagesrealtime.repository.chat

import com.amazon.ivs.stagesrealtime.common.COLOR_BOTTOM_ATTRIBUTE_NAME
import com.amazon.ivs.stagesrealtime.common.COLOR_LEFT_ATTRIBUTE_NAME
import com.amazon.ivs.stagesrealtime.common.COLOR_RIGHT_ATTRIBUTE_NAME
import com.amazon.ivs.stagesrealtime.common.DEFAULT_COLOR_BOTTOM
import com.amazon.ivs.stagesrealtime.common.DEFAULT_COLOR_LEFT
import com.amazon.ivs.stagesrealtime.common.DEFAULT_COLOR_RIGHT
import com.amazon.ivs.stagesrealtime.common.VOTE_SESSION_TIME_SECONDS
import com.amazon.ivs.stagesrealtime.common.extensions.asObject
import com.amazon.ivs.stagesrealtime.common.extensions.asPKModeScore
import com.amazon.ivs.stagesrealtime.common.extensions.launchIO
import com.amazon.ivs.stagesrealtime.common.extensions.launchMain
import com.amazon.ivs.stagesrealtime.common.extensions.toJson
import com.amazon.ivs.stagesrealtime.common.extensions.updateList
import com.amazon.ivs.stagesrealtime.repository.models.PKModeScore
import com.amazon.ivs.stagesrealtime.repository.models.PKModeSessionTime
import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar
import com.amazon.ivs.stagesrealtime.ui.stage.models.ChatUIMessage
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class ChatManager {
    private var chatRoom: ChatRoom? = null
    private var roomListener: ChatRoomListener? = null
    private var localUserId: String? = null
    private val likeMessageAttributes = ChatMessageAttributes(StageChatEvent.EVENT.eventName, "heart")

    private val _onError = Channel<ChatSdkError>()
    private val _messages = MutableStateFlow(emptyList<ChatUIMessage>())
    private val _onModeChanged = Channel<String>()
    private val _onSeatsUpdated = Channel<List<String>>()
    private val _onStageLike = Channel<Unit>()
    private val _onPKModeScore = MutableSharedFlow<PKModeScore>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _onVoteStart = Channel<PKModeSessionTime>()

    val messages = _messages.asStateFlow()
    val onModeChanged = _onModeChanged.receiveAsFlow()
    val onSeatsUpdated = _onSeatsUpdated.receiveAsFlow()
    val onStageLike = _onStageLike.receiveAsFlow()
    val onPKModeScore = _onPKModeScore.asSharedFlow()
    val onVoteStart = _onVoteStart.receiveAsFlow()

    fun joinRoom(chatToken: ChatToken, region: String, userId: String, hostId: String) {
        Timber.d("Initializing chat room")
        clearPreviousChat()

        chatRoom = ChatRoom(
            regionOrUrl = region,
            tokenProvider = { it.onSuccess(chatToken) },
            maxReconnectAttempts = 0
        )
        localUserId = userId
        roomListener = object : ChatRoomListener {
            override fun onConnected(room: ChatRoom) {
                Timber.d("On connected ${room.id} ")
            }

            override fun onConnecting(room: ChatRoom) {
                Timber.d("On connecting ${room.id}")
            }

            override fun onDisconnected(room: ChatRoom, reason: DisconnectReason) {
                Timber.d("On disconnected ${room.id} ${reason.name}")
            }

            override fun onEventReceived(room: ChatRoom, event: ChatEvent) {
                Timber.d("On event received ${room.id} $event")
                launchMain {
                    val mode = event.attributes?.get("mode")
                    val seats = event.attributes?.get("seats")
                    val message = event.attributes?.get("message")
                    val notice = event.attributes?.get("notice")
                    val eventMessages = mutableListOf<ChatUIMessage>()
                    if (mode != null) {
                        _onModeChanged.send(mode)
                    }
                    if (seats != null) {
                        try {
                            val parsedSeats = seats.removeSurrounding("[", "]")
                                .split(",")
                                .map { it.removeSurrounding("\"", "\"").trim() }
                            Timber.d("Seats received: $seats, $parsedSeats")
                            _onSeatsUpdated.send(parsedSeats)
                        } catch (e: Exception) {
                            Timber.d("Failed to parse seats")
                        }
                    }
                    if (message != null) {
                        eventMessages.add(ChatUIMessage.SystemMessage(event.id, message))
                    }
                    if (notice != null) {
                        eventMessages.add(ChatUIMessage.SystemMessage(event.id, notice))
                    }
                    _messages.updateList { addAll(eventMessages) }

                    when (event.eventName) {
                        StageChatEvent.VOTE.eventName -> {
                            val score = event.attributes!!.asPKModeScore(hostId)
                            _onPKModeScore.tryEmit(score)
                        }
                        StageChatEvent.VOTE_START.eventName -> {
                            _onPKModeScore.tryEmit(event.attributes!!.asPKModeScore(hostId, true))
                            _onVoteStart.send(PKModeSessionTime(VOTE_SESSION_TIME_SECONDS))
                        }
                    }
                }
            }

            override fun onMessageDeleted(room: ChatRoom, event: DeleteMessageEvent) {
                Timber.d("On message deleted ${room.id} ${event.attributes}")
                _messages.updateList { remove(find { it.id == event.messageId }) }
            }

            override fun onMessageReceived(room: ChatRoom, message: ChatMessage) {
                Timber.d("Message: $message")
                if (message.attributes?.get("type") != null && message.sender.userId != localUserId) {
                    val messageAttributes = message.attributes!!.asObject<ChatMessageAttributes>()
                    when (messageAttributes.type) {
                        likeMessageAttributes.type -> {
                            launchIO {
                                _onStageLike.send(Unit)
                            }
                        }
                    }
                } else if (message.attributes?.get("type") == null) {
                    _messages.updateList {
                        add(message.toUserMessage())
                    }
                }
            }

            override fun onUserDisconnected(room: ChatRoom, event: DisconnectUserEvent) {
                Timber.d("On user disconnected ${event.userId}")
            }
        }
        chatRoom?.listener = roomListener
        chatRoom?.logLevel = ChatLogLevel.INFO
        chatRoom?.connect()
    }

    fun sendMessage(chatMessageRequest: SendMessageRequest) {
        if (chatRoom?.state != ChatRoom.State.CONNECTED) {
            Timber.d("Failed to send message - chat room not connected")
            _onError.trySend(ChatSdkError.MESSAGE_SEND_FAILED)
            return
        }
        Timber.d("Sending message: $chatMessageRequest")
        chatRoom?.sendMessage(chatMessageRequest, object : SendMessageCallback {
            override fun onConfirmed(request: SendMessageRequest, response: ChatMessage) {
                Timber.d("Message sent: ${request.requestId} ${response.content}")
            }

            override fun onRejected(request: SendMessageRequest, error: ChatError) {
                Timber.d("Message send rejected: ${request.requestId} ${error.errorMessage}")
                _onError.trySend(ChatSdkError.MESSAGE_SEND_FAILED.apply {
                    this.rawCode = error.errorCode
                    this.rawError = error.errorMessage
                })
            }
        })
    }

    fun likeStage() {
        if (chatRoom?.state != ChatRoom.State.CONNECTED) {
            Timber.d("Failed to send message - chat room not connected")
            _onError.trySend(ChatSdkError.MESSAGE_SEND_FAILED)
            return
        }
        val likeRequest = SendMessageRequest(
            likeMessageAttributes.reaction,
            likeMessageAttributes.toJson().asObject()
        )
        chatRoom?.sendMessage(likeRequest, object : SendMessageCallback {
            override fun onConfirmed(request: SendMessageRequest, response: ChatMessage) {
                Timber.d("Message sent: ${request.requestId} ${response.content}")
            }

            override fun onRejected(request: SendMessageRequest, error: ChatError) {
                Timber.d("Message send rejected: ${request.requestId} ${error.errorMessage}")
                _onError.trySend(ChatSdkError.LIKE_FAILED.apply {
                    this.rawCode = error.errorCode
                    this.rawError = error.errorMessage
                })
            }
        })
    }

    fun clearPreviousChat() {
        chatRoom?.disconnect()
        chatRoom = null
        roomListener = null
        localUserId = null
        _messages.update { emptyList() }
    }

    private fun ChatMessage.toUserMessage() = ChatUIMessage.UserMessage(
        messageId = id,
        username = sender.attributes?.get("username") ?: sender.userId,
        message = content,
        avatar = UserAvatar(
            colorBottom = sender.attributes?.get(COLOR_BOTTOM_ATTRIBUTE_NAME) ?: DEFAULT_COLOR_BOTTOM,
            colorLeft = sender.attributes?.get(COLOR_LEFT_ATTRIBUTE_NAME) ?: DEFAULT_COLOR_LEFT,
            colorRight = sender.attributes?.get(COLOR_RIGHT_ATTRIBUTE_NAME) ?: DEFAULT_COLOR_RIGHT
        )
    )
}
