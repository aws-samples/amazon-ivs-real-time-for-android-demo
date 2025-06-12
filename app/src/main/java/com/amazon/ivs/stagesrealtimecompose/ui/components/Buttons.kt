package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.common.BITRATE_DEFAULT
import com.amazon.ivs.stagesrealtimecompose.core.common.BITRATE_MAX
import com.amazon.ivs.stagesrealtimecompose.core.common.BITRATE_MIN
import com.amazon.ivs.stagesrealtimecompose.core.common.BITRATE_STEP
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.roundToClosest
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.toStringThousandOrZero
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackQuaternary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackSecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BluePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GraySecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GrayTertiary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterTertiary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.OrangePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RedPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RobotoMonoPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary

@Composable
fun ButtonPrimary(
    text: String,
    background: Color,
    modifier: Modifier = Modifier,
    textColor: Color = BlackSecondary,
    onClick: () -> Unit,
) {
    val buttonShape = RoundedCornerShape(100)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(
                color = background,
                shape = buttonShape
            )
            .clip(buttonShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = OrangePrimary),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = InterPrimary.copy(color = textColor)
        )
    }
}

@Composable
fun ButtonImage(
    @DrawableRes image: Int,
    description: String,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    background: Color = GraySecondary,
    onClick: () -> Unit,
) {
    val buttonShape = RoundedCornerShape(100)

    Box(
        modifier = modifier
            .background(
                color = background,
                shape = buttonShape
            )
            .clip(buttonShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = OrangePrimary),
                onClick = onClick
            )
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = description
        )
    }
}

@Composable
fun VoteButton(
    votes: Int,
    iconBackground: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val buttonShape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .background(
                color = WhitePrimary.copy(alpha = 0.3f),
                shape = buttonShape
            )
            .clip(buttonShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = OrangePrimary),
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = iconBackground, shape = CircleShape)
                    .padding(4.dp),
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null
            )
            Text(
                text = votes.toString(),
                style = InterTertiary.copy(fontSize = 14.sp)
            )
        }
    }
}

@Composable
fun ButtonCircleImage(
    @DrawableRes image: Int,
    description: String,
    modifier: Modifier = Modifier,
    background: Color = WhitePrimary,
    tint: Color = BlackSecondary,
    padding: PaddingValues = PaddingValues(),
    onClick: () -> Unit
) {
    val buttonShape = CircleShape

    Box(
        modifier = modifier
            .requiredSize(42.dp)
            .background(
                color = background,
                shape = buttonShape
            )
            .clip(buttonShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = OrangePrimary),
                onClick = onClick
            )
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(image),
            contentDescription = description,
            tint = tint
        )
    }
}

@Composable
fun ButtonSwitch(
    text: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = text,
            style = RobotoMonoPrimary.copy(fontWeight = FontWeight.W500, fontSize = 18.sp)
        )
        Switch(
            checked = isChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WhitePrimary,
                checkedTrackColor = OrangePrimary,
                uncheckedThumbColor = WhitePrimary,
                uncheckedTrackColor = GrayTertiary,
                uncheckedBorderColor = GrayTertiary
            ),
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitrateSlider(
    text: String,
    modifier: Modifier = Modifier,
    value: Float = BITRATE_DEFAULT,
    isEnabled: Boolean = true,
    onValueChanged: (Float) -> Unit
) {
    val minValue = BITRATE_MIN
    val maxValue = BITRATE_MAX
    val steps = ((maxValue - minValue) / BITRATE_STEP).toInt() + 1
    var sliderValue by remember { mutableFloatStateOf(value) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row {
            Text(
                modifier = Modifier.weight(weight = 1f),
                text = text,
                style = RobotoMonoPrimary.copy(fontWeight = FontWeight.W500, fontSize = 18.sp)
            )
            Text(
                text = if (!isEnabled) stringResource(R.string.auto) else sliderValue.toStringThousandOrZero(),
                style = RobotoMonoPrimary.copy(fontWeight = FontWeight.W700, fontSize = 18.sp)
            )
        }
        AnimatedVisibility(
            visible = isEnabled
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = -1) }
                        .width(16.dp)
                        .height(6.dp)
                        .align(Alignment.CenterStart)
                        .background(shape = RoundedCornerShape(100), color = OrangePrimary)
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = -1) }
                        .width(16.dp)
                        .height(6.dp)
                        .align(Alignment.CenterEnd)
                        .background(shape = RoundedCornerShape(100), color = GraySecondary)
                )
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = (minValue .. maxValue),
                    steps = steps,
                    value = sliderValue,
                    onValueChange = { value ->
                        sliderValue = value.roundToClosest(BITRATE_STEP)
                    },
                    onValueChangeFinished = {
                        onValueChanged(sliderValue)
                    },
                    track = { state ->
                        SliderDefaults.Track(
                            modifier = Modifier.height(6.dp),
                            sliderState = state,
                            thumbTrackGapSize = 0.dp,
                            drawStopIndicator = null,
                            colors = SliderDefaults.colors(
                                activeTrackColor = OrangePrimary,
                                inactiveTrackColor = GraySecondary,
                                activeTickColor = OrangePrimary,
                                inactiveTickColor = GraySecondary,
                            )
                        )
                    },
                    thumb = {
                        SliderDefaults.Thumb(
                            modifier = Modifier.background(color = OrangePrimary, shape = CircleShape),
                            interactionSource = remember { MutableInteractionSource() },
                            colors = SliderDefaults.colors(thumbColor = OrangePrimary),
                            thumbSize = DpSize(width = 24.dp, height = 24.dp)
                        )
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ButtonPreview() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonPrimary(text = "Primary", background = RedPrimary, textColor = WhitePrimary, onClick = {})
            ButtonImage(modifier = Modifier.size(48.dp), image = R.drawable.ic_settings, description = "", onClick = {})
            ButtonSwitch(text = "Video stats", isChecked = true, onCheckedChange = {})
            ButtonSwitch(text = "Simulcast", isChecked = false, onCheckedChange = {})
            BitrateSlider(text = "Maximum bitrate") { }
            BitrateSlider(text = "Maximum bitrate", isEnabled = false) { }
            Row(
                modifier = Modifier.fillMaxWidth().background(color = BlackQuaternary).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VoteButton(
                    modifier = Modifier.weight(1f),
                    votes = 16,
                    iconBackground = RedPrimary,
                    onClick = {}
                )
                VoteButton(
                    modifier = Modifier.weight(1f),
                    votes = 1456,
                    iconBackground = BluePrimary,
                    onClick = {}
                )
            }
        }
    }
}
