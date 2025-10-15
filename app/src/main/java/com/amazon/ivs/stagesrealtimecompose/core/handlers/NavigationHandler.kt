package com.amazon.ivs.stagesrealtimecompose.core.handlers

import com.amazon.ivs.stagesrealtimecompose.core.common.ANIMATION_DURATION_LONG
import com.amazon.ivs.stagesrealtimecompose.core.common.ANIMATION_DURATION_NORMAL
import com.amazon.ivs.stagesrealtimecompose.core.common.ERROR_BAR_DURATION
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.launchMain
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.Error
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

object NavigationHandler {
    private val _backStack = mutableListOf<Destination>()
    private val _destination = MutableStateFlow<Destination>(Destination.None)
    private val _dialogDestination = MutableStateFlow<DialogDestination>(DialogDestination.None)
    private val _errorDestination = MutableStateFlow<ErrorDestination>(ErrorDestination.None)
    private val _isDialogClosing = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private var _errorJob: Job? = null

    val destination = _destination.asStateFlow()
    val dialogDestination = _dialogDestination.asStateFlow()
    val errorDestination = _errorDestination.asStateFlow()
    val isDialogClosing = _isDialogClosing.asStateFlow()
    val isLoading = _isLoading.asStateFlow()

    init {
        val session = PreferencesHandler.session
        Timber.d("Current session: $session")
        val startDestination = if (session != null) {
            Destination.Landing()
        } else {
            Destination.Splash
        }
        goTo(startDestination)
    }

    fun signOut() {
        hideError()
        closeDialog()
        Timber.d("Signing out")

        PreferencesHandler.session = null
        PreferencesHandler.user = null
        _backStack.clear()
        _backStack.add(Destination.Splash)
        _destination.update { Destination.Splash }

        launchMain {
            delay(ANIMATION_DURATION_LONG)
            showDialog(DialogDestination.EnterCode)
        }
    }

    fun goTo(destination: Destination) {
        if (destination is Destination.Landing) {
            Timber.d("Clearing backstack")
            _backStack.clear()
        }
        val indexOfExactCopy = _backStack.indexOfFirst { it == destination }.takeIf { it >= 0 }
        val indexOfClassInstance = _backStack.indexOfFirst { it::class == destination::class }.takeIf { it >= 0 }

        hideError()
        closeDialog()

        if (indexOfClassInstance != null) {
            Timber.d("Reordered the: $destination from: $indexOfClassInstance to ${_backStack.size - 1}")
            _backStack.removeAt(indexOfClassInstance)
        } else if (indexOfExactCopy != null) {
            Timber.d("Reordered the: $destination from: $indexOfExactCopy to ${_backStack.size - 1}")
            _backStack.removeAt(indexOfExactCopy)
        }

        _backStack.add(destination)
        Timber.d("Going to: $destination, backstack: [${_backStack.size}]")
        _destination.update { destination }
    }

    fun goBack() {
        Timber.d("Going back")
        if (closeDialog()) return
        val stage = StageHandler.currentStage
        if (stage != null && stage.isJoined) {
            Timber.d("Can't go back - still participating in stage: $stage")
            if (stage.isStageCreator) {
                showDialog(DialogDestination.EndStage)
            } else if (stage.isAudioRoom) {
                showDialog(DialogDestination.LeaveAudioRoom)
            } else {
                showDialog(DialogDestination.LeaveStage)
            }
            return
        }

        val last = _backStack.removeLastOrNull()
        val parent = _backStack.lastOrNull() ?: Destination.Finish
        if (last is Destination.Stage) {
            StageHandler.dispose()
        }
        Timber.d("Returning to: $parent from: $last")
        _destination.update { parent }
    }

    fun showDialog(destination: DialogDestination) {
        Timber.d("Showing dialog: $destination")
        _dialogDestination.update { destination }
    }

    fun showError(error: ErrorDestination) {
        if (_errorDestination.value == error) return

        _errorJob?.cancel()
        _errorJob = launchMain {
            _errorDestination.update { error }
            delay(ERROR_BAR_DURATION)
            _errorDestination.update { ErrorDestination.None }
        }
    }

    fun setLoading(isLoading: Boolean) = _isLoading.update { isLoading }

    private fun closeDialog() = if (_dialogDestination.value != DialogDestination.None) {
        launchMain {
            if (_dialogDestination.value == DialogDestination.None) return@launchMain
            _isDialogClosing.update { true }
            delay(ANIMATION_DURATION_NORMAL)

            Timber.d("Closing dialog: ${_dialogDestination.value}")
            _dialogDestination.update { DialogDestination.None }

            delay(ANIMATION_DURATION_NORMAL)
            _isDialogClosing.update { false }
        }
        true
    } else false

    private fun hideError() {
        _errorJob?.cancel()
        _errorJob = null
        _errorDestination.update { ErrorDestination.None }
    }
}

sealed class Destination {
    open val useDarkIcons = false

    data object None : Destination()
    data object Finish : Destination()
    data object Splash : Destination()
    data object QR : Destination()
    data class Landing(override val useDarkIcons: Boolean = true) : Destination()
    data class Stage(val type: StageDestinationType) : Destination()
}

sealed class DialogDestination {
    data object None : DialogDestination()
    data object EnterCode : DialogDestination()
    data object Settings : DialogDestination()
    data object Experience : DialogDestination()
    data object Debug : DialogDestination()
    data object JoinStage : DialogDestination()
    data object LeaveStage : DialogDestination()
    data object LeaveAudioRoom : DialogDestination()
    data object KickParticipant : DialogDestination()
    data object EndStage : DialogDestination()
}

sealed class ErrorDestination {
    open val error: Error? = null

    data object None : ErrorDestination()
    data class SnackBar(override val error: Error) : ErrorDestination()
}

enum class StageDestinationType {
    Audio, Video, None
}
