package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@Composable
fun HeartBox(
    modifier: Modifier = Modifier
) {
    val hearts by StageHandler.hearts.collectAsStateWithLifecycle()
    var animatedHearts by remember { mutableIntStateOf(0) }
    val width = 150.dp
    val height = 300.dp

    DisposableEffect(Unit) {
        onDispose {
            StageHandler.clearHearts()
        }
    }

    Box(
        modifier = modifier
            .size(width, height)
    ) {
        (animatedHearts until hearts).forEach { index ->
            key(index) {
                HeartAnimation(
                    index = index,
                    boxWidth = width.value,
                    boxHeight = height.value,
                    onAnimated = { animatedIndex ->
                        animatedHearts = animatedIndex
                    }
                )
            }
        }
    }
}

@Composable
private fun HeartAnimation(
    index: Int,
    boxWidth: Float,
    boxHeight: Float,
    onAnimated: (Int) -> Unit
) {
    val xOffset = remember { Animatable(boxWidth) }
    val yOffset = remember { Animatable(boxHeight) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val step1 = mutableListOf<Job>()
        val step2 = mutableListOf<Job>()
        val x1 = boxWidth - (40 .. 90).random()
        val y1 = boxHeight - (220 .. 280).random()
        val x2 = x1 - (-10 .. 10).random()
        val y2 = y1 - (30 .. 50).random()

        step1.add(
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = (500 .. 600).random()
                    )
                )
            }
        )
        step1.add(
            launch {
                xOffset.animateTo(
                    targetValue = x1,
                    animationSpec = tween(
                        durationMillis = (900 .. 1100).random(),
                        easing = FastOutSlowInEasing
                    )
                )
            }
        )
        step1.add(
            launch {
                yOffset.animateTo(
                    targetValue = y1,
                    animationSpec = tween(
                        durationMillis = (900 .. 1100).random(),
                        easing = LinearEasing
                    )
                )
            }
        )
        step1.joinAll()

        step2.add(
            launch {
                xOffset.animateTo(
                    targetValue = x2,
                    animationSpec = tween(
                        durationMillis = (600 .. 900).random(),
                        easing = LinearOutSlowInEasing
                    )
                )
            }
        )
        step2.add(
            launch {
                yOffset.animateTo(
                    targetValue = y2,
                    animationSpec = tween(
                        durationMillis = (600 .. 900).random(),
                        easing = LinearEasing
                    )
                )
            }
        )
        step2.add(
            launch {
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = (800 .. 1100).random()
                    )
                )
            }
        )
        step2.joinAll()
        onAnimated(index)
    }

    val offsetX = xOffset.value.dp
    val offsetY = yOffset.value.dp

    AsyncImage(
        model = R.drawable.ic_flying_heart,
        contentDescription = null,
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer(alpha = alpha.value)
            .size(32.dp),
    )
}
