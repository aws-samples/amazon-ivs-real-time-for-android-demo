package com.amazon.ivs.stagesrealtime.common

typealias Ok = Unit

/**
 * [Response] is our custom class for handling errors in a functional programming way like with
 * Either in Arrow (and other languages) and Result in Rust.
 * We have created our own small utility in this sample project to avoid taking on another library as a
 * dependency.
 *
 * If you wish to learn more about this pattern of error handling, it is highly recommended to check out
 * Rust's implementation or to search for "Railway Oriented Programming".
 *
 * The advantage it gives us in this project is ensuring every error type a function can return
 * is clearly seen in the return type of that function.
 */
sealed class Response<out F, out S> {
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
