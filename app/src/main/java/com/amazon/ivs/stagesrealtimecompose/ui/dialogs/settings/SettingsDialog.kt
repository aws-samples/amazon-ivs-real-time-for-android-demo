package com.amazon.ivs.stagesrealtimecompose.ui.dialogs.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.SettingsHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.SettingsState
import com.amazon.ivs.stagesrealtimecompose.ui.components.BitrateSlider
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonSwitch
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GraySecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RedPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RobotoPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary

@Composable
fun SettingsDialog() {
    val state by SettingsHandler.state.collectAsStateWithLifecycle()

    SettingsDialogContent(
        state = state,
        onVideoStatsEnabledChanged = SettingsHandler::changeVideoStatsEnabled,
        onSimulcastEnabledChanged = SettingsHandler::changeSimulcastEnabled,
        onBitrateChanged = SettingsHandler::changeBitrate,
    )
}

@Composable
private fun SettingsDialogContent(
    state: SettingsState,
    onVideoStatsEnabledChanged: (Boolean) -> Unit,
    onSimulcastEnabledChanged: (Boolean) -> Unit,
    onBitrateChanged: (Float) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = RobotoPrimary,
        )
        ButtonSwitch(
            modifier = Modifier.padding(top = 32.dp, bottom = 8.dp),
            text = stringResource(R.string.video_stats),
            isChecked = state.videoStatsEnabled,
            onCheckedChange = onVideoStatsEnabledChanged
        )
        ButtonSwitch(
            modifier = Modifier.padding(bottom = 16.dp),
            text = stringResource(R.string.simulcast),
            isChecked = state.simulcastEnabled,
            onCheckedChange = onSimulcastEnabledChanged
        )
        BitrateSlider(
            modifier = Modifier.padding(bottom = 8.dp),
            text = stringResource(R.string.maximum_bitrate),
            isEnabled = !state.simulcastEnabled,
            onValueChanged = onBitrateChanged
        )
        Box(
            modifier = Modifier
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(GraySecondary)
        )
        ButtonPrimary(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.sign_out),
            background = RedPrimary,
            textColor = WhitePrimary,
            onClick = NavigationHandler::signOut
        )
        ButtonPrimary(
            text = stringResource(R.string.dismiss),
            background = Color.Transparent,
            onClick = NavigationHandler::goBack
        )
    }
}

@Preview
@Composable
private fun SettingsDialogPreview() {
    PreviewSurface {
        SettingsDialogContent(
            state = SettingsState(),
            onVideoStatsEnabledChanged = {},
            onSimulcastEnabledChanged = {},
            onBitrateChanged = {}
        )
    }
}
