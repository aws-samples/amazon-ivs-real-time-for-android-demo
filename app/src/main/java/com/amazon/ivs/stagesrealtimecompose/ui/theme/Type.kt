package com.amazon.ivs.stagesrealtimecompose.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.amazon.ivs.stagesrealtimecompose.R

private val Inter @Composable get() = if (LocalInspectionMode.current) {
    FontFamily.SansSerif
} else {
    FontFamily(
        Font(R.font.inter, FontWeight.Normal),
        Font(R.font.inter_bold, FontWeight.Bold),
        Font(R.font.inter_black, FontWeight.Black),
    )
}

private val Roboto @Composable get() = if (LocalInspectionMode.current) {
    FontFamily.SansSerif
} else {
    FontFamily(
        Font(R.font.roboto, FontWeight.Normal),
    )
}

private val RobotoMono @Composable get() = if (LocalInspectionMode.current) {
    FontFamily.SansSerif
} else {
    FontFamily(
        Font(R.font.roboto_mono, FontWeight.Normal),
    )
}

val InterPrimary @Composable get() = TextStyle(
    color = BlackSecondary,
    fontFamily = Inter,
    fontSize = 18.sp,
    fontWeight = FontWeight.W800
)

val InterSecondary @Composable get() = InterPrimary.copy(
    color = GrayTertiary,
    fontWeight = FontWeight.W600,
    fontSize = 16.sp
)

val InterTertiary @Composable get() = InterPrimary.copy(
    color = WhitePrimary,
    fontWeight = FontWeight.W700,
    fontSize = 15.sp
)

val InterSmall @Composable get() = InterPrimary.copy(
    color = BlackSecondary,
    fontWeight = FontWeight.W400,
    fontSize = 13.sp
)

val InterStats @Composable get() = InterPrimary.copy(
    color = WhitePrimary,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    shadow = Shadow(
        blurRadius = 4f,
        color = BlackPrimary,
        offset = Offset(x = 2f, y = 2f)
    )
)

val InterDebug @Composable get() = InterPrimary.copy(
    color = BlackPrimary,
    fontWeight = FontWeight.W700,
    fontSize = 14.sp
)

val RobotoPrimary @Composable get() = TextStyle(
    color = BlackSecondary,
    fontFamily = Roboto,
    fontSize = 22.sp,
    fontWeight = FontWeight.W800
)

val RobotoMonoPrimary @Composable get() = TextStyle(
    color = BlackSecondary,
    fontFamily = RobotoMono,
    fontSize = 16.sp,
    fontWeight = FontWeight.W700
)

val RobotoMonoSecondary @Composable get() = TextStyle(
    color = GrayQuaternary,
    fontFamily = RobotoMono,
    fontSize = 18.sp,
    fontWeight = FontWeight.W500
)

val RobotoMonoSecondaryBold @Composable get() = TextStyle(
    color = BlackSecondary,
    fontFamily = RobotoMono,
    fontSize = 18.sp,
    fontWeight = FontWeight.W700
)
