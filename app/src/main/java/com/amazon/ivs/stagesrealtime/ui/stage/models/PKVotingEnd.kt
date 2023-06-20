package com.amazon.ivs.stagesrealtime.ui.stage.models

import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar

sealed class PKVotingEnd(val userAvatar: UserAvatar?) {
    data class HostWon(val avatar: UserAvatar) : PKVotingEnd(avatar)
    data class GuestWon(val avatar: UserAvatar) : PKVotingEnd(avatar)
    object Draw : PKVotingEnd(null)
    object Nothing : PKVotingEnd(null)
}
