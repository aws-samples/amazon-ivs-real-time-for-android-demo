package com.amazon.ivs.stagesrealtimecompose.ui.dialogs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.handlers.Destination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.ErrorDestination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserHandler
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonImage
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.components.TextInput
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackSecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.OrangePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RedPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RobotoPrimary

@Composable
fun EnterCodeDialog() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var code by remember { mutableStateOf("") }
        val error by NavigationHandler.errorDestination.collectAsStateWithLifecycle()
        val hasCodeError = error !is ErrorDestination.None
        val keyboardController = LocalSoftwareKeyboardController.current
        val interactionSource = remember { MutableInteractionSource() }
        val focusRequester = remember { FocusRequester() }

        fun enterCode() {
            UserHandler.clearLastCode()
            UserHandler.enterCode(
                code = code,
                keyboardController = keyboardController
            )
        }

        LaunchedEffect(key1 = Unit) {
            focusRequester.requestFocus()
        }

        Text(
            text = stringResource(R.string.enter_customer_code),
            style = RobotoPrimary,
        )
        Row(
            modifier = Modifier.padding(top = 32.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextInput(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
                    .focusable(
                        enabled = true,
                        interactionSource = interactionSource
                    )
                    .focusRequester(focusRequester),
                text = code,
                interactionSource = interactionSource,
                borderColor = if (hasCodeError) RedPrimary else BlackSecondary,
                hint = stringResource(R.string.paste_your_code_here),
                onValueChanged = { code = it },
                capitalization = KeyboardCapitalization.None,
                onImeAction = {
                    enterCode()
                }
            )
            ButtonImage(
                padding = PaddingValues(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
                description = stringResource(R.string.dsc_qr_button),
                image = R.drawable.ic_qr,
                onClick = {
                    NavigationHandler.goTo(Destination.QR)
                }
            )
        }
        ButtonPrimary(
            text = stringResource(R.string.button_continue),
            background = OrangePrimary,
            onClick = ::enterCode
        )
    }
}

@Preview
@Composable
private fun EnterCodeDialogPreview() {
    PreviewSurface {
        EnterCodeDialog()
    }
}
