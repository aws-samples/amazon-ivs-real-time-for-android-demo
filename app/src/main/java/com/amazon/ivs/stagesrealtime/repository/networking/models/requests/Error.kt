package com.amazon.ivs.stagesrealtime.repository.networking.models.requests

import androidx.annotation.StringRes
import com.amazon.ivs.stagesrealtime.R

sealed class Error(@StringRes val errorResource: Int) {
    object CustomerCodeError : Error(R.string.error_customer_code)
    object GetStagesError : Error(R.string.error_get_stages)
    object CreateStageError : Error(R.string.error_create_stage)
    object JoinStageError : Error(R.string.error_join_stage)
    object LeaveStageError : Error(R.string.error_leave_stage)
    object KickParticipantError : Error(R.string.error_kick_participant)
    object DeleteStageError : Error(R.string.error_delete_stage)
    object UpdateSeatsError : Error(R.string.update_seats_error)
    object CastVoteError : Error(R.string.error_cast_vote)
}
