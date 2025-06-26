package com.amazon.ivs.stagesrealtimecompose.ui.components

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

val isPortrait @Composable get() = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

fun Modifier.unClickable() = composed {
    clickable(
        enabled = false,
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = {},
    )
}

fun Modifier.unScrollable(
    enabled: Boolean
): Modifier = composed {
    if (!enabled) return@composed this
    val connection = object : NestedScrollConnection {
        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity
        ) = available

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ) = available

        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource
        ) = available
    }
    nestedScroll(connection = connection)
}

inline fun Modifier.thenOptional(
    enabled: Boolean,
    apply: Modifier.() -> Modifier
): Modifier {
    return if (enabled) {
        this@thenOptional.apply()
    } else {
        this@thenOptional
    }
}

fun Modifier.imeOffsetPadding(
    enabled: Boolean = true,
    bottomOffset: Dp = 36.dp
) = composed {
    if (!enabled) return@composed this

    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    val navigationBarBottomPx = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    val reductionPx = with(LocalDensity.current) { bottomOffset.toPx() }
    val finalPaddingPx = (imeBottomPx - navigationBarBottomPx - reductionPx).coerceAtLeast(0f)

    offset {
        IntOffset(
            x = 0,
            y= -finalPaddingPx.roundToInt()
        )
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
fun Modifier.fillMaxPortraitWidth(
    maxWidth: Dp? = null,
) = composed {
    if (isPortrait) {
        fillMaxWidth()
    } else {
        val configuration = LocalConfiguration.current
        var portraitWidth = configuration.screenHeightDp.dp
        if (maxWidth != null) portraitWidth = portraitWidth.coerceAtMost(maxWidth)
        width(portraitWidth)
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun isSquareOrLandscapeSize(): Boolean {
    if (isPortrait) return false

    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp.dp
    val height = configuration.screenHeightDp.dp
    return width >= height
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun isLandscape(): Boolean {
    if (isPortrait) return false

    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp.dp
    val height = configuration.screenHeightDp.dp
    return width > height
}
