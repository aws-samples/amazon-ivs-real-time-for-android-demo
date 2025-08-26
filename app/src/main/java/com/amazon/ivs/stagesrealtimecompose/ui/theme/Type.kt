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
        Font(resId = R.font.inter, weight = FontWeight.Normal),
        Font(resId = R.font.inter_black, weight = FontWeight.Black),
        Font(resId = R.font.inter_bold, weight = FontWeight.Bold),
        Font(resId = R.font.inter_extrabold, weight = FontWeight.ExtraBold),
        Font(resId = R.font.inter_light, weight = FontWeight.Light),
        Font(resId = R.font.inter_medium, weight = FontWeight.Medium),
        Font(resId = R.font.inter_semibold, weight = FontWeight.SemiBold),
    )
}

private val Roboto @Composable get() = if (LocalInspectionMode.current) {
    FontFamily.SansSerif
} else {
    FontFamily(
        Font(resId = R.font.roboto, weight = FontWeight.Normal),
        Font(resId = R.font.roboto_black, weight = FontWeight.Black),
        Font(resId = R.font.roboto_bold, weight = FontWeight.Bold),
        Font(resId = R.font.roboto_light, weight = FontWeight.Light),
        Font(resId = R.font.roboto_medium, weight = FontWeight.Medium),
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
    color = WhitePrimary,
    fontWeight = FontWeight.W400,
    fontSize = 15.sp
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
    fontWeight = FontWeight.W800,
    letterSpacing = 0.sp,
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
