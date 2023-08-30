
package com.amazon.ivs.stagesrealtime.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext

/**
 * Calls the specified function [block] with [SuspendableResultBinding] as the receiver and returns a [Response].
 *
 * Inside of the [binding] block, any function returning a [Response] can call [SuspendableResultBinding.bind], which
 * will try to unwrap and locally return it's [Success] value.
 * If that fails, the binding [block] is terminated and it returns the given [Failure].
 *
 * In other words, it allows us to easily back out of a function [block] if any of the operations inside of it fail.
 */
@OptIn(ExperimentalContracts::class)
suspend inline fun <F, S> binding(crossinline block: suspend SuspendableResultBinding<F>.() -> S): Response<F, S> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    lateinit var receiver: SuspendableResultBindingImpl<F>

    return try {
        coroutineScope {
            receiver = SuspendableResultBindingImpl(this.coroutineContext)
            with(receiver) { Success(block()) }
        }
    } catch (ex: BindCancellationException) {
        receiver.internalError
    }
}

internal object BindCancellationException : CancellationException(null as String?)

interface SuspendableResultBinding<F> : CoroutineScope {
    suspend fun <S> Response<F, S>.bind(): S
}

@PublishedApi
internal class SuspendableResultBindingImpl<F>(
    override val coroutineContext: CoroutineContext
) : SuspendableResultBinding<F> {
    private val mutex = Mutex()
    lateinit var internalError: Failure<F>

    override suspend fun <V> Response<F, V>.bind(): V {
        return when (this) {
            is Success -> value
            is Failure -> mutex.withLock {
                if (::internalError.isInitialized.not()) {
                    internalError = this
                    this@SuspendableResultBindingImpl.cancel(BindCancellationException)
                }

                throw BindCancellationException
            }
        }
    }
}
