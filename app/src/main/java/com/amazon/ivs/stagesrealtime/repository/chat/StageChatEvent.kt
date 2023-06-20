package com.amazon.ivs.stagesrealtime.repository.chat

enum class StageChatEvent(val eventName: String) {
    VOTE("stage:VOTE"),
    VOTE_START("stage:VOTE_START"),
    EVENT("EVENT")
}
