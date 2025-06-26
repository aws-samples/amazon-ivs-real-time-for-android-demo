package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.handlers.VSHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.VSScore
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.VSLeft
import com.amazon.ivs.stagesrealtimecompose.ui.theme.VSRight
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.ldp
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun ScoreBar(
    width: Dp,
) {
    val density = LocalDensity.current
    val score by VSHandler.score.collectAsStateWithLifecycle()
    val halfWidth = width / 2
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val offsetAnimated by animateFloatAsState(
        targetValue = offsetPx,
        animationSpec = tween(
            durationMillis = 1000,
            easing = EaseOut
        )
    )
    val radius = if (isSquareOrLandscapeSize()) 20.dp else 0.dp
    val shape = RoundedCornerShape(radius)

    LaunchedEffect(key1 = score) {
        val diff = (score.creatorScore - score.participantScore).coerceIn((-10 .. 10))
        val stepSize = halfWidth / 10
        val offset = 0.dp + stepSize * diff
        offsetPx = density.run { offset.toPx() }
        Timber.d("Score bar: $score, $width, $offset, $halfWidth, $offsetPx")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clipToBounds()
            .clip(shape = shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(VSLeft)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(VSRight)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SeamlessTextureScrollBox(
                width = halfWidth,
                offset = offsetAnimated.roundToInt(),
                isLeft = true
            )
            SeamlessTextureScrollBox(
                width = halfWidth,
                offset = offsetAnimated.roundToInt(),
                isLeft = false
            )
        }
    }
}

@Composable
fun SeamlessTextureScrollBox(
    isLeft: Boolean,
    offset: Int,
    width: Dp,
) {
    val scrollSpeed = 300f
    val image = ImageBitmap.imageResource(id = R.drawable.ic_sparks)
    val imageWidth = image.width
    val infiniteTransition = rememberInfiniteTransition()
    val rawOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = imageWidth.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (imageWidth / scrollSpeed * 1000).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )
    val offsetX = if (isLeft) rawOffset else (imageWidth - rawOffset) % imageWidth
    val background = if (isLeft) VSLeft else VSRight
    val gradient = if (isLeft) {
        listOf(Color.Transparent, WhitePrimary.copy(alpha = 0.8f))
    } else {
        listOf(WhitePrimary.copy(alpha = 0.8f), Color.Transparent)
    }

    Box(
        modifier = Modifier
            .width(width)
            .offset {
                IntOffset(
                    x = offset,
                    y = 0
                )
            }
            .background(color = background)
            .graphicsLayer {
                alpha = 0.99f
            }
            .drawWithContent {
                val maskBrush = Brush.linearGradient(
                    colors = if (isLeft) {
                        listOf(Color.Transparent, WhitePrimary)
                    } else {
                        listOf(WhitePrimary, Color.Transparent)
                    },
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = gradient,
                        startX = 0f,
                        endX = size.width - 6 * if (isLeft) -1 else 1
                    )
                )
                drawContent()
                drawRect(
                    brush = maskBrush,
                    blendMode = BlendMode.DstIn
                )
            }
            .clipToBounds()
    ) {
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(WhitePrimary.copy(alpha = alpha - 0.4f))
                .align(if (isLeft) Alignment.CenterEnd else Alignment.CenterStart)
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 2.dp)
        ) {
            val imageHeight = image.height
            val scroll = offsetX % imageWidth

            val drawWidth = size.width.toInt()
            val drawHeight = size.height.toInt()

            drawImage(
                image = image,
                srcOffset = IntOffset(scroll.toInt(), 0),
                srcSize = IntSize(imageWidth - scroll.toInt(), imageHeight),
                dstOffset = IntOffset(0, 0),
                dstSize = IntSize(drawWidth - scroll.toInt(), drawHeight)
            )

            drawImage(
                image = image,
                srcOffset = IntOffset(0, 0),
                srcSize = IntSize(scroll.toInt(), imageHeight),
                dstOffset = IntOffset(drawWidth - scroll.toInt(), 0),
                dstSize = IntSize(scroll.toInt(), drawHeight)
            )
        }
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .rotate(if (isLeft) 0f else 180f)
                .alpha(alpha),
            contentDescription = null,
            model = R.drawable.ic_beam,
            contentScale = ContentScale.FillBounds
        )
    }
}

@Preview(widthDp = 400)
@Composable
private fun ScoreBarAnimationPreview() {
    PreviewSurface {
        var score by remember { mutableStateOf(VSScore()) }
        var width by remember { mutableIntStateOf(400) }

        Column(
            modifier = Modifier
                .background(BlackPrimary)
                .padding(16.dp)
                .onSizeChanged { width = it.width },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScoreBar(
                width = width.ldp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ButtonPrimary(
                    modifier = Modifier.weight(1f),
                    text = "Left",
                    background = VSLeft,
                    textColor = WhitePrimary,
                ) {
                    score = score.copy(creatorScore = score.creatorScore + 4)
                    VSHandler.setScore(score)
                }
                ButtonPrimary(
                    modifier = Modifier.weight(1f),
                    text = "Right",
                    background = VSRight,
                    textColor = WhitePrimary,
                ) {
                    score = score.copy(participantScore = score.participantScore + 4)
                    VSHandler.setScore(score)
                }
            }
        }
    }
}
