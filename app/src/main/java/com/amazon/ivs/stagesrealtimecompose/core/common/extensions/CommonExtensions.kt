package com.amazon.ivs.stagesrealtimecompose.core.common.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.amazon.ivs.stagesrealtimecompose.core.common.COLOR_BOTTOM_ATTRIBUTE_NAME
import com.amazon.ivs.stagesrealtimecompose.core.common.COLOR_LEFT_ATTRIBUTE_NAME
import com.amazon.ivs.stagesrealtimecompose.core.common.COLOR_RIGHT_ATTRIBUTE_NAME
import com.amazon.ivs.stagesrealtimecompose.core.common.USERNAME_ATTRIBUTE_NAME
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.handlers.VSScore
import com.amazon.ivs.stagesrealtimecompose.ui.theme.AvatarBottom
import com.amazon.ivs.stagesrealtimecompose.ui.theme.AvatarLeft
import com.amazon.ivs.stagesrealtimecompose.ui.theme.AvatarRight
import com.amazon.ivs.stagesrealtimecompose.ui.theme.OrangePrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val localDateFormat get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

fun String.toDate(): Date = try {
    localDateFormat.parse(this)!!
} catch (_: Exception) {
    Date()
}

fun String.firstCharUpperCase() = this.replaceFirstChar { it.uppercase() }

fun formatString(pattern: String, vararg args: Any?) = String.format(Locale.getDefault(), pattern, *args)

@Composable
fun String?.formatStringOrEmpty(format: @Composable (String) -> String) = if (this != null) format(this) else ""

fun Float.toStringThousandOrZero(): String {
    val thousandValue = (this / 1000).roundToInt()
    return if (thousandValue > 0) "${thousandValue}k" else "0"
}

fun Float.roundToClosest(step: Float): Float {
    val thisValue = this
    if (step == 0f || thisValue == 0f) return thisValue
    val reminder = this % step
    val delta = step - reminder
    val extraStep = if (reminder >= delta) step else 0f
    var value = 0f
    while (value + step <= thisValue) {
        value += step
    }
    value += extraStep
    return value
}

fun Color.toHex(): String {
    val argb = this.toArgb()
    return String.format(Locale.US, "#%06X", (0xFFFFFF and argb))
}

fun Color.toHexA(): String {
    val argb = this.toArgb()
    return String.format(Locale.US, "#%08X", argb)
}

fun String.toColor(): Color {
    val hex = replace("#", "")
    val parsedColor = when (hex.length) {
        6 -> hex.toLong(16) or 0x00000000FF000000
        8 -> hex.toLong(16)
        else -> OrangePrimary.toArgb().toLong()
    }
    return Color(parsedColor)
}

fun Int.toTimerSeconds() = String.format(Locale.getDefault(), "00:%02d", this)

fun <T> MutableStateFlow<List<T>>.updateList(block: MutableList<T>.() -> Unit) = update {
    it.toMutableList().apply(block = block)
}

fun Map<String, String>?.getUserName(fallback: String = "") = if (this == null) {
    fallback
} else {
    get(USERNAME_ATTRIBUTE_NAME) ?: fallback
}

fun Map<String, String>?.getUserAvatar() = if (this == null) UserAvatar() else UserAvatar(
    colorBottom = get(COLOR_BOTTOM_ATTRIBUTE_NAME) ?: AvatarBottom.toHex(),
    colorLeft = get(COLOR_LEFT_ATTRIBUTE_NAME) ?: AvatarLeft.toHex(),
    colorRight = get(COLOR_RIGHT_ATTRIBUTE_NAME) ?: AvatarRight.toHex()
)

fun Map<String, String>.asVSSCore(stageId: String): VSScore {
    val creatorIndex = keys.indexOf(stageId).takeIf { it >= 0 } ?: 0
    val participantIndex = if (creatorIndex == 0) 1 else 0
    val scores = this.map { it.value }
    return VSScore(
        creatorScore = scores[creatorIndex].toIntOrNull() ?: 0,
        participantScore = scores[participantIndex].toIntOrNull() ?: 0,
    )
}

fun Boolean?.isNullOrFalse() = this == null || !this
