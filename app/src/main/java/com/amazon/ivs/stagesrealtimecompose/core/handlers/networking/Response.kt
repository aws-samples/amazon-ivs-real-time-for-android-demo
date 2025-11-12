package com.amazon.ivs.stagesrealtimecompose.core.handlers.networking

import com.amazon.ivs.stagesrealtimecompose.R

typealias Ok = Unit

@Suppress("unused")
sealed class Response<out F, out S> {
    inline fun <T> handle(onSuccess: (S) -> T, onFailure: (F) -> T): T = when (this) {
        is Failure -> onFailure(value)
        is Success -> onSuccess(value)
    }

    inline fun <T> onSuccess(block: (S) -> T) {
        if (this is Success) block(this.value)
    }

    inline fun <T> onFailure(block: (F) -> T) {
        if (this is Failure) block(this.value)
    }
}

class Failure<out F>(val value: F) : Response<F, Nothing>()

class Success<out S>(val value: S) : Response<Nothing, S>()

fun Success() = Success(Ok)

sealed class Error(val errorResource: Int) {
    data object CustomerCodeError : Error(R.string.err_customer_code)
    data object GetStagesError : Error(R.string.err_get_stages)
    data object CreateStageError : Error(R.string.err_create_stage)
    data object JoinStageError : Error(R.string.err_join_stage)
    data object LeaveStageError : Error(R.string.err_leave_stage)
    data object KickParticipantError : Error(R.string.err_kick_participant)
    data object DeleteStageError : Error(R.string.err_delete_stage)
    data object UpdateSeatsError : Error(R.string.err_update_seats)
    data object CastVoteError : Error(R.string.err_cast_vote)
    data object UpdateModeError : Error(R.string.err_update_mode)
}
