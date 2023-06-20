package com.amazon.ivs.stagesrealtime.common

typealias Ok = Unit

sealed class Response<out F, out S> {

    fun failure() = when (this) {
        is Failure -> value
        else -> null
    }

    inline fun <T> onSuccess(block: (S) -> T) {
        if (this is Success) block(this.value)
    }

    inline fun <T> onFailure(block: (F) -> T) {
        if (this is Failure) block(this.value)
    }

    fun unwrap(): S =
        if (this is Success) {
            this.value
        } else {
            throw IllegalStateException("A response that was expected to be Success was a Failure: $this")
        }
}

class Failure<out F>(val value: F) : Response<F, Nothing>()

class Success<out S>(val value: S) : Response<Nothing, S>()

fun Success() = Success(Ok)
