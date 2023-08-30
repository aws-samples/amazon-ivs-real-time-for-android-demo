package com.amazon.ivs.stagesrealtime.common.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

fun launchMain(block: suspend CoroutineScope.() -> Unit) = mainScope.launch(
    context = CoroutineExceptionHandler { _, e -> Timber.w(e, "Coroutine failed: ${e.localizedMessage}") },
    block = block
)

fun Fragment.launchUI(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.() -> Unit
) = viewLifecycleOwner.lifecycleScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        Timber.e(e, "Coroutine failed: ${e.localizedMessage}")
    }
) {
    repeatOnLifecycle(state = lifecycleState, block = block)
}

fun ViewModel.launch(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        Timber.e(e, "Coroutine failed: ${e.localizedMessage}")
    },
    block = block
)

fun <T> Fragment.collectLatestWithLifecycle(
    flow: Flow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collectLatest: suspend (T) -> Unit
) {
    launchUI(lifecycleState) {
        flow.collectLatest(collectLatest)
    }
}

fun <T> MutableStateFlow<List<T>>.updateList(block: MutableList<T>.() -> Unit) = update {
    it.toMutableList().apply(block = block)
}

suspend fun <T : Any> runCancellableCatching(
    tryBlock: suspend () -> T,
    errorBlock: suspend (e: Exception) -> T,
): T = try {
    tryBlock()
} catch (e: Exception) {
    // If the coroutine was cancelled - let the error flow upstream to be handled properly
    if (e is CancellationException) throw e
    val failure = errorBlock(e)
    Timber.e(e, "Caught error with ${failure::class.qualifiedName}")
    failure
}

fun <T> Flow<T>.asStateFlow(
    coroutineScope: CoroutineScope,
    initialValue: T,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000),
) = stateIn(coroutineScope, sharingStarted, initialValue)
