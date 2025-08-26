package com.amazon.ivs.stagesrealtimecompose.ui.dialogs

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amazon.ivs.stagesrealtimecompose.core.common.ANIMATION_DURATION_NORMAL
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.ui.components.DesktopPreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.LandscapePreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.PortraitPreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.components.SquarePreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.fillMaxPortraitWidth
import com.amazon.ivs.stagesrealtimecompose.ui.components.unClickable
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GrayPrimary
import kotlin.math.roundToInt

@Composable
fun DialogContainer(
    background: Color = GrayPrimary,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dialogShape = RoundedCornerShape(28.dp)
    val yOffset = 300.dp

    val isClosing by NavigationHandler.isDialogClosing.collectAsStateWithLifecycle()
    var isOpen by remember { mutableStateOf(false) }
    val offset by animateIntOffsetAsState(
        targetValue = if (isOpen) {
            IntOffset.Zero
        } else {
            IntOffset(
                x = 0,
                y = yOffset.value.roundToInt()
            )
        },
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION_NORMAL.toInt(),
            easing = EaseOut,
        )
    )
    val alpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else if (isClosing) 0f else 0.5f,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION_NORMAL.toInt(),
            easing = EaseOut,
        )
    )

    LaunchedEffect(key1 = isClosing) {
        isOpen = !isClosing
    }

    Box(
        modifier = modifier
            .alpha(alpha)
            .offset { offset }
            .fillMaxPortraitWidth()
            .unClickable()
            .background(color = background, shape = dialogShape)
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 16.dp)
    ) {
        content()
    }
}

@PortraitPreview
@Composable
private fun DialogPortrait() {
    DialogContainerPreview()
}

@LandscapePreview
@Composable
private fun DialogLandscape() {
    DialogContainerPreview()
}

@SquarePreview
@Composable
private fun DialogSquare() {
    DialogContainerPreview()
}

@DesktopPreview
@Composable
private fun DialogDesktop() {
    DialogContainerPreview()
}

@Composable
fun DialogContainerPreview() {
    PreviewSurface {
        DialogOverlay {
            DialogContainer {
                JoinStageDialog()
            }
        }
    }
}
