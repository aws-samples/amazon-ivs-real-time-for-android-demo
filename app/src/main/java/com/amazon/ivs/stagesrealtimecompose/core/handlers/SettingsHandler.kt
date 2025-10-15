package com.amazon.ivs.stagesrealtimecompose.core.handlers

import com.amazon.ivs.stagesrealtimecompose.core.common.BITRATE_DEFAULT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

object SettingsHandler {
    private val _state = MutableStateFlow(
        SettingsState(
            videoStatsEnabled = PreferencesHandler.videoStatsEnabled,
            simulcastEnabled = PreferencesHandler.simulcastEnabled,
            bitrate = PreferencesHandler.bitrate.toFloat(),
        )
    )
    val state = _state.asStateFlow()

    fun changeVideoStatsEnabled(enabled: Boolean) {
        PreferencesHandler.videoStatsEnabled = enabled
        _state.update { _state.value.copy(videoStatsEnabled = enabled) }
    }

    fun changeSimulcastEnabled(enabled: Boolean) {
        PreferencesHandler.simulcastEnabled = enabled
        _state.update { _state.value.copy(simulcastEnabled = enabled) }
    }

    fun changeBitrate(value: Float) {
        PreferencesHandler.bitrate = value.roundToInt()
        _state.update { _state.value.copy(bitrate = value) }
    }
}

data class SettingsState(
    val videoStatsEnabled: Boolean = true,
    val simulcastEnabled: Boolean = false,
    val bitrate: Float = BITRATE_DEFAULT
)
