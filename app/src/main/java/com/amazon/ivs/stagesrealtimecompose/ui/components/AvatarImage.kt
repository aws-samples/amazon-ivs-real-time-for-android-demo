package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.toHex
import com.amazon.ivs.stagesrealtimecompose.core.common.getNewUserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.common.unknownAvatar
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserAvatar
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GrayQuaternary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.OrangePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RedPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary

@Composable
fun AvatarImage(
    avatar: UserAvatar?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isMuted: Boolean = false,
    isSpeaking: Boolean = false,
) {
    val currentAvatar = avatar ?: unknownAvatar
    val hasBorder = currentAvatar.hasBorder

    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            modifier = Modifier.requiredSize(size + 10.dp)
                .background(
                    color = OrangePrimary,
                    shape = CircleShape
                )
                .clip(CircleShape),
            visible = isSpeaking && !isMuted,
            enter = fadeIn(),
            exit = fadeOut()
        ) {}
        AnimatedVisibility(
            modifier = Modifier.requiredSize(size + 4.dp)
                .background(
                    color = WhitePrimary.copy(alpha = 0.6f),
                    shape = CircleShape
                )
                .clip(CircleShape),
            visible = hasBorder,
            enter = fadeIn(),
            exit = fadeOut()
        ) {}
        Box {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .drawBehind {
                        val angles = listOf(90f, 90f, 180f)
                        var startAngle = 180f

                        currentAvatar.colors.forEachIndexed { index, color ->
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = angles[index],
                                useCenter = true
                            )
                            startAngle += angles[index]
                        }
                    }
            )
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                visible = isMuted
            ) {
                Image(
                    modifier = Modifier
                        .size(24.dp)
                        .background(WhitePrimary, CircleShape)
                        .padding(5.dp),
                    painter = painterResource(R.drawable.ic_mic_off),
                    colorFilter = ColorFilter.tint(color = RedPrimary),
                    contentScale = ContentScale.Inside,
                    contentDescription = null
                )
            }
        }
    }
}

@Preview
@Composable
private fun AvatarImagePreview() {
    PreviewSurface(
        background = GrayQuaternary
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val boxSizeSmall = 48.dp
            val boxSizeLarge = 60.dp

            Box(
                modifier = Modifier.size(boxSizeSmall).background(RedPrimary)
            ) {
                AvatarImage(
                    avatar = UserAvatar(),
                    size = boxSizeSmall
                )
            }

            Box(
                modifier = Modifier.size(boxSizeLarge).background(RedPrimary)
            ) {
                AvatarImage(
                    avatar = UserAvatar(
                        colorLeft = Color(0xffbb983e).toHex(),
                        colorRight = Color(0xff7bbbf3).toHex(),
                        colorBottom = Color(0xfff3417e).toHex(),
                        hasBorder = true
                    ),
                    isMuted = true,
                    size = boxSizeLarge,
                )
            }

            Box(
                modifier = Modifier.size(boxSizeLarge).background(RedPrimary)
            ) {
                AvatarImage(
                    avatar = getNewUserAvatar(hasBorder = true),
                    isSpeaking = true,
                    isMuted = true,
                    size = boxSizeLarge,
                )
            }

            Box(
                modifier = Modifier.size(boxSizeLarge).background(RedPrimary)
            ) {
                AvatarImage(
                    avatar = null,
                    size = boxSizeLarge,
                )
            }
        }
    }
}
