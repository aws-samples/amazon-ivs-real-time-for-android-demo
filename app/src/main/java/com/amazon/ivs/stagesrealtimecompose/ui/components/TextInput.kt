package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackQuaternary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackSecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GrayQuaternary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GrayTertiary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterSecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.OrangePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RedPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary

@Composable
private fun textFieldColors(
    backgroundColor: Color = WhitePrimary,
    textColor: Color = BlackSecondary,
    hintColor: Color = GrayTertiary,
) = TextFieldDefaults.colors(
    cursorColor = OrangePrimary,
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    focusedLabelColor = textColor,
    unfocusedLabelColor = textColor,
    unfocusedPlaceholderColor = hintColor,
    focusedPlaceholderColor = hintColor,
    disabledPlaceholderColor = hintColor,
    focusedContainerColor = backgroundColor,
    unfocusedContainerColor = backgroundColor,
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor =  Color.Transparent,
    selectionColors = TextSelectionColors(handleColor = OrangePrimary, backgroundColor = GrayTertiary),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInput(
    hint: String,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    text: String = "",
    maxLines: Int = 1,
    singleLine: Boolean = true,
    backgroundColor: Color = WhitePrimary,
    borderColor: Color = BlackSecondary,
    textColor: Color = BlackSecondary,
    hintColor: Color = GrayTertiary,
    colors: TextFieldColors = textFieldColors(
        backgroundColor = backgroundColor,
        textColor = textColor,
        hintColor = hintColor
    ),
    textStyle: TextStyle = InterSecondary.copy(color = textColor),
    hintStyle: TextStyle = InterSecondary.copy(color = hintColor),
    shape: Shape = RoundedCornerShape(100),
    imeAction: ImeAction = ImeAction.Done,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    keyboardType: KeyboardType = KeyboardType.Unspecified,
    onValueChanged: (String) -> Unit = {},
    onImeAction: (String) -> Unit = {}
) {
    val label: (@Composable () -> Unit)? = if (text.isBlank()) {{
        Text(
            text = hint,
            style = hintStyle,
        )
    }} else null

    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = borderColor,
                shape = shape
            ),
        value = text,
        maxLines = maxLines,
        singleLine = singleLine,
        textStyle = textStyle,
        onValueChange = onValueChanged,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            imeAction = imeAction,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions(
            onAny = {
                onImeAction(text)
            },
        ),
        cursorBrush = SolidColor(OrangePrimary),
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = text,
                innerTextField = innerTextField,
                placeholder = label,
                shape = shape,
                singleLine = singleLine,
                enabled = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                colors = colors,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    )
}

@Preview
@Composable
private fun TextInputPreview() {
    PreviewSurface(
        background = GrayQuaternary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextInput(
                hint = "White BG hint"
            )
            TextInput(
                hint = "White BG, Red Error hint",
                borderColor = RedPrimary
            )
            TextInput(
                hint = "Gray BG hint",
                backgroundColor = BlackQuaternary.copy(alpha = 0.4f),
                borderColor = Color.Transparent,
                hintColor = WhitePrimary
            )
            TextInput(
                hint = "",
                text = "Gray BG, White text",
                backgroundColor = BlackQuaternary.copy(alpha = 0.4f),
                borderColor = Color.Transparent,
                textColor = WhitePrimary
            )
        }
    }
}
