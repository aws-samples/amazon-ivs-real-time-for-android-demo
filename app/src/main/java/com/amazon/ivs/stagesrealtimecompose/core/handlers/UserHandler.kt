package com.amazon.ivs.stagesrealtimecompose.core.handlers

import androidx.compose.ui.platform.SoftwareKeyboardController
import com.amazon.ivs.stagesrealtimecompose.core.common.ANIMATION_DURATION_MEDIUM
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.asObject
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.launchMain
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.toColor
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.toHex
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.toJson
import com.amazon.ivs.stagesrealtimecompose.core.common.getNewStageId
import com.amazon.ivs.stagesrealtimecompose.core.common.getNewUserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.Error
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.NetworkHandler
import com.amazon.ivs.stagesrealtimecompose.ui.theme.AvatarBottom
import com.amazon.ivs.stagesrealtimecompose.ui.theme.AvatarLeft
import com.amazon.ivs.stagesrealtimecompose.ui.theme.AvatarRight
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import timber.log.Timber

object UserHandler {
    private val _selfUser = MutableStateFlow(User())
    private val _session = MutableStateFlow<Session?>(null)
    private val _codeValidated = Channel<Unit>()
    private var _lastCode: String? = null

    val selfUser = _selfUser.asStateFlow()
    val session = _session.asStateFlow()
    val codeValidated = _codeValidated.receiveAsFlow()
    val currentUser get() = _selfUser.value
    val currentSession get() = _session.value

    init {
        _selfUser.update { PreferencesHandler.user?.asObject() ?: User() }
        _session.update { PreferencesHandler.session?.asObject() }
    }

    fun refreshUserName() {
        val user = _selfUser.value.copy(username = getNewStageId())
        PreferencesHandler.user = user.toJson()
        _selfUser.update { user }
    }

    fun enterCode(
        code: String,
        keyboardController: SoftwareKeyboardController? = null
    ) = launchMain {
        if (code == _lastCode) return@launchMain
        Timber.d("Validating code: $code, $_lastCode")
        _lastCode = code
        val parts = code.split("-")
        val customerCode = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: run {
            NavigationHandler.showError(ErrorDestination.SnackBar(Error.CustomerCodeError))
            return@launchMain
        }
        val apiKey = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: run {
            NavigationHandler.showError(ErrorDestination.SnackBar(Error.CustomerCodeError))
            return@launchMain
        }
        val session = Session(
            customerCode = customerCode,
            apiKey = apiKey
        )
        val user = User()
        Timber.d("Session: $session, $user")
        PreferencesHandler.user = user.toJson()
        PreferencesHandler.session = session.toJson()
        _session.update { session }

        keyboardController?.run {
            hide()
            delay(ANIMATION_DURATION_MEDIUM)
        }

        NavigationHandler.setLoading(true)
        NetworkHandler.verifyConnection().handle(
            onSuccess = {
                Timber.d("Signed in")
                _selfUser.update { user }
                _codeValidated.trySend(Unit)
                NavigationHandler.setLoading(false)
                NavigationHandler.goTo(Destination.Landing())
            },
            onFailure = { error ->
                Timber.d("Failed to sign in: $error")
                _codeValidated.trySend(Unit)
                NavigationHandler.setLoading(false)
                NavigationHandler.showError(error = ErrorDestination.SnackBar(error))
                PreferencesHandler.user = null
                PreferencesHandler.session = null
                _session.update { null }
            }
        )
    }

    fun clearLastCode() {
        Timber.d("Clearing last code: $_lastCode")
        _lastCode = null
    }
}

@Serializable
data class User(
    val username: String = getNewStageId(),
    val userAvatar: UserAvatar = getNewUserAvatar(),
)

@Serializable
data class UserAvatar(
    val colorLeft: String = AvatarLeft.toHex(),
    val colorRight: String = AvatarRight.toHex(),
    val colorBottom: String = AvatarBottom.toHex(),
    var hasBorder: Boolean = false
) {
    val colors get() = listOf(
        colorLeft.toColor(),
        colorRight.toColor(),
        colorBottom.toColor()
    )
}

@Serializable
data class Session(
    val customerCode: String,
    val apiKey: String
)
