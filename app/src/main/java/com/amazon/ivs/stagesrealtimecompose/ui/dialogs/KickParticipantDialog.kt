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
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RedPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RobotoPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary

@Composable
fun KickParticipantDialog() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.remove_participant),
            style = RobotoPrimary,
        )
        ButtonPrimary(
            modifier = Modifier.padding(top = 32.dp, bottom = 20.dp),
            text = stringResource(R.string.remove),
            background = RedPrimary,
            textColor = WhitePrimary,
            onClick = StageHandler::kickParticipant
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
private fun KickParticipantDialogPreview() {
    PreviewSurface {
        KickParticipantDialog()
    }
}
