package com.amazon.ivs.stagesrealtime.ui.welcome

import androidx.lifecycle.ViewModel
import com.amazon.ivs.stagesrealtime.common.DEFAULT_LOADING_DELAY
import com.amazon.ivs.stagesrealtime.common.extensions.launch
import com.amazon.ivs.stagesrealtime.common.getNewStageId
import com.amazon.ivs.stagesrealtime.common.getNewUserAvatar
import com.amazon.ivs.stagesrealtime.repository.StageRepository
import com.amazon.ivs.stagesrealtime.repository.models.CreateStageMode
import com.amazon.ivs.stagesrealtime.repository.models.ParticipantMode
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageType
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.Error
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val repository: StageRepository
) : ViewModel() {
    private val _userName = MutableStateFlow(repository.stageId)
    private val _onSignedIn = Channel<Unit>()
    private val _onSignedOut = Channel<Unit>()
    private val _onError = Channel<Error>()
    private val _onCustomerCodeSet = Channel<Boolean>()
    private val _onNavigateToStage = Channel<Pair<ParticipantMode, CreateStageMode>>()
    private val _onLoading = MutableSharedFlow<Boolean>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    var bitrate by Delegates.observable(repository.bitrate) { _, _, bitrate ->
        repository.bitrate = bitrate
    }

    val userName = _userName.asStateFlow()
    val onSignedIn = _onSignedIn.receiveAsFlow()
    val onSignedOut = _onSignedOut.receiveAsFlow()
    val onCustomerCodeSet = _onCustomerCodeSet.receiveAsFlow()
    val onNavigateToStage = _onNavigateToStage.receiveAsFlow()
    val onError = _onError.receiveAsFlow()
    val onLoading = _onLoading.asSharedFlow()

    fun refreshUserName() {
        val name = getNewStageId()
        val userAvatar = getNewUserAvatar()
        repository.stageId = name
        repository.userAvatar = userAvatar
        _userName.value = name
    }

    fun silentSignIn() = launch {
        val isLoading = _onLoading.replayCache.firstOrNull() ?: false
        if (repository.customerCode != null && repository.apiKey != null && !isLoading) {
            Timber.d("Silent sign in")
            _onSignedIn.send(Unit)
        }
    }

    fun signIn(fullCode: String) = launch {
        if (fullCode.isBlank()) {
            _onCustomerCodeSet.send(false)
            return@launch
        }

        fullCode.trim().split("-").takeIf { it.size >= 2 }?.let { codeAndKey ->
            val customerCode = codeAndKey[0]
            var key = codeAndKey[1]
            codeAndKey.getOrNull(2)?.let { secondPartOfTheKey ->
                key = "$key-$secondPartOfTheKey"
            }
            verifyCodeAndKey(customerCode, key)
        } ?: run {
            _onCustomerCodeSet.send(false)
        }
    }

    fun onSignOut() = launch {
        repository.customerCode = null
        _onSignedOut.send(Unit)
    }

    fun onJoinStage() = launch {
        _onLoading.tryEmit(true)
        val response = repository.verifyConnectionCode()
        response.onFailure { error ->
            Timber.d("Failed to sign in: $error")
            _onLoading.tryEmit(false)
            delay(DEFAULT_LOADING_DELAY)
            _onError.send(Error.CustomerCodeError)
        }
        response.onSuccess {
            Timber.d("Signed in")
            _onLoading.tryEmit(false)
            delay(DEFAULT_LOADING_DELAY)
            _onNavigateToStage.send(Pair(ParticipantMode.VIEWER, CreateStageMode.NONE))
        }
    }

    fun onCreateStage(mode: CreateStageMode) = launch {
        _onLoading.tryEmit(true)
        val type = if (mode == CreateStageMode.VIDEO) StageType.VIDEO else StageType.AUDIO
        val response = repository.createStage(type)
        response.onFailure { error ->
            Timber.d("Create stage failed: $error")
            _onLoading.tryEmit(false)
            delay(DEFAULT_LOADING_DELAY)
            _onError.send(error)
        }
        response.onSuccess {
            Timber.d("Stage created")
            _onLoading.tryEmit(false)
            delay(DEFAULT_LOADING_DELAY)
            _onNavigateToStage.send(Pair(ParticipantMode.CREATOR, mode))
        }
    }

    private fun verifyCodeAndKey(customerCode: String, apiKey: String) = launch {
        if (customerCode.isBlank() || apiKey.isBlank()) {
            Timber.d("Code or api key blank.")
            _onCustomerCodeSet.send(false)
            return@launch
        }

        Timber.d("Verifying $customerCode and $apiKey")
        _onLoading.tryEmit(true)
        repository.customerCode = customerCode
        repository.apiKey = apiKey
        val response = repository.verifyConnectionCode()
        response.onFailure { error ->
            Timber.d("Failed to verify customer code or api key: $error")
            _onLoading.tryEmit(false)
            repository.customerCode = null
            repository.apiKey = null
            delay(DEFAULT_LOADING_DELAY)
            _onCustomerCodeSet.send(false)
        }
        response.onSuccess {
            Timber.d("Customer code and api key are valid!")
            _onLoading.tryEmit(false)
            delay(DEFAULT_LOADING_DELAY)
            repository.userAvatar = getNewUserAvatar()
            _onCustomerCodeSet.send(true)
            _onSignedIn.send(Unit)
        }
    }
}
