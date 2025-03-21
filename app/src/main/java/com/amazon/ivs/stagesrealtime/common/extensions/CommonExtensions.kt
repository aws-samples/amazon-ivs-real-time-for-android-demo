package com.amazon.ivs.stagesrealtime.common.extensions

import androidx.datastore.core.DataStore
import com.amazon.ivs.stagesrealtime.repository.models.AppSettings
import com.amazon.ivs.stagesrealtime.repository.models.PKModeScore
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val localDateFormat get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

fun String.firstCharUpperCase() = this.replaceFirstChar { it.uppercase() }

fun String.toDate(): Date = localDateFormat.parse(this)!!

fun String.toLocalDateFromUTC(): Date {
    val dateFormat = localDateFormat
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val formattedDate = dateFormat.parse(this)!!
    dateFormat.timeZone = TimeZone.getDefault()
    return dateFormat.format(formattedDate).toDate()
}

fun String.getElapsedTimeFromNow() = (System.currentTimeMillis() - this.toLocalDateFromUTC().time) / 1000

fun String?.formatStringOrEmpty(format: (String) -> String) = if (this != null) format(this) else ""

fun Long.toStringWithLeadingZero() = if (this in 0..9) "0$this" else this.toString()

fun Int.toStringThousandOrZero(): String {
    val thousandValue = this / 1000
    return if (thousandValue > 0) "${thousandValue}k" else "0"
}

fun formatString(pattern: String, vararg args: Any?) = String.format(Locale.getDefault(), pattern, *args)

fun Map<String, String>.asPKModeScore(hostId: String, shouldResetScore: Boolean = false): PKModeScore {
    val hostIdIndex = keys.indexOf(hostId).takeIf { it >= 0 } ?: 0
    val guestIdIndex = if (hostIdIndex == 0) 1 else 0
    val scores = this.map { it.value }
    return PKModeScore(
        guestScore = scores[guestIdIndex].toIntOrNull() ?: 0,
        hostScore = scores[hostIdIndex].toIntOrNull() ?: 0,
        shouldResetScore = shouldResetScore
    )
}

suspend fun DataStore<AppSettings>.getUserAvatar() = data.first().userAvatar
suspend fun DataStore<AppSettings>.getStageId() = data.first().stageId
suspend fun DataStore<AppSettings>.getCustomerCode() = data.first().customerCode
suspend fun DataStore<AppSettings>.isVideoStatsEnabled() = data.first().isVideoStatsEnabled
