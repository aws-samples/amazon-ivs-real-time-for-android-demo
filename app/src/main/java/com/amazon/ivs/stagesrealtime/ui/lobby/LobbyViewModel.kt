package com.amazon.ivs.stagesrealtime.ui.lobby

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazon.ivs.stagesrealtime.common.DEFAULT_LOADING_DELAY
import com.amazon.ivs.stagesrealtime.common.extensions.asStateFlow
import com.amazon.ivs.stagesrealtime.common.extensions.launch
import com.amazon.ivs.stagesrealtime.common.getNewStageId
import com.amazon.ivs.stagesrealtime.common.getNewUserAvatar
import com.amazon.ivs.stagesrealtime.repository.StageRepository
import com.amazon.ivs.stagesrealtime.repository.models.AppSettings
import com.amazon.ivs.stagesrealtime.repository.models.CreateStageMode
import com.amazon.ivs.stagesrealtime.repository.models.ParticipantMode
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageType
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.Error
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val repository: StageRepository,
    private val appSettingsStore: DataStore<AppSettings>
) : ViewModel() {
    private val _onSignedIn = Channel<Unit>()
    private val _onSignedOut = Channel<Unit>()
    private val _onError = Channel<Error>()
    private val _onCustomerCodeSet = Channel<Boolean>()
    private val _onNavigateToStage = Channel<Pair<ParticipantMode, CreateStageMode>>()
    private val _isLoading = MutableStateFlow(false)

    val onSignedIn = _onSignedIn.receiveAsFlow()
    val onSignedOut = _onSignedOut.receiveAsFlow()
    val onCustomerCodeSet = _onCustomerCodeSet.receiveAsFlow()
    val onNavigateToStage = _onNavigateToStage.receiveAsFlow()
    val onError = _onError.receiveAsFlow()
    val isLoading = _isLoading.asStateFlow()
    val appSettings = appSettingsStore.data.onEach { settings ->
        if (settings.customerCode == null) {
            repository.destroyApi()
        }
    }.asStateFlow(viewModelScope, AppSettings())

    fun refreshUserName() = launch {
        val name = getNewStageId()
        val userAvatar = getNewUserAvatar()
        appSettingsStore.updateData { it.copy(stageId = name, userAvatar = userAvatar) }
    }

    fun silentSignIn() = launch {
        val appSettings = appSettingsStore.data.first()
        Timber.d("Attempting silent sign in: $appSettings")
        if (appSettings.customerCode != null && appSettings.apiKey != null && !_isLoading.value) {
            Timber.d("Silent sign in: ${appSettings.customerCode}-${appSettings.apiKey}")
            _onSignedIn.send(Unit)
        } else {
            Timber.d("No silent sign in")
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

    fun changeBitrate(newBitrate: Int) = launch {
        appSettingsStore.updateData { it.copy(bitrate = newBitrate) }
    }

    fun changeIsSimulcastEnabled(isSimulcastEnabled: Boolean) = launch {
        appSettingsStore.updateData { it.copy(isSimulcastEnabled = isSimulcastEnabled) }
    }

    fun changeIsVideoStatsEnabled(isVideoStatsEnabled: Boolean) = launch {
        appSettingsStore.updateData { it.copy(isVideoStatsEnabled = isVideoStatsEnabled) }
    }

    fun onSignOut() = launch {
        appSettingsStore.updateData { it.copy(customerCode = null) }
        _onSignedOut.send(Unit)
    }

    fun onJoinStage() = launch {
        _isLoading.update { true }
        val response = repository.verifyConnectionCode()
        response.onFailure { error ->
            Timber.d("Failed to sign in: $error")
            _isLoading.update { false }
            delay(DEFAULT_LOADING_DELAY)
            _onError.send(Error.CustomerCodeError)
        }
        response.onSuccess {
            Timber.d("Signed in")
            _isLoading.update { false }
            delay(DEFAULT_LOADING_DELAY)
            _onNavigateToStage.send(Pair(ParticipantMode.VIEWER, CreateStageMode.NONE))
        }
    }

    fun onCreateStage(mode: CreateStageMode) = launch {
        _isLoading.update { true }
        val type = if (mode == CreateStageMode.VIDEO) StageType.VIDEO else StageType.AUDIO
        val response = repository.createStage(type)
        response.onFailure { error ->
            Timber.d("Create stage failed: $error")
            _isLoading.update { false }
            delay(DEFAULT_LOADING_DELAY)
            _onError.send(error)
        }
        response.onSuccess {
            Timber.d("Stage created")
            _isLoading.update { false }
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
        _isLoading.update { true }
        appSettingsStore.updateData { it.copy(customerCode = customerCode, apiKey = apiKey) }

        val response = repository.verifyConnectionCode()
        response.onFailure { error ->
            Timber.d("Failed to verify customer code or api key: $error")
            _isLoading.update { false }
            appSettingsStore.updateData { it.copy(customerCode = null, apiKey = null) }
            delay(DEFAULT_LOADING_DELAY)
            _onCustomerCodeSet.send(false)
        }
        response.onSuccess {
            Timber.d("Customer code and api key are valid!")
            _isLoading.update { false }
            delay(DEFAULT_LOADING_DELAY)
            appSettingsStore.updateData { it.copy(userAvatar = getNewUserAvatar()) }
            _onCustomerCodeSet.send(true)
            _onSignedIn.send(Unit)
        }
    }
}
