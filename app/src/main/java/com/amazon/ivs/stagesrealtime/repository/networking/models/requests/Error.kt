package com.amazon.ivs.stagesrealtime.repository.networking.models.requests

import androidx.annotation.StringRes
import com.amazon.ivs.stagesrealtime.R

sealed class Error(@StringRes val errorResource: Int) {
    data object CustomerCodeError : Error(R.string.error_customer_code)
    data object GetStagesError : Error(R.string.error_get_stages)
    data object CreateStageError : Error(R.string.error_create_stage)
    data object JoinStageError : Error(R.string.error_join_stage)
    data object LeaveStageError : Error(R.string.error_leave_stage)
    data object KickParticipantError : Error(R.string.error_kick_participant)
    data object DeleteStageError : Error(R.string.error_delete_stage)
    data object UpdateSeatsError : Error(R.string.update_seats_error)
    data object CastVoteError : Error(R.string.error_cast_vote)
}
