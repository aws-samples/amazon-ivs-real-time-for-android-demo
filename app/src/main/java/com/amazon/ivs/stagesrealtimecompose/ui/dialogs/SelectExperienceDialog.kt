package com.amazon.ivs.stagesrealtimecompose.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageType
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GraySecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RobotoPrimary

@Composable
fun SelectExperienceDialog() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.select_experience),
            style = RobotoPrimary,
        )
        ButtonPrimary(
            modifier = Modifier.padding(top = 32.dp, bottom = 20.dp),
            text = stringResource(R.string.video_stage),
            background = GraySecondary,
            onClick = {
                StageHandler.createStage(StageType.Video)
            }
        )
        ButtonPrimary(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.audio_stage),
            background = GraySecondary,
            onClick = {
                StageHandler.createStage(StageType.Audio)
            }
        )
        ButtonPrimary(
            text = stringResource(R.string.cancel),
            background = Color.Transparent,
            onClick = NavigationHandler::goBack
        )
    }
}

@Preview
@Composable
private fun SelectExperienceDialogPreview() {
    PreviewSurface {
        SelectExperienceDialog()
    }
}
